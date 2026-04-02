use anyhow::bail;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::TcpStream;

// A simple 9 byte payload that will get a response from most servers
const SIMPLE_PAYLOAD: [u8; 9] = [
	6, // Size: Amount of bytes in the message
	0, // ID: Has to be 0
	0, // Protocol Version: Can be anything as long as it's a valid varint
	0, // Server address
	0, 0, // Port: Can be anything (Notchian servers don't this)
	1, // Next state: 1 for status, 2 for login. Therefore, has to be 1
	1, // Size
	0, // ID
];

/// This function does the bare minimum to get a response from majority of minecraft servers
/// some servers will require a proper handshake sequence
pub async fn simple_ping(stream: &mut TcpStream) -> anyhow::Result<String> {
	stream.write_all(&SIMPLE_PAYLOAD).await?;
	let mut response = [0; 1024];

	// The index is used to point to the position at the start of the string.
	// It gets increased by the amount of bytes read to decode the packet ID, Packet length
	// And string length
	let mut index = 0;

	// Returns how many bytes were read from the stream into the buffer
	let total_read_bytes = stream.read(&mut response).await?;

	if total_read_bytes == 0 {
		bail!("Read bytes is 0");
	}

	// Packet length
	index += decode_varint(&response).1;

	// Since Packet ID should always be 0 and will never take more than 1 byte to encode
	// We can ignore it entirely and just advance the index by 1
	index += 1;

	// Decode the string length
	let (string_length, string_length_bytes) = decode_varint(&response[index as usize..]);
	index += string_length_bytes;

	if string_length == 0 || string_length > 32767 {
		bail!("String length is 0 or string length is too big");
	}

	// WARNING: Don't allocate vec size based on what the server says it needs from the varint.
	// Allocate size based on what the server *actually* sends back, some servers can crash the
	// program by attempting to allocate insane amounts of memory this way.
	//
	// Adds everything we have read so far minus the packet ID and packet length to a new vec
	let mut output = Vec::from(&response[index as usize..total_read_bytes]);
	let string_length = string_length + index as usize;

	if total_read_bytes > string_length {
		bail!("String is bigger than total read bytes");
	}

	// Read the rest of the servers JSON
	stream
		// Takes everything after the end of the data we already have in the buffer
		// Up until the end of the strings length
		.take((string_length - total_read_bytes) as u64)
		.read_to_end(&mut output)
		.await?;

	Ok(String::from_utf8_lossy_owned(output))
}

// returns the decoded varint and how many bytes were read
#[inline(always)]
fn decode_varint(bytes: &[u8]) -> (usize, u8) {
	let mut value: usize = 0;
	let mut count: u8 = 0;

	for b in bytes {
		value |= ((b & 0x7F) as usize) << count;

		// If resulting value is 0 it means this is the end of the varint
		if (b >> 7) != 1 {
			break;
		}

		count += 7;
	}

	(value, (count / 7) + 1)
}
