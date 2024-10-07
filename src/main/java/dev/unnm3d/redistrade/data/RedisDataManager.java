package dev.unnm3d.redistrade.data;

import com.google.gson.Gson;
import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.Utils;
import dev.unnm3d.redistrade.objects.NewTrade;
import dev.unnm3d.redistrade.redistools.RedisAbstract;
import io.lettuce.core.RedisClient;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RedisDataManager extends RedisAbstract {
    private static final Gson gson = new Gson();
    private static final UUID serverUUID = UUID.randomUUID();
    private final RedisTrade plugin;

    public RedisDataManager(RedisTrade plugin, RedisClient client, int poolSize) {
        super(client, poolSize);
        this.plugin = plugin;
        registerSub(DataKeys.UPDATE_TRADE.toString(),
                DataKeys.UPDATE_CONFIRM.toString(),
                DataKeys.NEW_UPDATE_TRADE.toString(),
                DataKeys.UPDATE_MONEY.toString(),
                DataKeys.UPDATE_ITEM.toString(),
                DataKeys.PLAYERLIST.toString(),
                DataKeys.OPEN_WINDOW.toString()
        );
    }

    @Override
    public void receiveMessage(String channel, String message) {
        System.out.println("Received message: " + channel + " " + message);
        if (channel.equals(DataKeys.UPDATE_TRADE.toString())) {
            NewTrade trade = NewTrade.deserialize(this, message.getBytes(StandardCharsets.ISO_8859_1));
            plugin.getTradeManager().tradeUpdate(trade);

        } else if (channel.equals(DataKeys.UPDATE_CONFIRM.toString())) {
            String[] split = message.split(":");
            UUID tradeUUID = UUID.fromString(split[0]);
            boolean isTrader = Boolean.parseBoolean(split[1]);
            boolean confirm = Boolean.parseBoolean(split[2]);
            plugin.getTradeManager().getTrade(tradeUUID).ifPresent(trade -> {
                if (isTrader) {
                    trade.setTraderConfirm(confirm);
                } else {
                    trade.setTargetConfirm(confirm);
                }
            });

        } else if (channel.equals(DataKeys.UPDATE_MONEY.toString())) {
            String[] split = message.split(":");
            UUID tradeUUID = UUID.fromString(split[0]);
            boolean isTrader = Boolean.parseBoolean(split[1]);
            double money = Double.parseDouble(split[2]);
            plugin.getTradeManager().getTrade(tradeUUID).ifPresent(trade -> {
                if (isTrader) {
                    trade.setTraderPrice(money);
                } else {
                    trade.setTargetPrice(money);
                }
            });
        } else if (channel.equals(DataKeys.UPDATE_ITEM.toString())) {
            String[] split = message.split("§;");
            UUID tradeUUID = UUID.fromString(split[0]);
            boolean isTrader = Boolean.parseBoolean(split[1]);
            int slot = Integer.parseInt(split[2]);
            plugin.getTradeManager().getTrade(tradeUUID).ifPresent(trade -> {
                final ItemStack item = Utils.deserialize(split[3])[0];
                if (isTrader) {
                    trade.updateTraderItem(slot, item);
                } else {
                    trade.updateTargetItem(slot, item);
                }
            });

        } else if (channel.equals(DataKeys.PLAYERLIST.toString())) {
            plugin.getLogger().info("Received player list");
            if (plugin.getPlayerListManager() != null)
                plugin.getPlayerListManager().updatePlayerList(Arrays.asList(message.split("§")));

        } else if (channel.equals(DataKeys.OPEN_WINDOW.toString())) {
            String[] split = message.split("§");
            String name = split[0];
            boolean traderView = Boolean.parseBoolean(split[1]);
            UUID tradeUUID = UUID.fromString(split[2]);
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getTradeManager().getTrade(tradeUUID)
                        .ifPresentOrElse(trade -> trade.openWindow(name, traderView),
                                () -> plugin.getLogger().warning("DIOCANE Trade not found"));
            });
        } else if (channel.equals(DataKeys.NEW_UPDATE_TRADE.toString())) {
            UUID remoteServerUUID = UUID.fromString(message.substring(0, 36));
            if (remoteServerUUID.equals(serverUUID)) return;
            UUID tradeUUID = UUID.fromString(message.substring(36, 72));
            TradeUpdateType type = TradeUpdateType.valueOf(message.charAt(72));
            String value = message.substring(73);

            plugin.getTradeManager().getTrade(tradeUUID).ifPresent(trade -> {
                switch (type) {
                    case MONEY_TRADER -> trade.setTraderPrice(Double.parseDouble(value));
                    case ITEM_TRADER -> {
                        String[] split = value.split("§;");
                        int slot = Integer.parseInt(split[0]);
                        trade.updateTraderItem(slot, Utils.deserialize(split[1])[0]);
                    }
                    case CONFIRM_TRADER -> trade.setTraderConfirm(Boolean.parseBoolean(value));
                    case MONEY_TARGET -> trade.setTargetPrice(Double.parseDouble(value));
                    case ITEM_TARGET -> {
                        String[] split = value.split("§;");
                        int slot = Integer.parseInt(split[0]);
                        trade.updateTargetItem(slot, Utils.deserialize(split[1])[0]);
                    }
                    case CONFIRM_TARGET -> trade.setTargetConfirm(Boolean.parseBoolean(value));
                    case null -> {
                    }
                }
            });
        }
    }

    public void publishPlayerList(@NotNull List<String> playerNames) {
        getConnectionAsync(connection ->
                connection.publish(DataKeys.PLAYERLIST.toString(),
                                String.join("§", playerNames))
                        .toCompletableFuture().orTimeout(1, TimeUnit.SECONDS)
                        .exceptionally(exception -> {
                            exception.printStackTrace();
                            plugin.getLogger().warning("Error when publishing player list");
                            return 0L;
                        })
        );
    }

    public void openRemoteWindow(@NotNull String name, boolean traderView, UUID tradeUUID) {
        getConnectionAsync(connection ->
                connection.publish(DataKeys.OPEN_WINDOW.toString(), name + "§" + traderView + "§" + tradeUUID)
                        .toCompletableFuture().orTimeout(1, TimeUnit.SECONDS)
                        .exceptionally(exception -> {
                            exception.printStackTrace();
                            plugin.getLogger().warning("Error when publishing trade update");
                            return 0L;
                        })
        );
    }

    public void updateTrade(NewTrade trade) {
        getConnectionAsync(connection ->
                connection.publish(DataKeys.UPDATE_TRADE.toString(), new String(trade.serialize(), StandardCharsets.ISO_8859_1))
                        .toCompletableFuture().orTimeout(1, TimeUnit.SECONDS)
                        .exceptionally(exception -> {
                            exception.printStackTrace();
                            plugin.getLogger().warning("Error when publishing trade update");
                            return 0L;
                        })
        );
    }

    public void updateTrade(UUID tradeUUID, TradeUpdateType type, Object value) {
        getConnectionAsync(connection ->
                connection.publish(DataKeys.NEW_UPDATE_TRADE.toString(), serverUUID + tradeUUID.toString() + type + value)
                        .toCompletableFuture().orTimeout(1, TimeUnit.SECONDS)
                        .exceptionally(exception -> {
                            exception.printStackTrace();
                            plugin.getLogger().warning("Error when publishing trade update");
                            return 0L;
                        })
        );
    }

    @Getter
    public enum TradeUpdateType {
        MONEY_TRADER('M'),
        ITEM_TRADER('I'),
        CONFIRM_TRADER('C'),
        MONEY_TARGET('m'),
        ITEM_TARGET('i'),
        CONFIRM_TARGET('c');

        private final char code;

        TradeUpdateType(char code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return String.valueOf(code);
        }

        public static TradeUpdateType valueOf(char code) {
            return switch (code) {
                case 'M' -> MONEY_TRADER;
                case 'I' -> ITEM_TRADER;
                case 'C' -> CONFIRM_TRADER;
                case 'm' -> MONEY_TARGET;
                case 'i' -> ITEM_TARGET;
                case 'c' -> CONFIRM_TARGET;
                default -> null;
            };
        }
    }
}
