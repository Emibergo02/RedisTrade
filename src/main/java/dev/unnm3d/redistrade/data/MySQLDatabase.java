package dev.unnm3d.redistrade.data;

import com.zaxxer.hikari.HikariDataSource;
import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.integrity.RedisTradeStorageException;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Setter
@Getter
public class MySQLDatabase extends SQLiteDatabase {
    private final Settings.MySQL settings;

    public MySQLDatabase(RedisTrade plugin, Settings.MySQL settings) {
        super(plugin);
        this.settings = settings;
    }

    @Override
    public void connect() {
        dataSource = new HikariDataSource();
        final String databaseType = settings.driverClass().contains("mariadb") ? "mariadb" : "mysql";

        dataSource.setJdbcUrl("jdbc:" + databaseType + "://" +
                settings.databaseHost() + ":" + settings.databasePort() + "/" +
                settings.databaseName());
        dataSource.setUsername(settings.databaseUsername());
        dataSource.setPassword(settings.databasePassword());

        dataSource.setMaximumPoolSize(settings.maximumPoolSize());
        dataSource.setMinimumIdle(settings.minimumIdle());
        dataSource.setMaxLifetime(settings.maxLifetime());
        dataSource.setKeepaliveTime(settings.keepAliveTime());
        dataSource.setConnectionTimeout(settings.connectionTimeout());
        dataSource.setPoolName("RedisTradeHikariPool");

        final Properties properties = new Properties();
        properties.putAll(
                Map.of("cachePrepStmts", "true",
                        "prepStmtCacheSize", "250",
                        "prepStmtCacheSqlLimit", "2048",
                        "useServerPrepStmts", "true",
                        "useLocalSessionState", "true",
                        "useLocalTransactionState", "true"
                ));
        properties.putAll(
                Map.of(
                        "rewriteBatchedStatements", "true",
                        "cacheResultSetMetadata", "true",
                        "cacheServerConfiguration", "true",
                        "elideSetAutoCommits", "true",
                        "maintainTimeStats", "false")
        );
        dataSource.setDataSourceProperties(properties);

        try (Connection connection = dataSource.getConnection()) {
            final String[] databaseSchema = getSchemaStatements("schema.sql");
            try (Statement statement = connection.createStatement()) {
                for (String tableCreationStatement : databaseSchema) {
                    statement.execute(tableCreationStatement);
                }
                connected = true;
            } catch (SQLException e) {
                close();
                throw new IllegalStateException("Failed to create database tables. Please ensure you are running MySQL v8.0+ or MariaDB " +
                        "and that your connecting user account has privileges to create tables.", e);
            }
        } catch (SQLException | IOException e) {
            close();
            throw new IllegalStateException("Failed to establish a connection to the MySQL/MariaDB database. " +
                    "Please check the supplied database credentials in the config file", e);
        }

    }

    @Override
    public CompletableFuture<Boolean> archiveTrade(NewTrade trade) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         INSERT INTO `archived` (trade_uuid,trader_uuid,target_uuid,timestamp,serialized)
                         VALUES (?,?,?,?,?)
                         ON DUPLICATE KEY UPDATE trade_uuid=VALUES(trade_uuid), trader_uuid=VALUES(trader_uuid),
                         target_uuid = VALUES(target_uuid), timestamp = VALUES(timestamp), serialized=VALUES(serialized)
                         ;""")) {
                statement.setString(1, trade.getUuid().toString());
                statement.setString(2, trade.getTraderSide().getTraderUUID().toString());
                statement.setString(3, trade.getOtherSide().getTraderUUID().toString());
                statement.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                statement.setString(5, new String(trade.serialize(), StandardCharsets.ISO_8859_1));
                return statement.executeUpdate() != 0;
            } catch (SQLException e) {
                plugin.getIntegritySystem().handleStorageException(new RedisTradeStorageException(e, RedisTradeStorageException.ExceptionSource.ARCHIVE_TRADE, trade.getUuid()));
                return false;
            }
        });
    }

    @Override
    public void backupTrade(NewTrade trade) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         INSERT INTO `backup` (trade_uuid,server_id,serialized)
                            VALUES (?,?,?)
                         ON DUPLICATE KEY UPDATE trade_uuid = VALUES(trade_uuid),server_id = VALUES(server_id), serialized = VALUES(serialized);""")) {
                statement.setString(1, trade.getUuid().toString());
                statement.setInt(2, RedisTrade.getServerId());
                statement.setString(3, new String(trade.serialize(), StandardCharsets.ISO_8859_1));
                statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getIntegritySystem().handleStorageException(new RedisTradeStorageException(e, RedisTradeStorageException.ExceptionSource.BACKUP_TRADE, trade.getUuid()));
            }
        });
    }

    @Override
    public void updateStoragePlayerList(String playerName, UUID playerUUID) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         INSERT INTO `player_list` (player_name,player_uuid)
                            VALUES (?,?)
                         ON DUPLICATE KEY UPDATE player_name = VALUES(player_name), player_uuid = VALUES(player_uuid);""")) {
                statement.setString(1, playerName);
                statement.setString(2, playerUUID.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getIntegritySystem().handleStorageException(new RedisTradeStorageException(e, RedisTradeStorageException.ExceptionSource.PLAYERLIST));
            }
        });
    }

    @Override
    public void ignorePlayer(String playerName, String targetName, boolean ignore) {
        CompletableFuture.runAsync(() -> {
            String query = ignore ?
                    "INSERT INTO `ignored_players` (ignorer,ignored) VALUES (?,?);" :
                    "DELETE FROM `ignored_players` WHERE ignored = ? AND ignorer = ?;";
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, playerName);
                statement.setString(2, targetName);
                statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getIntegritySystem().handleStorageException(new RedisTradeStorageException(e, RedisTradeStorageException.ExceptionSource.IGNORED_PLAYER));
            }
        });
    }

    @Override
    public void close() {
        if (dataSource == null) return;
        if (dataSource.isClosed()) return;
        dataSource.close();
        connected = false;
    }
}
