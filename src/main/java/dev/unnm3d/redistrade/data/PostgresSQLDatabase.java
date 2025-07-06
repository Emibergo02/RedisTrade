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
public class PostgresSQLDatabase extends MySQLDatabase {

    public PostgresSQLDatabase(RedisTrade plugin, Settings.MySQL settings) {
        super(plugin, settings);
    }

    @Override
    public void connect() {
        dataSource = new HikariDataSource();
        try {
            Class.forName(settings.driverClass());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        dataSource.setJdbcUrl("jdbc:postgresql://" +
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
                statement.execute("CREATE DOMAIN tinyint as smallint;");//Adapted from MariaDB
            } catch (SQLException ignored) {
                // Ignore if the domain already exists
            }
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
                         INSERT INTO archived (trade_uuid,trade_timestamp,trader_uuid,trader_name,trader_rating,trader_price,
                         customer_uuid,customer_name,customer_rating,customer_price,trader_items,customer_items)
                         VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                         ON CONFLICT (trade_uuid)
                         DO UPDATE SET trade_timestamp=EXCLUDED.trade_timestamp,
                         trader_uuid=EXCLUDED.trader_uuid, trader_name=EXCLUDED.trader_name, trader_rating=EXCLUDED.trader_rating,
                         trader_price=EXCLUDED.trader_price, customer_uuid=EXCLUDED.customer_uuid, customer_name=EXCLUDED.customer_name,
                         customer_rating=EXCLUDED.customer_rating, customer_price=EXCLUDED.customer_price,
                         trader_items=EXCLUDED.trader_items, customer_items=EXCLUDED.customer_items
                         ;""")) {
                statement.setString(1, trade.getUuid().toString());
                statement.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                //Trader side
                statement.setString(3, trade.getTraderSide().getTraderUUID().toString());
                statement.setString(4, trade.getTraderSide().getTraderName());
                statement.setInt(5, trade.getTraderSide().getOrder().getRating());
                statement.setString(6, gson.toJson(trade.getTraderSide().getOrder().getPrices()));
                //Customer side
                statement.setString(7, trade.getCustomerSide().getTraderUUID().toString());
                statement.setString(8, trade.getCustomerSide().getTraderName());
                statement.setInt(9, trade.getCustomerSide().getOrder().getRating());
                statement.setString(10, gson.toJson(trade.getCustomerSide().getOrder().getPrices()));

                statement.setString(11, new String(
                        trade.getTraderSide().getOrder().getVirtualInventory().serialize(), StandardCharsets.ISO_8859_1));
                statement.setString(12, new String(
                        trade.getCustomerSide().getOrder().getVirtualInventory().serialize(), StandardCharsets.ISO_8859_1));
                return statement.executeUpdate() != 0;
            } catch (SQLException e) {
                plugin.getIntegritySystem().handleStorageException(new RedisTradeStorageException(e, RedisTradeStorageException.ExceptionSource.ARCHIVE_TRADE, trade.getUuid()));
                return false;
            }
        });
    }

    @Override
    public void backupTrade(NewTrade trade) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO backup (trade_uuid, server_id, serialized)
                        VALUES (?,?,?)
                     ON CONFLICT (trade_uuid)
                     DO UPDATE SET trade_uuid = EXCLUDED.trade_uuid,server_id = EXCLUDED.server_id, serialized = EXCLUDED.serialized;""")) {
            statement.setString(1, trade.getUuid().toString());
            statement.setInt(2, RedisTrade.getServerId());
            statement.setString(3, new String(trade.serialize(), StandardCharsets.ISO_8859_1));
            if (statement.executeUpdate() != 0) {
                RedisTrade.debug("Trade " + trade.getUuid() + " backed up");
            }
        } catch (Exception e) {
            plugin.getIntegritySystem().handleStorageException(new RedisTradeStorageException(e, RedisTradeStorageException.ExceptionSource.BACKUP_TRADE, trade.getUuid()));
        }
    }

    @Override
    public void updateStoragePlayerList(String playerName, UUID playerUUID) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         INSERT INTO player_list (player_name,player_uuid)
                            VALUES (?,?)
                         ON CONFLICT (player_name)
                         DO UPDATE SET player_name = EXCLUDED.player_name, player_uuid = EXCLUDED.player_uuid;""")) {
                statement.setString(1, playerName);
                statement.setString(2, playerUUID.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getIntegritySystem().handleStorageException(new RedisTradeStorageException(e, RedisTradeStorageException.ExceptionSource.PLAYERLIST));
            }
        });
    }

}
