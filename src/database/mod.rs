pub mod country_tracking;

use crate::CONFIG;
use crate::protocol::response::ANONYMOUS_PLAYER_NAME;

use super::protocol::response::MinecraftServer;
use chrono::NaiveDateTime;
use sqlx::postgres::PgQueryResult;
use sqlx::types::Uuid;
use sqlx::types::ipnet::IpNet;
use sqlx::{Pool, Postgres, QueryBuilder, Row};

#[derive(Debug, Clone)]
pub struct Database {
	pub connection: Pool<Postgres>,
}

impl Database {
	/// Gets the count of servers from database
	pub async fn count_servers(&self) -> Result<i64, sqlx::Error> {
		sqlx::query("SELECT COUNT(*) FROM servers")
			.fetch_one(&self.connection)
			.await
			.map(|a| a.get("count"))
	}

	/// Deletes a server from the database
	pub async fn delete_server(&self, address: IpNet) -> Result<PgQueryResult, sqlx::Error> {
		sqlx::query("DELETE FROM servers WHERE address = $1")
			.bind(address)
			.execute(&self.connection)
			.await
	}
}

#[derive(Debug)]
pub struct ServerUpdateOperation {
	pub server: MinecraftServer,
	pub address: IpNet,
	pub port: i32,
	pub timestamp: NaiveDateTime,
	pub database: Database,
}

impl ServerUpdateOperation {
	/// Inserts a single server into the database
	pub async fn update_or_insert_server(&self) -> anyhow::Result<()> {
		let mut query = String::from("INSERT INTO servers (address, port, first_seen, last_seen, ");

		// Used to determine if we should update last time player seen online or last time no player seen online field
		let has_players_online = self
			.server
			.players
			.sample
			.as_ref()
			.is_some_and(|s| !s.is_empty());

		query.push_str(match has_players_online {
			true => "last_time_player_online, ",
			false => "last_time_no_players_online, ",
		});

		query.push_str("version_protocol, version_name, enforces_secure_chat, previews_chat, is_online_mode, favicon_hash, max_players, online_players, description_formatted, description_raw, neoforge_is_modded, fml_network_version, prevents_chat_reports, bcc_modpack_projectid, bcc_modpack_version, bcc_modpack_name) VALUES (");

		let mut query_builder = sqlx::QueryBuilder::new(query);

		query_builder
			.push_bind(self.address)
			.push(", ")
			.push_bind(self.port)
			.push(", ")
			.push_bind(self.timestamp)
			.push(", ")
			.push_bind(self.timestamp)
			.push(", ")
			.push_bind(self.timestamp)
			.push(", ")
			.push_bind(self.server.version.protocol)
			.push(", ")
			.push_bind(&self.server.version.name)
			.push(", ")
			.push_bind(self.server.enforces_secure_chat.unwrap_or(false))
			.push(", ")
			.push_bind(self.server.previews_chat.unwrap_or(false))
			.push(", ")
			.push_bind(self.server.is_server_online_mode())
			.push(", ");

		// Hash the icon using blake3
		let favicon_hash = self
			.server
			.favicon
			.as_ref()
			.and_then(|a| a.split("data:image/png;base64,").nth(1))
			.map(|str| blake3::hash(str.as_bytes()))
			.map(|a| *a.as_bytes());

		// The favicon must be inserted before the server due to foreign key constraints
		if let Some(hash) = favicon_hash {
			self.update_or_insert_favicon(hash).await.unwrap();
		}

		query_builder
			.push_bind(favicon_hash)
			.push(", ")
			.push_bind(self.server.players.max)
			.push(", ")
			.push_bind(self.server.players.online)
			.push(", ");

		// Format description if it exists
		let formatted_description = self
			.server
			.description_raw
			.as_ref()
			.map(|value| self.server.format_description(value));

		query_builder
			.push_bind(formatted_description)
			.push(", ")
			.push_bind(&self.server.description_raw)
			.push(", ")
			// Modded fields
			.push_bind(self.server.is_modded)
			.push(", ");

		// Forge mod loader network version
		let fml_network_version = self
			.server
			.forge_data
			.as_ref()
			.map(|f| f.fml_network_version);

		query_builder
			.push_bind(fml_network_version)
			.push(", ")
			.push_bind(self.server.prevents_chat_reports.unwrap_or(false))
			.push(", ");

		// Better compatibility checker
		let bcc = self.server.bcc.as_ref();

		query_builder
			.push_bind(bcc.map(|v| v.project_id))
			.push(", ")
			.push_bind(bcc.map(|v| &v.version))
			.push(", ")
			.push_bind(bcc.map(|v| &v.name));

		query_builder.push(
			") ON CONFLICT (address, port) DO UPDATE SET
			last_seen = EXCLUDED.last_seen, ",
		);

		// Update timestamps for existing servers
		query_builder.push(match has_players_online {
			true => "last_time_player_online = EXCLUDED.last_time_player_online, ",
			false => "last_time_no_players_online = EXCLUDED.last_time_no_players_online, ",
		});

		// Push remaining conflict updates
		query_builder.push(
			"version_protocol = EXCLUDED.version_protocol,
			version_name = EXCLUDED.version_name,
			enforces_secure_chat = EXCLUDED.enforces_secure_chat,
			previews_chat = EXCLUDED.previews_chat,
			is_online_mode = EXCLUDED.is_online_mode,
			favicon_hash = EXCLUDED.favicon_hash,
			max_players = EXCLUDED.max_players,
			online_players = EXCLUDED.online_players,
			description_formatted = EXCLUDED.description_formatted,
			description_raw = EXCLUDED.description_raw,
			neoforge_is_modded = EXCLUDED.neoforge_is_modded,
			fml_network_version = EXCLUDED.fml_network_version,
			prevents_chat_reports = EXCLUDED.prevents_chat_reports,
			bcc_modpack_projectid = EXCLUDED.bcc_modpack_projectid,
			bcc_modpack_version = EXCLUDED.bcc_modpack_version,
			bcc_modpack_name = EXCLUDED.bcc_modpack_name",
		);

		query_builder
			.build()
			.execute(&self.database.connection)
			.await
			.unwrap();

		Ok(())
	}

