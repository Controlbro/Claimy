# Claimy

Claimy is a lightweight, modern land-claim plugin for Minecraft servers.  
It allows players to claim and protect land while giving server owners full control over how claims behave.

Originally developed for the TBFMC Minecraft server, Claimy focuses on performance, simplicity, and configurability without changing the vanilla survival feel.

---

## Features

- Chunk-based land claiming
- Region and claim protection (block break/place/interact)
- Member and trust system for claims
- Configurable claim flags (pvp, explosions, fire, etc.)
- Admin tools to manage and override claims
- Designed for Paper / modern Minecraft servers
- Clean and extensible codebase

---

## Installation

1. Download the latest Claimy jar from the Releases page
2. Place the jar into your server's `plugins/` folder
3. Start the server to generate configuration files
4. Edit `config.yml` to fit your server rules
5. Restart the server

---

## Commands

Example commands;

/town create  
Create a claim at your current location

/town delete  
Delete the claim you are currently standing in

/town claim
Claim the current chunk you are standing in so long as it is touching another claimed chunk

/town border and /town border stay
Display town border or perm display town border until ran again for stay.

/town claim auto
auto claim every chunk you walk into so long as it is touching another claimed chunk.

/town  
View information about the claim you are in

/town resident
Allow a player to build and interact in your claim

/town kick <player>  
Remove a player's access from your claim

---

## Permissions

claimy.use  
Allows players to create and manage their own claims

claimy.admin  
Allows administrative claim management

---

## Configuration

Claimy includes a fully configurable `config.yml` where you can:

- Enable or disable claim protection features
- Configure default claim flags
- Restrict claiming to specific worlds
- Control limits such as maximum claims or sizes
- Enable/Disable Squaremap Compatibility

---

## Development & Building

Claimy uses Maven and requires Java 21 or newer.

To build locally:

mvn clean package

The compiled jar will be located in the `target/` directory.

---

## Contributing

Pull requests and issues are welcome.  
Please open an issue before making major changes.

---

## Support

For issues, feature requests, or help, open an issue on GitHub.
