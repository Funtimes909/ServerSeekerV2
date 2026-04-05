use std::{collections::HashSet, str::FromStr};

use super::MinecraftColorCodes;
use serde::{Deserialize, Serialize};
use serde_json::Value;
use sqlx::types::Uuid;

const OPT_OUT_CODE: &str = "§b§d§f§d§b";
const ANONYMOUS_PLAYER_NAME: &str = "Anonymous Player";
const FAKE_PLAYER_SAMPLE_MESSAGE: &str =
	"To protect the privacy of this server and its\nusers, you must log in once to see ping data.";

#[derive(Debug, Deserialize, Serialize, Clone)]
pub struct MinecraftServer {
	pub version: MinecraftVersion,
	pub favicon: Option<String>,
	pub players: MinecraftPlayers,
	#[serde(rename = "description")]
	pub description_raw: Option<Value>,
	pub description_formatted: Option<String>,
	#[serde(rename = "enforcesSecureChat")]
	pub enforces_secure_chat: Option<bool>,
	#[serde(rename = "previewsChat")]
	pub previews_chat: Option<bool>,

	// Neoforge
	#[serde(rename = "isModded")]
	pub is_modded: Option<bool>,

	// Forge
	#[serde(rename = "forgeData", alias = "modinfo")]
	pub forge_data: Option<ForgeData>,

	// No chat reports
	#[serde(rename = "preventsChatReports")]
	pub prevents_chat_reports: Option<bool>,

	// Better Compatibility Checker
	#[serde(rename = "betterStatus")]
	pub bcc: Option<BetterCompatibilityChecker>,
}

#[derive(Deserialize, Serialize, PartialEq, Clone, Debug)]
pub struct MinecraftVersion {
	pub name: String,
	pub protocol: i32,
}

#[derive(Deserialize, Serialize, PartialEq, Clone, Debug)]
pub struct MinecraftPlayers {
	pub max: i32,
	pub online: i32,
	pub sample: Option<Vec<MinecraftPlayer>>,
}

#[derive(Deserialize, Serialize, PartialEq, Clone, Debug)]
pub struct MinecraftPlayer {
	pub id: String,
	pub name: String,
}

#[derive(Deserialize, Serialize, PartialEq, Clone, Debug)]
pub struct ForgeData {
	#[serde(rename = "fmlNetworkVersion")]
	pub fml_network_version: Option<i32>,
	pub truncated: Option<bool>,

	#[serde(rename = "d")]
	pub forge_encoded_data: Option<String>,

	#[serde(rename = "mods", alias = "modList")]
	pub mods: Vec<MinecraftMod>,
}

#[derive(Deserialize, Serialize, PartialEq, Clone, Debug)]
pub struct MinecraftMod {
	#[serde(rename = "modId", alias = "modid")]
	pub id: String,
	#[serde(rename = "modmarker", alias = "version")]
	pub version: String,
}

#[derive(Deserialize, Serialize, PartialEq, Clone, Debug)]
pub struct BetterCompatibilityChecker {
	#[serde(rename = "projectID")]
	pub project_id: Option<i32>,
	pub name: String,
	pub version: String,
}

impl MinecraftServer {
	// Checks if this server is running in offline mode by checking if any of the players uuid version is anything other than 4
	pub fn is_server_online_mode(&self) -> bool {
		if let Some(sample) = &self.players.sample {
			sample
				.iter()
				.filter_map(|p| Uuid::from_str(&p.id).ok())
				.any(|a| a.get_version_num() == 4)
		} else {
			false
		}
	}

	/// Checks various methods to see if the response the server sent
	/// contains fake player information
	pub fn is_fake_player_sample(&self) -> bool {
		let mut seen_uuids = HashSet::new();
		let mut is_fake_sample = false;

		// Servers with this description hide player data
		if self
			.description_formatted
			.as_ref()
			.is_some_and(|d| d.eq(FAKE_PLAYER_SAMPLE_MESSAGE))
		{
			return true;
		}

		// Check for duplicate uuids
		if let Some(a) = &self.players.sample {
			a.iter().for_each(|a| {
				if let Ok(uuid) = Uuid::parse_str(&a.id) {
					if seen_uuids.contains(&uuid) {
						is_fake_sample = true
					}

					seen_uuids.insert(uuid);
				}
			})
		}

		is_fake_sample
	}

	// Check if the user has opted out of scanning
	pub fn has_opted_out(&self) -> bool {
		match &self.description_formatted {
			Some(description) => String::from(description).contains(OPT_OUT_CODE),
			None => false,
		}
	}

	#[rustfmt::skip]
	/// Format a servers description object as close as i can be bothered to get it
	pub fn format_description(&self, value: &Value) -> String {
		let mut output = String::new();

		match value {
			Value::String(s) => output.push_str(s),
			Value::Array(array) => {
				for value in array {
					output.push_str(&self.format_description(value));
				}
			}
			Value::Object(object) => {
				for (key, value) in object {
					match key.as_str() {
						"obfuscated" => {
							if let Some(b) = value.as_bool() && b {
								output.push_str("§k")
							}
						},
						"bold" => {
							if let Some(b) = value.as_bool() && b {
								output.push_str("§l")
							}
						},
						"strikethrough" => {
							if let Some(b) = value.as_bool() && b {
								output.push_str("§m")
							}
						},
						"underline" => {
							if let Some(b) = value.as_bool() && b {
								output.push_str("§n")
							}
						},
						"italic" => {
							if let Some(b) = value.as_bool() && b {
								output.push_str("§o")
							}
						},
						"color" => {
							if let Some(c) = value.as_str() {
								let color = MinecraftColorCodes::from(c);
								output.push_str(format!("§{}", color.get_code()).as_str())
							}
						},
						_ => (),
					}
				}

				// MiniMOTD can put the "extra" field before the text field, this causes some servers
				// using it to format incorrectly unless we specifically add the text AFTER
				// all other format codes but BEFORE the extra field
				if object.contains_key("text") {
					if let Some(text) = object.get("text") {
						if let Some(text) = text.as_str() {
							output.push_str(text);
						}
					}
				}

				if object.contains_key("extra") {
					if let Some(extra) = object.get("extra") {
						output.push_str(&self.format_description(extra));
					}
				}
			}
			_ => {}
		}

		output
	}
}
