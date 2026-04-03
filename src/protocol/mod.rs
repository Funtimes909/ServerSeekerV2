pub mod minecraft;
pub mod response;

pub enum MinecraftColorCodes {
	Black,
	DarkBlue,
	DarkGreen,
	DarkAqua,
	DarkRed,
	DarkPurple,
	Gold,
	Gray,
	DarkGray,
	Blue,
	Green,
	Aqua,
	Red,
	LightPurple,
	Yellow,
	White,
	Reset,
	UnknownValue,
}

impl From<&str> for MinecraftColorCodes {
	fn from(s: &str) -> Self {
		use MinecraftColorCodes::*;

		match s {
			"black" => Black,
			"dark_blue" => DarkBlue,
			"dark_green" => DarkGreen,
			"dark_aqua" => DarkAqua,
			"dark_red" => DarkRed,
			"dark_purple" | "purple" => DarkPurple,
			"gold" => Gold,
			"gray" | "grey" => Gray,
			"dark_gray" | "dark_grey" => DarkGray,
			"blue" => Blue,
			"green" => Green,
			"aqua" => Aqua,
			"red" => Red,
			"pink" | "light_purple" => LightPurple,
			"yellow" => Yellow,
			"white" => White,
			"reset" => Reset,
			_ => UnknownValue,
		}
	}
}

impl MinecraftColorCodes {
	pub fn get_code(&self) -> char {
		use MinecraftColorCodes::*;

		match self {
			Black => '0',
			DarkBlue => '1',
			DarkGreen => '2',
			DarkAqua => '3',
			DarkRed => '4',
			DarkPurple => '5',
			Gold => '6',
			Gray => '7',
			DarkGray => '8',
			Blue => '9',
			Green => 'a',
			Aqua => 'b',
			Red => 'c',
			LightPurple => 'd',
			Yellow => 'e',
			White => 'f',
			Reset => 'r',
			// TODO: Currently its only servers that respond with hex values as colors that don't match
			// Maybe theres a way with color averaging to fix this?
			UnknownValue => 'r',
		}
	}
}
