use anyhow::bail;
use std::{
	net::{Ipv4Addr, SocketAddrV4},
	time::Duration,
};
use tokio::{net::TcpStream, task::JoinHandle};

use crate::protocol::{minecraft, response::MinecraftServer};

pub mod discovery;
pub mod rescanner;

pub fn run(address: Ipv4Addr, port: u16) -> JoinHandle<anyhow::Result<MinecraftServer>> {
	tokio::spawn(async move {
		let socket = SocketAddrV4::new(address, port);
		let mut stream = TcpStream::connect(socket).await.unwrap();

		return match tokio::time::timeout(
			Duration::from_secs(4),
			minecraft::simple_ping(&mut stream),
		)
		.await
		{
			Ok(Ok(s)) => {
				match serde_json::from_str::<MinecraftServer>(&s) {
					Ok(server) => Ok(server),
					Err(e) => bail!("{e}"),
				}
			}
			Ok(Err(e)) => bail!("{e}"),
			Err(e) => bail!("{e}"),
		};
	})
}
