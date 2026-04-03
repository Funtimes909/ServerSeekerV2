#![feature(string_from_utf8_lossy_owned)]

mod config;
mod database;
mod protocol;
mod scanning;

use clap::Parser;
use config::load_config;
use lazy_static::lazy_static;
use sqlx::ConnectOptions;
use sqlx::postgres::{PgConnectOptions, PgPoolOptions};
use std::time::Duration;
use tracing::error;
use tracing::log::LevelFilter;

use crate::config::Config;
use crate::database::Database;
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
	static ref CONFIG: Config = {
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
