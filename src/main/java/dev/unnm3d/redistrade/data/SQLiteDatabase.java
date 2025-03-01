package dev.unnm3d.redistrade.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.core.OrderInfo;
import dev.unnm3d.redistrade.core.TradeSide;
import dev.unnm3d.redistrade.core.enums.Actor;
import dev.unnm3d.redistrade.core.enums.Status;
import dev.unnm3d.redistrade.integrity.RedisTradeStorageException;
import lombok.Getter;
import xyz.xenondevs.invui.inventory.VirtualInventory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.logging.Level;

public class SQLiteDatabase implements Database {

    protected final RedisTrade plugin;
    @Getter
    protected boolean connected = false;
    protected HikariDataSource dataSource;
    protected Gson gson = new Gson();


    public SQLiteDatabase(RedisTrade plugin) {
        this.plugin = plugin;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void connect() {
        final File databaseFile = new File(RedisTrade.getInstance().getDataFolder(), "redistrade.db");
        try {
            if (databaseFile.createNewFile()) {
                plugin.getLogger().info("Created the SQLite database file");
            }

            Class.forName("org.sqlite.JDBC");

            HikariConfig config = new HikariConfig();
            config.setPoolName("RedisTradeHikariPool");
            config.setDriverClassName("org.sqlite.JDBC");
            config.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            config.setConnectionTestQuery("SELECT 1");
            config.setMaxLifetime(60000);
            config.setIdleTimeout(45000);
            config.setMaximumPoolSize(50);
            dataSource = new HikariDataSource(config);

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "An exception occurred creating the database file", e);
        } catch (ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load the necessary SQLite driver", e);
        }

        //Backup the database file
        if (!databaseFile.exists()) {
            return;
        }
        final File backup = new File(databaseFile.getParent(), String.format("%s.bak", databaseFile.getName()));
        try {
            if (!backup.exists() || backup.delete()) {
                Files.copy(databaseFile.toPath(), backup.toPath());
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to backup flat file database", e);
        }

        //Initialize the database
        try {
            final String[] databaseSchema = getSchemaStatements("schema.sql");
            for (String tableCreationStatement : databaseSchema) {
                try (Statement statement = getConnection().createStatement()) {
                    statement.execute(tableCreationStatement);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            close();
            throw new IllegalStateException("Failed to create database tables.", e);
        }
        connected = true;
    }


    @Override
    public void close() {
        try {
            if (getConnection() != null) {
                if (!getConnection().isClosed()) {
                    getConnection().close();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        connected = false;
    }

    @Override
    public CompletableFuture<Boolean> archiveTrade(NewTrade trade) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         INSERT OR REPLACE INTO `archived` (trade_uuid,trade_timestamp,trader_uuid,trader_name,trader_rating,trader_price,
                         customer_uuid,customer_name,customer_rating,customer_price,trader_items,customer_items)
                         VALUES (?,?,?,?,?,?,?,?,?,?,?,?);""")) {
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
    public CompletableFuture<Map<Long, NewTrade>> getArchivedTrades(UUID playerUUID, LocalDateTime startTimestamp, LocalDateTime endTimestamp) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         SELECT * FROM archived WHERE trader_uuid = ? OR customer_uuid = ? AND trade_timestamp BETWEEN ? AND ?
                         ORDER BY trade_timestamp DESC;""")) {
                statement.setString(1, playerUUID.toString());
                statement.setString(2, playerUUID.toString());
                statement.setTimestamp(3, Timestamp.valueOf(startTimestamp));
                statement.setTimestamp(4, Timestamp.valueOf(endTimestamp));

                try (ResultSet result = statement.executeQuery()) {
                    final Map<Long, NewTrade> trades = new LinkedHashMap<>();
                    while (result.next()) {
                        try {
                            trades.put(result.getTimestamp("trade_timestamp").getTime(), tradeFromResultSet(result));
                        } catch (Exception e) {
                            plugin.getIntegritySystem().handleStorageException(new RedisTradeStorageException(e, RedisTradeStorageException.ExceptionSource.SERIALIZATION));
                        }
                    }
                    return trades;
                }
            } catch (SQLException e) {
                plugin.getIntegritySystem().handleStorageException(new RedisTradeStorageException(e, RedisTradeStorageException.ExceptionSource.UNARCHIVE_TRADE));
                return Collections.emptyMap();
            }
        });
    }

    @Override
    public CompletableFuture<Optional<NewTrade>> getArchivedTrade(UUID tradeUUID) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         SELECT * FROM archived WHERE trade_uuid = ?""")) {
                statement.setString(1, tradeUUID.toString());

                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        return Optional.of(tradeFromResultSet(result));
                    }
                    return Optional.empty();
                }
            } catch (SQLException e) {
                plugin.getIntegritySystem().handleStorageException(new RedisTradeStorageException(e, RedisTradeStorageException.ExceptionSource.UNARCHIVE_TRADE));
                return Optional.empty();
            }
        });
    }

    @Override
    public void backupTrade(NewTrade trade) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT OR REPLACE INTO `backup` (trade_uuid,server_id,serialized)
                     VALUES (?,?,?);
                     """)) {
            statement.setString(1, trade.getUuid().toString());
            statement.setInt(2, RedisTrade.getServerId());
            statement.setString(3, new String(trade.serialize(), StandardCharsets.ISO_8859_1));
            if (statement.executeUpdate() != 0) {
                RedisTrade.debug("Trade " + trade.getUuid() + " backed up");
            }
        } catch (Exception e) {
            plugin.getIntegritySystem().handleStorageException(new RedisTradeStorageException(e,
                    RedisTradeStorageException.ExceptionSource.BACKUP_TRADE, trade.getUuid()));
        }
    }

    @Override
    public void removeTradeBackup(UUID tradeUUID) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         DELETE FROM `backup` WHERE trade_uuid = ?;""")) {
                statement.setString(1,
                        tradeUUID.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getIntegritySystem().handleStorageException(new RedisTradeStorageException(e, RedisTradeStorageException.
                        ExceptionSource.BACKUP_TRADE, tradeUUID));
            }
        });
    }

    @Override
    public void updateStoragePlayerList(String playerName, UUID playerUUID) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         INSERT OR REPLACE INTO `player_list` (player_name,player_uuid)
                         VALUES (?,?
                         );""")) {
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
                    "INSERT OR REPLACE INTO `ignored_players` (ignorer,ignored) VALUES (?,?);" :
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
    public void rateTrade(NewTrade archivedTrade, Actor actor, int rating) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE archived SET " + (actor == Actor.CUSTOMER ? "customer_rating" : "trader_rating") +
                                 " = ? WHERE trade_uuid = ?;")) {
                statement.setInt(1, rating);
                statement.setString(2, archivedTrade.getUuid().toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getIntegritySystem().handleStorageException(new RedisTradeStorageException(e, RedisTradeStorageException.ExceptionSource.IGNORED_PLAYER));
            }
        });
    }

    @Override
    public CompletionStage<Map<Integer, NewTrade>> restoreTrades() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         SELECT * FROM `backup`;""")) {
                try (ResultSet result = statement.executeQuery()) {
                    final HashMap<Integer, NewTrade> trades = new HashMap<>();
                    while (result.next()) {
                        try {
                            trades.put(result.getInt("server_id"),
                                    NewTrade.deserialize(result.getString("serialized").getBytes(StandardCharsets.ISO_8859_1)));
                        } catch (Exception e) {
                            plugin.getIntegritySystem().handleStorageException(new RedisTradeStorageException(e, RedisTradeStorageException.ExceptionSource.SERIALIZATION));
                        }
                    }
                    return trades;
                }
            } catch (SQLException e) {
                plugin.getIntegritySystem().handleStorageException(new RedisTradeStorageException(e, RedisTradeStorageException.ExceptionSource.RESTORE_TRADE));
                return Collections.emptyMap();
            }
        });
    }

    @Override
    public CompletionStage<Map<String, UUID>> loadNameUUIDs() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         SELECT * FROM `player_list`;""")) {
                try (ResultSet result = statement.executeQuery()) {
                    final Map<String, UUID> nameUUIDs = new HashMap<>();
                    while (result.next()) {
                        nameUUIDs.put(result.getString("player_name"), UUID.fromString(result.getString("player_uuid")));
                    }
                    return nameUUIDs;
                }
            } catch (SQLException e) {
                plugin.getIntegritySystem().handleStorageException(new RedisTradeStorageException(e, RedisTradeStorageException.ExceptionSource.PLAYERLIST));
                return new HashMap<>();
            }
        });
    }

    @Override
    public CompletionStage<Set<String>> getIgnoredPlayers(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         SELECT * FROM `ignored_players` WHERE ignorer = ?;""")) {
                statement.setString(1, playerName);
                try (ResultSet result = statement.executeQuery()) {
                    final Set<String> ignoredPlayers = new HashSet<>();
                    while (result.next()) {
                        ignoredPlayers.add(result.getString("ignored"));
                    }
                    return ignoredPlayers;
                }
            } catch (SQLException e) {
                plugin.getIntegritySystem().handleStorageException(new RedisTradeStorageException(e, RedisTradeStorageException.ExceptionSource.IGNORED_PLAYER));
                return Collections.emptySet();
            }
        });
    }

    @Override
    public CompletionStage<TradeRating> getTradeRating(UUID tradeUUID) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         SELECT trader_rating, customer_rating FROM archived
                         WHERE trade_uuid = ?;""")) {
                statement.setString(1, tradeUUID.toString());
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        return new TradeRating(result.getInt(1), result.getInt(2));
                    }
                    return new TradeRating(0, 0);
                }
            } catch (SQLException e) {
                plugin.getIntegritySystem().handleStorageException(new RedisTradeStorageException(e, RedisTradeStorageException.ExceptionSource.IGNORED_PLAYER));
                return new TradeRating(0, 0);
            }
        });
    }

    @Override
    public CompletableFuture<MeanRating> getMeanRating(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         SELECT username,AVG(rating),COUNT(rating)
                         FROM (SELECT trader_rating as rating,trader_name as username FROM archived WHERE archived.trader_uuid = ?
                         UNION ALL
                         SELECT customer_rating as rating,customer_name as username FROM archived WHERE archived.customer_uuid = ?)
                         union_alias
                         WHERE rating > 0
                         GROUP BY username;""")) {
                statement.setString(1, playerUUID.toString());
                statement.setString(2, playerUUID.toString());
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        return new MeanRating(result.getString(1), result.getDouble(2), result.getInt(3));
                    }
                    return new MeanRating(null, 0D, 0);
                }
            } catch (SQLException e) {
                plugin.getIntegritySystem().handleStorageException(new RedisTradeStorageException(e, RedisTradeStorageException.ExceptionSource.IGNORED_PLAYER));
                return new MeanRating(null, 0D, 0);
            }
        });
    }

    private NewTrade tradeFromResultSet(ResultSet result) throws SQLException {
        Type typeOfPriceHashMap = new TypeToken<Map<String, Double>>() {
        }.getType();
        HashMap<String, Double> traderPrice = new HashMap<>(gson.fromJson(result.getString("trader_price"), typeOfPriceHashMap));
        OrderInfo traderOrder = new OrderInfo(VirtualInventory.deserialize(
                result.getString("trader_items").getBytes(StandardCharsets.ISO_8859_1)),
                Status.COMPLETED, result.getInt("trader_rating"), traderPrice);
        TradeSide traderSide = new TradeSide(UUID.fromString(result.getString("trader_uuid")),
                result.getString("trader_name"), traderOrder, false);

        HashMap<String, Double> customerPrice = new HashMap<>(gson.fromJson(result.getString("customer_price"), typeOfPriceHashMap));
        OrderInfo customerOrder = new OrderInfo(VirtualInventory.deserialize(
                result.getString("customer_items").getBytes(StandardCharsets.ISO_8859_1)),
                Status.COMPLETED, result.getInt("customer_rating"), customerPrice);
        TradeSide customerSide = new TradeSide(UUID.fromString(result.getString("customer_uuid")),
                result.getString("customer_name"), customerOrder, false);
        return new NewTrade(UUID.fromString(result.getString("trade_uuid")), traderSide, customerSide);
    }
}
