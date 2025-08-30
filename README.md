# RedisTrade

A cross-server trades plugin for Minecraft servers, designed with a focus on consistency, reliability, and seamless player trading across multiple servers.

## Overview

RedisTrade enables players to trade items, currency, or other in-game assets securely across different Minecraft servers using Redis as a backend for fast and reliable data synchronization. The plugin ensures consistent trade states, prevents duplication exploits, and provides a user-friendly interface for players to initiate and complete trades.

## Features

- **Cross-Server Trading**: Players on different servers can trade items or currency in real-time.
- **Redis Integration**: Leverages Redis for fast, reliable, and scalable data management.
- **Consistency & Reliability**: Ensures trade integrity with robust transaction handling to prevent item loss or duplication.
- **User-Friendly Interface**: Simple commands and GUI (if applicable) for initiating and managing trades.
- **Customizable**: Configurable settings for trade limits, allowed items, and more.
- **Anti-Exploit Mechanisms**: Built-in checks to prevent trade abuse or server crashes.

## Installation

1. **Prerequisites**:
   - A Minecraft server running Spigot, Paper, or a compatible fork (version 1.20.6 or higher).
   - A Redis server installed and accessible.
   - Java 21 or higher.

2. **Steps**:
   - Download the latest `RedisTrade.jar` from the [SpigotMC](https://www.spigotmc.org/resources/redistrade%E2%9A%A1cross-server-trades%E2%9A%A1plug-play.120797/) or [Modrinth](https://modrinth.com/plugin/redistrade) page.
   - Place the `RedisTrade.jar` file in your server's `plugins` folder.
   - Restart your server to load the plugin.

## Usage

### Commands
- `/trade <player>`: Initiate a trade request with another player.
- `/trade`: Resume trade
- `/trade-browse`: Browse trades

### Permissions
- `redistrade.trade`: Allows players to use the `/trade` command.
- `redistrade.usecurrency.<currencyName>`: Allows the use of a currency for a player
- `redistrade.browse`: Browse archived trades
- `redistrade.admin`: Grants access to administrative commands (e.g., reloading config or using browser).

## Contributing

Contributions are welcome! To contribute:

1. Fork the repository.
2. Create a new branch (`git checkout -b feature/your-feature`).
3. Make your changes and commit (`git commit -m 'Add your feature'`).
4. Push to your branch (`git push origin feature/your-feature`).
5. Open a Pull Request.

Please ensure your code follows the project's coding style and includes appropriate tests.

## Issues

If you encounter bugs or have feature requests, please open an issue on the [Issues](https://github.com/Emibergo02/RedisTrade/issues) page. Provide as much detail as possible, including server version, plugin version, and error logs.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Contact

For questions or support, reach out via the [Discord](https://discord.gg/c6MBaKtkDc) server or contact the developer at [insert contact info if available].

---

© 2025 Emibergo02
