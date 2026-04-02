pub mod country_tracking;

use std::str::FromStr;

use crate::protocol::response::MinecraftPlayers;

use super::protocol::response::MinecraftServer;
use sqlx::postgres::PgQueryResult;
use sqlx::types::ipnet::IpNet;
use sqlx::types::Uuid;
use sqlx::{Pool, Postgres, QueryBuilder, Row};

const INSERT_SERVERS_QUERY: &str = "INSERT INTO servers VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19, $20, $21, $22, $23) ON CONFLICT (address, port) DO UPDATE SET last_seen = EXCLUDED.last_seen, version_protocol = EXCLUDED.version_protocol, version_name = EXCLUDED.version_name, enforces_secure_chat = EXCLUDED.enforces_secure_chat, previews_chat = EXCLUDED.previews_chat, is_online_mode = EXCLUDED.is_online_mode, max_players = EXCLUDED.max_players, online_players = EXCLUDED.online_players, description_formatted = EXCLUDED.description_formatted, description_raw = EXCLUDED.description_raw, is_modded = EXCLUDED.is_modded, fml_network_version = EXCLUDED.fml_network_version, prevents_chat_reports = EXCLUDED.prevents_chat_reports, bcc_modpack_projectid = EXCLUDED.bcc_modpack_projectid, bcc_modpack_version = EXCLUDED.bcc_modpack_version, bcc_modpack_name = EXCLUDED.bcc_modpack_name";

#[derive(Debug, Clone)]
pub struct Database {
	pub connection: Pool<Postgres>
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
	pub timestamp: chrono::NaiveDateTime,
	pub database: Database,
}

impl ServerUpdateOperation {
	/// Inserts a single server into the database
	pub async fn update_or_insert_server(&self) -> anyhow::Result<()> {
		let favicon_hash = self
			.server
			.favicon
			.as_ref()
			.map(|x| MinecraftServer::get_favicon_hash(x));

		let formatted_description = self
			.server
			.description_raw
			.as_ref()
			.map(|value| self.server.format_description(value));

		let bcc = self.server.bcc.as_ref();

		sqlx::query(INSERT_SERVERS_QUERY)
			.bind(self.address)
			.bind(self.port)
			// first_seen
			.bind(self.timestamp)
			// last_seen
			.bind(self.timestamp)
			// last_time_player_seen_online
			.bind(self.timestamp)
			// last_time_no_players_seen_online
			.bind(self.timestamp)
			.bind(&self.server.version.protocol)
			.bind(&self.server.version.name)
			.bind(self.server.enforces_secure_chat.unwrap_or(false))
			.bind(self.server.previews_chat.unwrap_or(false))
			.bind(self.server.is_server_online_mode())
			// Whitelist checking, not implemented
			.bind(false)
			// TODO! Verify this works
			.bind(self.server.players.max)
			.bind(self.server.players.online)
			.bind(formatted_description)
			.bind(&self.server.description_raw)
			// Modded fields

			.bind(self.server.is_modded)
			.bind(self.server.forge_data.as_ref().map(|f| f.fml_network_version))
			.bind(self.server.prevents_chat_reports.unwrap_or(false))

			// Better Compatibility checker
			.bind(bcc.map(|v| v.project_id))
			.bind(bcc.map(|v| &v.version))
			.bind(bcc.map(|v| &v.name))
			.execute(&self.database.connection)
			.await
			.unwrap();

		Ok(())
	}

	/// Bulk insert players into the players table
	pub async fn update_or_insert_players(&self) -> anyhow::Result<()> {
		if let Some(player_sample) = &self.server.players.sample {
			let mut query_builder = QueryBuilder::new(
				"INSERT INTO players (address, port, uuid, username, is_online_mode, first_seen, last_seen) ",
			);

			query_builder.push_values(player_sample, |mut b, player| {

				// Ignore player if uuid fails to parse
				if let Ok(parsed_uuid) = Uuid::from_str(&player.id) {
					b.push_bind(self.address)
						.push_bind(self.port)
						.push_bind(parsed_uuid)
						.push_bind(&player.name)
						.push_bind(parsed_uuid.get_version_num() == 4)
						.push_bind(self.timestamp)
						.push_bind(self.timestamp);
				}
			});

			query_builder.push("ON CONFLICT (address, port, uuid) DO UPDATE SET last_seen = EXCLUDED.last_seen, username = EXCLUDED.username");
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
			let mut query_builder =
				QueryBuilder::new("INSERT INTO mods (address, port, id, mod_marker) ");

			query_builder.push_values(&forge_data.mods, |mut b, forge_mod| {
				b.push_bind(self.address)
					.push_bind(self.port)
					.push_bind(&forge_mod.id)
					.push_bind(&forge_mod.version)
					.push_bind(self.timestamp);
			});

			query_builder.push("ON CONFLICT (address, port, id) DO NOTHING");
			query_builder
				.build()
				.execute(&self.database.connection)
				.await?;
		}

		Ok(())
	}

	pub async fn update_or_insert_favicon(&self) -> Result<(), sqlx::Error> {
		if let Some(base64) = &self.server.favicon {
			let favicon_hash = MinecraftServer::get_favicon_hash(base64);

			sqlx::query("INSERT INTO favicons (hash, data, first_seen) VALUES ($1, $2, $3) ON CONFLICT (hash) DO NOTHING")
				.bind(favicon_hash.as_slice())
				.bind(base64)
				.bind(self.timestamp)
				.execute(&self.database.connection)
				.await?;
		}

		Ok(())
	}
}
