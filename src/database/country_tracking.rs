use crate::config::Config;
use anyhow::bail;
use flate2::read::GzDecoder;
use futures_util::StreamExt;
use indicatif::{ProgressBar, ProgressIterator, ProgressStyle};
use serde::Deserialize;
use sqlx::PgPool;
use sqlx::types::ipnet::IpNet;
use std::fs::File;
use std::io::{Read, Write};
use std::str::FromStr;
use std::time::Duration;
use tracing::{debug, info};

const DOWNLOAD_URL: &str = "https://ipinfo.io/data/ipinfo_lite.json.gz?token=";

#[derive(Deserialize, Debug)]
struct CountryRow {
	network: String,
	country: String,
	country_code: String,
	asn: Option<String>,
	#[serde(rename = "as_name")]
	company: Option<String>,
}

pub async fn country_tracking(pool: PgPool, config: Config) -> anyhow::Result<()> {
	insert_json_to_table(&pool).await?;

	// Sleep
	tokio::time::sleep(Duration::from_secs(
		config.country_tracking.update_frequency * 60 * 60,
	))
	.await;

	Ok(())
}

async fn download_database(config: &Config) -> anyhow::Result<()> {
	let url = format!("{}{}", DOWNLOAD_URL, config.country_tracking.ipinfo_token);
	let response = reqwest::get(url).await?;

	// If response is OK write to file and unzip
	if response.status().is_success() {
		// Content length header is required for the progress bar
		// IPInfo should always supply this
		let content_length = match response.content_length() {
			Some(len) => len,
			None => bail!("Content-Length header was not set!"),
		};

		let mut downloaded: u64 = 0;
		let mut output_file = File::create("ipinfo.json.gz")?;
		let mut reader = response.bytes_stream();

		let style = ProgressStyle::with_template(
			"[{elapsed_precise}] [{bar:40.white/blue}] {bytes}/{total_bytes} {msg}",
		)
		.expect("failed to create progress bar style")
		.progress_chars("=>-");

		let bar = ProgressBar::new(content_length).with_style(style);
		bar.set_message("Downloading the latest version of the IPInfo database...");

		while let Some(Ok(chunk)) = reader.next().await {
			output_file.write_all(&chunk)?;

			// Update download bar position
			let new = std::cmp::min(downloaded + (chunk.len() as u64), content_length);
			downloaded = new;
			bar.set_position(new);
		}

		// Done
		bar.finish_with_message("Finished!");
		info!("Decompressing output file...");

		// Decompress file
		let mut decoder = GzDecoder::new(File::open("ipinfo.json.gz")?);
		let mut file = File::create("ipinfo.json")?;
		let mut string = String::new();

		// Write to output file
		decoder.read_to_string(&mut string)?;
		let mut output_file = File::create("ipinfo.json")?;
		output_file.write_all(string.as_bytes())?;
		file.flush()?;

		// Delete compressed file
		std::fs::remove_file("ipinfo.json.gz")?;

		Ok(())
	} else {
		bail!(
			"IPInfo download failed: {} {:?}",
			response.status(),
			response.status().canonical_reason()
		)
	}
}

// JSON from ipinfo is not valid JSON, we need to parse it into valid JSON manually
async fn parse_json_to_vec(string: String) -> serde_json::Result<Vec<CountryRow>> {
	serde_json::from_str(&format!(
		"[{}]",
		string
			.split("}\n{")
			.map_while(|x| {
				// Skip all IPv6 netblocks
				match x.contains("::") {
					true => None,
					false => Some(x),
				}
			})
			.map(|s| s.trim_matches(&['\n', '{', '}'][..]))
			.map(|s| format!("{{{}}}", s))
			.collect::<Vec<_>>()
			.join(",")
	))
}

async fn insert_json_to_table(pool: &PgPool) -> anyhow::Result<()> {
	let mut file = File::open("ipinfo.json")?;
	let mut string = String::new();
	file.read_to_string(&mut string)?;

	let json = parse_json_to_vec(string).await?;
	info!("JSON Parsed successfully.");

	let mut transaction = pool.begin().await?;

	let style = ProgressStyle::with_template(
		"[{elapsed_precise}] [{bar:40.white/blue}] {human_pos}/{human_len} {msg}",
	)
	.expect("failed to create progress bar style")
	.progress_chars("=>-");

	let bar = ProgressBar::new(json.len() as u64).with_style(style);
	bar.set_message("Inserting rows to countries table...");

	for netblock in json.into_iter().progress_with(bar) {
		if let Ok(cidr) = IpNet::from_str(&netblock.network) {
			sqlx::query(
				"INSERT INTO countries VALUES ($1, $2, $3, $4, $5) 
					ON CONFLICT (network) DO UPDATE SET
					network = EXCLUDED.network,
					country = EXCLUDED.country,
					country_code = EXCLUDED.country_code,
					asn = EXCLUDED.asn,
					asn_name = EXCLUDED.asn_name",
			)
			.bind(cidr)
			.bind(netblock.country)
			.bind(netblock.country_code)
			.bind(netblock.asn)
			.bind(netblock.company)
			.execute(&mut *transaction)
			.await
			.unwrap();
		};
	}

	transaction.commit().await.unwrap();

	info!("All done!");
	Ok(())
}
