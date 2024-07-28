package dev.unnm3d.redistrade.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.objects.Order;
import dev.unnm3d.redistrade.objects.Trader;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class SQLiteDatabase extends DataCache implements Database {
    protected final RedisTrade plugin;
    @Getter
    @Setter
    protected boolean connected = false;
    protected HikariDataSource dataSource;


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
            try (Statement statement = getConnection().createStatement()) {
                for (String tableCreationStatement : databaseSchema) {
                    statement.execute(tableCreationStatement);
                }
            } catch (SQLException e) {
                destroy();
                throw new IllegalStateException("Failed to create database tables.", e);
            }
        } catch (IOException e) {
            destroy();
            throw new IllegalStateException("Failed to create database tables.", e);
        }
        setConnected(true);
        load();
    }

    @Override
    public void destroy() {
        try {
            if (getConnection() != null) {
                if (!getConnection().isClosed()) {
                    getConnection().close();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        setConnected(false);
    }

    @Override
    public CompletableFuture<Optional<Trader>> getTrader(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM trader WHERE uuid = ?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        return Optional.of(new Trader(UUID.fromString(result.getString("uuid")),
                                        result.getString("name"),null));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "An exception occurred retrieving a trader from the database", e);
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<Void> createTrader(Trader trader) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("INSERT INTO trader (uuid, name, receipt_serialized_item) VALUES (?, ?, ?)")) {
                statement.setString(1, trader.getUuid().toString());
                statement.setString(2, trader.getName());
                statement.setString(3, serialize(trader.getReceipt()));
                statement.executeUpdate();
                updateTraderCache(trader);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "An exception occurred creating a trader in the database", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> updateTrader(Trader trader) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("UPDATE trader SET name = ?, receipt_serialized_item = ? WHERE uuid = ?")) {
                statement.setString(1, trader.getName());
                statement.setString(2, serialize(trader.getReceipt()));
                statement.setString(3, trader.getUuid().toString());
                if(statement.executeUpdate() == 0){
                    throw new SQLException("Failed to update trader with uuid " + trader.getUuid());
                }
                updateTraderCache(trader);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "An exception occurred updating a trader in the database", e);
            }
        });
    }

    @Override
    public CompletableFuture<Optional<Order>> createOrder(Player seller, String buyerName, List<ItemStack> items, double offer) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         INSERT INTO `order` (seller, buyer, serialized_items, offer)
                         SELECT
                             ?,
                             t.uuid,
                             ?,
                             ?
                         FROM trader AS t
                         WHERE t.name = ?;
                         """)) {
                statement.setString(1, seller.toString());
                statement.setString(2, serialize(items.toArray(new ItemStack[0])));
                statement.setDouble(3, offer);
                statement.setString(4, buyerName);
                statement.executeUpdate();
                try (ResultSet result = statement.getGeneratedKeys()) {
                    if (result.next()) {
                        return Optional.of(updateOrderCache(new Order(result.getInt("last_insert_rowid()"),
                                System.currentTimeMillis(),
                                Trader.fromPlayer(seller),
                                traderCache.get(buyerName),
                                items,
                                false,
                                false,
                                (short) 0,
                                offer
                        )));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "An exception occurred creating an order in the database", e);
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<Void> updateOrder(Order order) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                    UPDATE `order`
                    SET buyer = ?,
                    serialized_items = ?,
                    completed = ?,
                    collected = ?,
                    review = ?
                    WHERE id = ?
                    """)) {
                statement.setString(1, order.getBuyer().getName());
                statement.setString(2, serialize(order.getItems().toArray(new ItemStack[0])));
                statement.setBoolean(3, order.isCompleted());
                statement.setBoolean(4, order.isCollected());
                statement.setShort(5, order.getReview());
                statement.setInt(6, order.getId());


                if(statement.executeUpdate() == 0){
                    throw new SQLException("Failed to update order with id " + order.getId());
                }
                updateOrder(order);

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "An exception occurred updating an order in the database", e);
            }
        });
    }


    @Override
    public CompletableFuture<List<Order>> getOrders(Trader trader){
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM `order` WHERE seller = ? OR buyer = ?")) {
                statement.setString(1, trader.getUuid().toString());
                statement.setString(2, trader.getName());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        new Order(result.getInt("id"),
                                result.getTimestamp("timestamp").getTime(),
                                trader,
                                traderCache.get(result.getString("buyer")),
                                List.of(deserialize(result.getString("serialized_items"))),
                                result.getBoolean("completed"),
                                result.getBoolean("collected"),
                                result.getShort("status"),
                                result.getDouble("offer")
                        );
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "An exception occurred retrieving orders from the database", e);
            }
            return List.copyOf(orderCache.values());
        });
    }
}