	/// Bulk insert players into the players table
	pub async fn update_or_insert_players(&self) -> anyhow::Result<()> {
		if let Some(player_sample) = &self.server.players.sample {
			let mut query_builder = QueryBuilder::new("INSERT INTO players ");

			query_builder.push_values(player_sample, |mut b, player| {
				// Disregard anonymous players if specified in the config
				if CONFIG.scanner.ignore_fake_players && !player.name.eq(ANONYMOUS_PLAYER_NAME) {
					// Ignore players that have invalid uuid's
					if let Ok(uuid) = Uuid::parse_str(&player.id) {
						b.push_bind(self.address)
							.push_bind(self.port)
							.push_bind(uuid)
							.push_bind(uuid.get_version_num() == 4)
							.push_bind(&player.name)
							.push_bind(self.timestamp)
							.push_bind(self.timestamp);
					}
				}
			});

			query_builder.push(" ON CONFLICT (address, port, uuid) DO UPDATE SET last_seen = EXCLUDED.last_seen, username = EXCLUDED.username");
			query_builder
				.build()
				.execute(&self.database.connection)
				.await?;
		}

		Ok(())
	}

	/// Bulk insert forge mods into the mods table
	pub async fn update_or_insert_mods(&self) -> anyhow::Result<()> {
		if let Some(forge_data) = &self.server.forge_data {
			let mut query_builder = QueryBuilder::new("INSERT INTO mods ");

			query_builder.push_values(&forge_data.mods, |mut b, forge_mod| {
				b.push_bind(self.address)
					.push_bind(self.port)
					.push_bind(&forge_mod.id)
					.push_bind(&forge_mod.version);
			});

			query_builder.push(" ON CONFLICT (address, port, id) DO NOTHING");
			query_builder
				.build()
				.execute(&self.database.connection)
				.await?;
		}

		Ok(())
	}

	/// Insert a servers icon into the favicons table
	async fn update_or_insert_favicon(&self, hash: [u8; 32]) -> anyhow::Result<()> {
		sqlx::query("INSERT INTO favicons VALUES ($1, $2, $3) ON CONFLICT (hash) DO UPDATE set last_seen = EXCLUDED.last_seen")
			.bind(hash.as_slice())
			.bind(&self.server.favicon)
			.bind(self.timestamp)
			.execute(&self.database.connection)
			.await?;

		Ok(())
	}
}
