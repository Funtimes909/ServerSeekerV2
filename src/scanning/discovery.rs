use chrono::Utc;
use sqlx::types::ipnet::{IpNet, Ipv4Net};
use std::{net::Ipv4Addr, str::FromStr};
use tokio::io::{AsyncBufReadExt, BufReader};
use tokio::process::Command;
use tracing::debug;

use crate::CONFIG;
use crate::database::ServerUpdateOperation;
use crate::database::Database;

pub struct DiscoveryScanner {
	database: Database,
}

impl DiscoveryScanner {
	async fn scan(&self) {
		// Spawn masscan
		let mut command = Command::new("sudo")
			.args(["masscan", "-c", &CONFIG.masscan.config_file])
			.stdout(std::process::Stdio::piped())
			.spawn()
			.expect("error while executing masscan");

		// Verify stdout is valid
		let stdout = command
			.stdout
			.take()
			.expect("Failed to get stdout from masscan!");

		let mut reader = BufReader::new(stdout).lines();

		// Iterate over the lines of output from masscan
		while let Ok(Some(line)) = reader.next_line().await {
			let mut line = line.split_whitespace();

			// Address
			let address = match line.nth(1).and_then(|a| Ipv4Addr::from_str(a).ok()) {
				Some(address) => address,
				_ => continue,
			};

			// Port
			let port = match line
				.nth(3)
				// Split on port/tcp
				.and_then(|p| p.split('/').nth(0))
				// Parse as u16
				.and_then(|s| s.parse().ok())
			{
				Some(port) => port,
				None => continue,
			};

			// Immediately ping any returned servers
			let completed_server = super::run(address, port).await;

			if let Ok(Ok(server)) = completed_server {
				let insertion_operation = ServerUpdateOperation {
					server,
					address: IpNet::from(Ipv4Net::from(address)),
					port: port as i32,
					timestamp: Utc::now().naive_local(),
					database: self.database.clone(),
				};

				// Update server
				if let Err(e) = insertion_operation.update_or_insert_server().await {
					debug!("Error while updating server ({address}:{port}) in database: {e}")
				}

				// Update players
				if let Err(e) = insertion_operation.update_or_insert_players().await {
					debug!("Error while updating players ({address}:{port}) in database: {e}")
				}

				// Update mods
				if let Err(e) = insertion_operation.update_or_insert_mods().await {
					debug!("Error while updating mods ({address}:{port}) in database: {e}")
				}
			}
		}
	}
}
