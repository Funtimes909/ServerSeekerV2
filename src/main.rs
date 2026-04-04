#![feature(string_from_utf8_lossy_owned)]

mod config;
mod database;
mod protocol;
mod scanning;

use chrono::{DateTime, Local, NaiveDate};
use clap::Parser;
use config::load_config;
use lazy_static::lazy_static;
use sqlx::postgres::{PgConnectOptions, PgPoolOptions};
use sqlx::{ConnectOptions, Row};
use std::time::Duration;
use tracing::log::LevelFilter;
use tracing::{error, info};

use crate::config::Config;
use crate::database::{Database, country_tracking};
use crate::scanning::rescanner::{Rescanner, ServerRescanPriority};

#[derive(Parser, Debug)]
#[clap(about = "Scans the internet for minecraft servers and indexes them")]
#[clap(rename_all = "kebab-case")]
struct Args {
	#[clap(help = "Specifies the mode to run")]
	#[clap(default_value = "rescanner")]
	#[clap(long, short = 'm')]
	mode: Mode,

	#[clap(help = "Specifies the location of the config file")]
	#[clap(default_value = "config.toml")]
	#[clap(long, short = 'c')]
	config_file: String,
}

#[derive(clap::ValueEnum, Clone, Debug, Default)]
pub enum Mode {
	#[default]
	Scanning,
	Rescanner,
}

lazy_static! {
	pub static ref CONFIG: Config = {
		match load_config(&Args::parse().config_file) {
			Ok(config) => config,
			Err(e) => {
				error!("Fatal error loading config file: {}", e);
				std::process::exit(1);
			}
		}
	};
}

#[tokio::main]
async fn main() {
	tracing_subscriber::fmt::init();

	let arguments = Args::parse();

	// Credentials for database
	let options = PgConnectOptions::new()
		.username(&CONFIG.database.user)
		.password(&CONFIG.database.password)
		.host(&CONFIG.database.host)
		.port(CONFIG.database.port)
		.database(&CONFIG.database.table)
		.log_slow_statements(LevelFilter::Off, Duration::from_secs(60));

	// Connect to the database with options
	let pool = PgPoolOptions::new()
		.max_lifetime(Duration::from_secs(86400))
		.max_connections(100)
		.acquire_slow_threshold(Duration::from_secs(60))
		.connect_with(options)
		.await
		.expect("Failed to connect to database!");

	// Run migrations to setup database
	sqlx::migrate!("./migrations")
		.run(&pool)
		.await
		.expect("Failed to run migrations on database!");

	// Setup country tracking if enabled
	if CONFIG.country_tracking.enabled {
		let track_commit_timestamp_enabled = sqlx::query("show track_commit_timestamp")
			.fetch_one(&pool)
			.await
			.expect("Couldn't fetch track_commit_timestamp status from postgres")
			.get::<&str, _>("track_commit_timestamp")
			.eq("on");

		// Ensure timestamp logging is enabled on the database
		if !track_commit_timestamp_enabled {
			error!("Please enable \"track_commit_timestamps\" in the database configuration");
			std::process::exit(1);
		}

		// Get the timestamp that the countries table was last modified
		let countries_last_updated_timestamp =
			sqlx::query("SELECT pg_xact_commit_timestamp(xmin) FROM countries")
				.fetch_one(&pool)
				.await
				.expect("Couldn't fetch timestamp of last transaction on countries table")
				.get::<DateTime<Local>, _>("pg_xact_commit_timestamp");

		// Only update countries table if it was last modified more than the update frequency ago
		if countries_last_updated_timestamp
			<= Local::now()
				- chrono::Duration::hours(CONFIG.country_tracking.update_frequency as i64)
		{
			info!("Updating out of date countries table");

			if let Err(e) = country_tracking::download_ipinfo_json().await {
				error!("Error while downloading countries database from ipinfo: {e}");
			};

			if let Err(e) = country_tracking::insert_records_to_database(&pool).await {
				error!("Error while inserting rows to countries table: {e}");
			}
		}

		country_tracking::run(&pool).await;
	}

	match arguments.mode {
		Mode::Scanning => todo!(),
		Mode::Rescanner => {
			let rescanner = Rescanner {
				is_active: true,
				database: Database { connection: pool },
				rescan_priority: ServerRescanPriority::OldestFirst,
			};

			rescanner.rescan_database().await;
		}
	}
}
