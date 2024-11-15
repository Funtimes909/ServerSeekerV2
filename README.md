
# ServerSeekerV2
[![88x31](https://odpay.net/88x31.gif)](https://odpay.net)

ServerSeekerV2 is a full rewrite of the original ServerSeeker in Java, it takes a JSON output file from [masscan](https://github.com/robertdavidgraham/masscan).  
Using that as input it asynchronously pings each IP address, on the port returned with a [Server List Ping](https://wiki.vg/Server_List_Ping) which returns the servers information, this information then gets stored in a PostgreSQL database.
Unlike the original ServerSeeker, V2 has some extra features:
- Faster
- Open Source
- Configurable
- Self Hostable (Host your own scanner and database!)
- Support for searching servers based on Forge mods
- Being able to detect if (some) servers are running as a cracked server

**Please also note!!**
This is just a backend, there's no frontend for easily searching for servers from the database yet, I will make a discord bot and a page on my website to do this in time, so don't worry

## Currently under heavy development!!
ServerSeekerV2 is **NOT** production ready! Please report any issues that you find, although I'm probably aware of most of them already, it would be good to track them.

Currently lacking features:
- Async and concurrency support (it will be unusably slow for large scans)
- General code cleanup and refactoring

Some longer term goals I would like to add:
- Bedrock support
- Support for scanning across multiple servers for increased scanning speed

## Getting Started
Currently, there are no prebuilt jars, you will have to build it yourself, thankfully this is easy, simply clone or download the repository locally and run `./gradlew buildShadow` the jar should be in the build/libs folder

To get information about the servers IP address (country, ASN), you will need to make an account with [IpInfo](https://ipinfo.io), after making an account, go to your dashboard and your token should be at the bottom of the page, put this token in the config.json file

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
- [EngurRuzgar](https://github.com/EngurRuzgar): Documentation and providing me with valid testing servers
- [coolGi](https://github.com/coolGi69): Code cleanup and general tips
