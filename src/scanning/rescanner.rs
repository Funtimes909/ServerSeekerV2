use std::net::SocketAddrV4;

use chrono::Timelike;
use futures_util::StreamExt;
use indicatif::{ProgressBar, ProgressStyle};
use sqlx::{Row, types::ipnet::IpNet};

use crate::{
	CONFIG, config::ServerRescanPriority, database::{Database, ServerUpdateOperation}, protocol::{minecraft::simple_ping, response::MinecraftServer}
};

pub struct Rescanner {
	pub is_active: bool,
	pub database: Database,
}

impl Rescanner {
	#[rustfmt::skip]
	pub async fn rescan_database(&self) {
		let stream_order = match CONFIG.scanner.rescan_priority {
			ServerRescanPriority::OldestFirst => "ORDER BY lastseen ASC",
			ServerRescanPriority::NewestFirst => "ORDER BY lastseen DESC",
			ServerRescanPriority::LeastPlayers => "ORDER BY online_players ASC",
			ServerRescanPriority::MostPlayers => "ORDER BY online_players DESC",
		};

		let query = format!("SELECT (address - '0.0.0.0'::inet) AS address FROM servers {}", stream_order);
		let servers_count = self.database.count_servers().await.unwrap();

        let mut servers_stream = sqlx::query(&query).fetch(&self.database.connection);

		let style = ProgressStyle::with_template("[{elapsed_precise}] [{bar:40.white/blue}] {human_pos}/{human_len} {msg}")
			.expect("failed to create progress bar style")
			.progress_chars("=>-");

		let bar = ProgressBar::new(servers_count as u64).with_style(style);

        // Need to be able to pause this process at anytime when the countries table gets updated
		while let Some(Ok(row)) = servers_stream.next().await && self.is_active {
            let address = row.get::<IpNet, _>("address");

            let connect_address = match address {
                IpNet::V4(i) => i.addr(),
                _ => continue,
            };

			let mut stream = tokio::net::TcpStream::connect(SocketAddrV4::new(connect_address, 25565)).await.unwrap();

            let database_clone = self.database.clone();

            if let Ok(ping_response) = simple_ping(&mut stream).await {
                if let Ok(server) = serde_json::from_str::<MinecraftServer>(&ping_response) {

					if server.has_opted_out() {
						database_clone.delete_server(address).await.unwrap();
					}

                    let update_operation = ServerUpdateOperation {
                        server,
                        address,
                        port: 25565,
                        timestamp: chrono::Utc::now().naive_utc().with_nanosecond(0).unwrap(),
                        database: database_clone,
                    };

					update_operation.update_or_insert_server().await.unwrap();
					update_operation.update_or_insert_players().await.unwrap();
					update_operation.update_or_insert_mods().await.unwrap();
                }
            }

            bar.inc(1);
        }
	}
}
