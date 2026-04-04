use crate::CONFIG;
use anyhow::bail;
use flate2::read::GzDecoder;
use futures_util::StreamExt;
use indicatif::{ProgressBar, ProgressIterator, ProgressStyle};
use serde::Deserialize;
use sqlx::types::ipnet::IpNet;
use sqlx::{PgPool, QueryBuilder};
use std::fs::File;
use std::io::{Read, Write};
use std::str::FromStr;
use tracing::{error, info};

const DOWNLOAD_URL: &str = "https://ipinfo.io/data/ipinfo_lite.json.gz?token=";
const POSTGRES_BIND_LIMIT: usize = 65535;

#[derive(Deserialize, Debug, Clone)]
struct CountryRow {
	network: String,
	country: String,
	country_code: String,
	asn: Option<String>,
	#[serde(rename = "as_name")]
	asn_name: Option<String>,
}

pub async fn download_ipinfo_json() -> anyhow::Result<()> {
	let url = format!("{}{}", DOWNLOAD_URL, CONFIG.country_tracking.ipinfo_token);
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
pub async fn parse_json_to_vec(string: String) -> serde_json::Result<Vec<CountryRow>> {
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

pub async fn insert_records_to_database(pool: &PgPool) -> anyhow::Result<()> {
	let mut file = File::open("ipinfo.json")?;
	let mut string = String::new();
	file.read_to_string(&mut string)?;

	let country_rows = parse_json_to_vec(string).await?;

	let style = ProgressStyle::with_template(
		"[{elapsed_precise}] [{bar:40.white/blue}] {human_pos}/{human_len}",
	)
	.expect("failed to create progress bar style")
	.progress_chars("=>-");

	let bar = ProgressBar::new(country_rows.len() as u64).with_style(style);

	let rows_per_chunk = POSTGRES_BIND_LIMIT / 5;
	let mut buffer = Vec::with_capacity(rows_per_chunk);
	let mut iter = country_rows.into_iter();
	let mut rows_affected = 0;

	loop {
		buffer.clear();

		for _ in 0..rows_per_chunk {
			match iter.next() {
				Some(item) => buffer.push(item),
				None => break,
			}
		}

		if buffer.is_empty() {
			break;
		}

		let mut builder = QueryBuilder::new("INSERT INTO countries ");

		builder.push_values(buffer.iter().take(rows_per_chunk), |mut query, record| {
			// Some addresses lack a suffix, parsing to an ipnet will fail if it's missing
			let network = match record.network.contains('/') {
				true => IpNet::from_str(&record.network),
				false => IpNet::from_str(&format!("{}{}", &record.network, "/32")),
			};

			if let Ok(inet) = network {
				query
					.push_bind(inet)
					.push_bind(&record.country)
					.push_bind(&record.country_code)
					.push_bind(&record.asn)
					.push_bind(&record.asn_name);
			}

			bar.inc(1);
		});

		builder.push(
			" ON CONFLICT (network) DO UPDATE SET 
			network = EXCLUDED.network,
			country = EXCLUDED.country,
			country_code = EXCLUDED.country_code,
			asn = EXCLUDED.asn,
			asn_name = EXCLUDED.asn_name",
		);

		let query_result = builder.build().execute(pool).await;

		match query_result {
			Ok(result) => rows_affected += result.rows_affected(),
			Err(e) => error!("Failed to update countries table: {e}"),
		}
	}

	info!("Updated {rows_affected} rows");

	Ok(())
}
