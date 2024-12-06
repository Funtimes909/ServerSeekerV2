# ServerSeekerV2

ServerSeekerV2 is a full rewrite of the original ServerSeeker in Java, it reads output from [masscan](https://github.com/robertdavidgraham/masscan).  
Using that as input it asynchronously pings each IP address, on the port returned with a [Server List Ping](https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/Server_List_Ping) which returns the servers information, this information then gets stored in a PostgreSQL database.

If enabled in the config, it will also attempt to log in to that server and report if its whitelisted or cracked

Unlike the original ServerSeeker, V2 has some extra features:
- Faster
- Basic cracked server detection
- Basic whitelist checking
- Player Tracking
- Open Source
- Configurable
- Self Hostable (Host your own scanner and database!)
- Support for searching servers based on Forge mods


## Please also note!!
This is just the scanner!, the repository for the discord bots code can be found [here](https://github.com/Funtimes909/ServerSeekerV2-Discord-Bot)

ServerSeekerV2 is mostly ready for production use, if you find any bugs, please report them in this repository, for bugs relating to the discord bot, report them in the repository for the bot

## Goals

Some longer term goals I would like to add:
- Bedrock support
- Use of a minecraft account pool for a faster and more accurate whitelist/cracked server detection

## Getting Started
Currently, there are no prebuilt jars, you will have to build it yourself, thankfully this is easy, simply clone or download the repository locally and run `./gradlew buildShadow` the jar should be in the build/libs folder

## Storing data in the database

To store information in a database you will need to set up PostgreSQL:  

### Installation
#### Ubuntu
```sh
sudo apt-get install postgresql
```
#### Arch
```sh
sudo pacman -S postgresql
```


### Configuration
```sh
sudo -u postgres psql
```
After that you should get a terminal like this  
```
postgres=#
```  
Run the commands below to create a new user:  
```sql
ALTER USER postgres with encrypted password 'your_password';
```
Then put the new password in the `config.json` file.

## Special thanks
- [EngurRuzgar](https://github.com/EngurRuzgar): Documentation and providing me with valid testing servers, maintaining the [Python API](https://github.com/Funtimes909/ServerSeekerV2-PyAPI)
- [coolGi](https://coolgi.dev/): Code cleanup and general tips
