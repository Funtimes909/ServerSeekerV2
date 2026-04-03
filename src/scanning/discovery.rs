use chrono::Timelike;
use sqlx::types::ipnet::{IpNet, Ipv4Net};
use std::{
	net::{Ipv4Addr, SocketAddrV4},
	str::FromStr,
};
use tokio::io::{AsyncBufReadExt, BufReader};
use tokio::process::Command;

use crate::{
	config::Config,
	database::{Database, ServerUpdateOperation},
	protocol::{minecraft::simple_ping, response::MinecraftServer},
};

pub struct DiscoveryScanner {
	pub is_active: bool,
	config: Config,
	database: Database,
}

impl DiscoveryScanner {
	async fn scan(&self) {
		// Spawn masscan
		let mut command = Command::new("sudo")
			.args(["masscan", "-c", &self.config.masscan.config_file])
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

			// .nth() consumes all preceding elements so address will be the 2nd
			let address = match line.nth(1).and_then(|a| Ipv4Addr::from_str(a).ok()) {
				Some(address) => address,
				_ => continue,
			};

			// Get port
			let port = match line
				.nth(3)
				// Split on port/tcp
				.and_then(|p| p.split('/').nth(0))
				// Parse as u16
				.and_then(|s| s.parse::<u16>().ok())
			{
				Some(port) => port,
				None => continue,
			};

			let database_clone = self.database.clone();

			// Spawn a pinging task for each server found
			tokio::spawn(async move {
				let socket = SocketAddrV4::new(address, port);
				let mut stream = tokio::net::TcpStream::connect(socket).await.unwrap();

				if let Ok(response) = simple_ping(&mut stream).await {
					if let Ok(server) = serde_json::from_str::<MinecraftServer>(&response) {
						let address = IpNet::from(Ipv4Net::from(address));

						if server.has_opted_out() {
							println!("Deleting server!");
							database_clone.delete_server(address).await.unwrap();
						}

						let update_operation = ServerUpdateOperation {
							server,
							address,
							port: port as i32,
							timestamp: chrono::Utc::now().naive_utc().with_nanosecond(0).unwrap(),
							database: database_clone,
						};

						update_operation.update_or_insert_server().await.unwrap();
						update_operation.update_or_insert_players().await.unwrap();
						update_operation.update_or_insert_mods().await.unwrap();
					}
				}
			});
		}
	}
}
