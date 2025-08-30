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
   - A Minecraft server running Spigot, Paper, or a compatible fork (version X.X or higher).
   - A Redis server (version X.X or higher) installed and accessible.
   - Java X or higher.

2. **Steps**:
   - Download the latest `RedisTrade.jar` from the [Releases](https://github.com/Emibergo02/RedisTrade/releases) page.
   - Place the `RedisTrade.jar` file in your server's `plugins` folder.
   - Configure your Redis server details in the `config.yml` file (generated on first run).
   - Restart your server to load the plugin.
   - Verify the connection to Redis and adjust settings as needed.

## Configuration

After the first run, a `config.yml` file will be generated in the `plugins/RedisTrade` folder. Key configuration options include:

```yaml
redis:
  host: localhost
  port: 6379
  password: yourpassword
  database: 0
trade:
  max-trade-distance: 100
  allowed-items:
    - DIAMOND
    - EMERALD
  trade-timeout: 300
```

- `redis`: Configure your Redis server connection details.
- `trade.max-trade-distance`: Maximum distance (in blocks) between players for cross-server trades.
- `trade.allowed-items`: List of items allowed for trading.
- `trade.trade-timeout`: Time (in seconds) before an unaccepted trade request expires.

## Usage

### Commands
- `/trade <player>`: Initiate a trade request with another player (on the same or different server).
- `/trade accept`: Accept a pending trade request.
- `/trade decline`: Decline a pending trade request.
- `/trade list`: View all active trade requests.

### Permissions
- `redistrade.use`: Allows players to use the `/trade` command.
- `redistrade.admin`: Grants access to administrative commands (e.g., reloading config).

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

For questions or support, reach out via the [GitHub Issues](https://github.com/Emibergo02/RedisTrade/issues) page or contact the developer at [insert contact info if available].

---

© 2025 Emibergo02