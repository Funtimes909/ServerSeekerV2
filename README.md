[![Forgejo](https://img.shields.io/badge/forgejo-%23F2712B.svg?style=for-the-badge&logo=forgejo&logoColor=white)](https://git.funtimes909.xyz/ServerSeekerV2/ServerSeekerV2-API)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-%234169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Rust](https://img.shields.io/badge/Rust-red?style=for-the-badge&logo=rust)](https://www.rust-lang.org/)
[![Forgejo Last Commit](https://img.shields.io/gitea/last-commit/ServerSeekerV2/ServerSeekerV2?gitea_url=https%3A%2F%2Fgit.funtimes909.xyz%2F&style=for-the-badge&logo=forgejo)](https://git.funtimes909.xyz/ServerSeekerV2/ServerSeekerV2)
[![AUR Maintainer](https://img.shields.io/aur/maintainer/serverseekerv2-git?style=for-the-badge&logo=archlinux&label=aur%20maintainer)](https://aur.archlinux.org/packages/serverseekerv2-git)
<br/>
<div align="center">
<a href="https://github.com/ShaanCoding/ReadME-Generator">
<img src="https://git.funtimes909.xyz/repo-avatars/248ef58dc8dc0ffa0a1cd47485a11703b49348540f2877b747c1846b843552b0" alt="Logo" width="80" height="80">
</a>
<h3 align="center">ServerSeekerV2</h3>
<p align="center">
Blazingly fast Minecraft server scanner written in Rust 🦀 🚀
<br/>
<br/>
<a href="https://git.funtimes909.xyz/ServerSeekerV2/ServerSeekerV2/wiki"><strong>Explore the docs »</strong></a>
<br/>
<br/>

<a href="https://git.funtimes909.xyz/ServerSeekerV2/ServerSeekerV2/issues/new">Report Bug</a> -
<a href="https://git.funtimes909.xyz/ServerSeekerV2/ServerSeekerV2/issues/new">Request Feature</a>
</p>
</div>

## About The Project

![ServerSeekerV2 Scanning Servers](https://files.nucceteere.xyz/assets/SSV2.png)

ServerSeekerV2 is a complete rewrite of the original ServerSeeker but faster and better, with more features.
ServerSeekerV2 is written in Rust allowing it to be blazingly fast and memory safe. 🦀 🚀

[Discord Server](https://discord.gg/UA5kyprunc)

[Matrix Space](https://matrix.to/#/#projects:funtimes909.xyz)

(The two are bridged together so you only need to join one)

### SSV2 is provided as is, no support is given for setting it up or using it.

## Features

- Rescanning. SSV2 Can rescan already found servers for the most up-to-date results (updates every few minutes)
- Performance. SSV2 is significantly faster than the original ServerSeeker.
- Open Source. Unlike the original, SSV2 and all it's related projects are fully open source.
- Advanced MOTD Parsing. Weather a servers description is really complex and has lots of formatting or a simple string,
  it will be built into a string with Minecraft style text formatting codes applied in place. both raw descriptions and
  formatted descriptions are saved in the database.
- Automatic opting out. Unlike the original ServerSeeker where you had to join a discord server and request your server
  be removed. You can automatically remove yourself from the database and prevent further scans by modifying your
  servers MOTD.
- Player and mod tracking. Find servers that have specific players online or servers running specific forge mods. (or both at the same time!)
- Self Hostable. Host your own scanning instance and find your own servers! (See below for warnings against running this
  on a residential network)
- Country tracking. If enabled, tracks which country and Autonomous System a server is from.

# For people just looking to not be scanned anymore

You can add "§b§d§f§d§b" to the end of your servers description by changing the ``server.properties`` file. This change
is invisible to the client and won't change the look of your servers description *in most cases.*

Additionally having this in your servers description **Will remove you from the database as well** if you were
previously scanned. The next time your server is found, it will automatically remove it from the database. Easy!

If something is wrong, or you're still being scanned after adding the above string to your servers description join
my [Matrix Space](https://matrix.to/#/#projects:funtimes909.xyz) and message ``@me:funtimes909.xyz`` directly.

## FAQ

- Q: How can I get my server removed?
- A: Add "§b§d§f§d§b" to the end of your servers MOTD, your server will be removed next time it is found.

- Q: What is this?
- A: ServerSeekerV2 is a faster version of the original ServerSeeker, it pings around 4 billion IPv4 addresses every few
  hours and attempts to join Minecraft servers on the ones that respond. This process is repeated over and over again.

- Q: I have a dynamic IP address, how can I get my server removed?
- A: I can't remove your IP address every time it changes, you will have to rely on using the MOTD method described
  above or use something like NFTables or UFW to block connections from my IP address

- Q: How can I protect my server?
- A: Enable a whitelist for your server, a whitelist allows only specified players to join your server, run
  ``/whitelist on`` and then ``/whitelist add <player>`` for every player that will join your server. Additionally,
  setting "online-mode" to true in the ``server.properties`` file helps a lot by enforcing that every player must own a
  copy of the game.

- Q: Why?
- A: As mentioned above, the previous owner of the original ServerSeeker, sold it to a third party, that got the discord
  bot and server terminated within a month of the sale. At the time I was looking for a project to sink my
  endless amounts of free time into, so shortly after the sale, I started developing this :)

- Q: Why don't you provide support for setting this up?
- A: To raise the barrier of entry for server scanning. People have often come to me asking how they can use these
  tools for griefing servers or harassing innocent people. I enjoy FOSS software and wish to keep all my software fully open source
  forever but if people want to use my software to harm others then I'm going to intentionally make it harder to use such that only people
  who actually know what they are doing can use this.

## Related projects

- [Discord Bot](https://git.funtimes909.xyz/ServerSeekerV2/ServerSeekerV2-Discord-Bot)
- [API](https://git.funtimes909.xyz/ServerSeekerV2/ServerSeekerV2-API)

### Built With

- [rust](https://www.rust-lang.org/)
- [tokio](https://crates.io/crates/tokio)
- [sqlx](https://crates.io/crates/sqlx)
- [serde](https://crates.io/crates/serde)

## Contributing

Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any
contributions you make are **greatly appreciated**.

If you have a suggestion that would make this better, please fork the repo and create a pull request. You can also
simply open an issue with the tag "enhancement".
Don't forget to give the project a star! Thanks again!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

Distributed under the GPLv3 License. See [GPLv3 License](https://opensource.org/license/gpl-3-0) for more information.
