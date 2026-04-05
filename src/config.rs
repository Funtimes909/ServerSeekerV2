use serde::Deserialize;
use std::cmp::max;
use std::fs::File;
use std::io::{ErrorKind, Read};
use tracing::error;

#[derive(Deserialize, Clone, Copy, Debug)]
pub enum ServerRescanPriority {
	OldestFirst,
	NewestFirst,
	LeastPlayers,
	MostPlayers,
}

#[derive(Deserialize, Clone, Debug)]
pub struct Config {
	pub database: Database,
	pub scanner: ScannerConfig,
	pub masscan: Masscan,
	pub player_tracking: PlayerTracking,
	pub country_tracking: CountryTracking,
}

#[derive(Deserialize, Clone, Debug)]
pub struct Database {
	pub host: String,
	pub port: u16,
	pub table: String,
	pub user: String,
	pub password: String,
}

#[derive(Deserialize, Clone, Debug)]
pub struct ScannerConfig {
	pub repeat: bool,
	pub scan_delay: u64,
	pub port_range_start: u16,
	pub port_range_end: u16,
	pub ignore_fake_players: bool,
	pub rescan_priority: ServerRescanPriority,
}

#[derive(Deserialize, Clone, Debug)]
pub struct Masscan {
	pub config_file: String,
}

#[derive(Deserialize, Clone, Debug)]
pub struct PlayerTracking {
	pub webhook: String,
	pub enabled: bool,
	pub players: Vec<String>,
}

#[derive(Deserialize, Clone, Debug)]
pub struct CountryTracking {
	pub enabled: bool,
	pub update_frequency: u64,
	pub ipinfo_token: String,
}

impl ScannerConfig {
	pub fn total_ports(&self) -> u16 {
		let start = self.port_range_start;
		let end = self.port_range_end;

		if start > end {
			error!("port_range_start cannot be greater than port_range_end!");
			std::process::exit(1);
		}

		max(1, end - start)
	}
}

pub fn load_config(path: &str) -> Result<Config, std::io::Error> {
	let mut file = File::open(path)?;
	let mut contents = String::new();
	file.read_to_string(&mut contents).unwrap_or_default();
	toml::from_str(&contents).map_err(|e| std::io::Error::new(ErrorKind::InvalidData, e))
}
