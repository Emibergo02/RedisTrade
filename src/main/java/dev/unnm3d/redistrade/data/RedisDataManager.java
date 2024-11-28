package dev.unnm3d.redistrade.data;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.core.OrderInfo;
import dev.unnm3d.redistrade.redistools.RedisAbstract;
import dev.unnm3d.redistrade.utils.Utils;
import io.lettuce.core.RedisClient;
import lombok.Getter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class RedisDataManager extends RedisAbstract {

    private final RedisTrade plugin;

    public RedisDataManager(RedisTrade plugin, RedisClient client, int poolSize) {
        super(client, poolSize);
        this.plugin = plugin;
        registerSub(DataKeys.FIELD_UPDATE_TRADE.toString(),
                DataKeys.PLAYERLIST.toString(),
                DataKeys.IGNORE_PLAYER_UPDATE.toString(),
                DataKeys.NAME_UUIDS.toString(),
                DataKeys.FULL_TRADE.toString(),
                DataKeys.TRADE_QUERY.toString()
        );
    }

    @Override
    public void receiveMessage(String channel, String message) {
        if (channel.equals(DataKeys.PLAYERLIST.toString())) {
            if (plugin.getPlayerListManager() != null)
                plugin.getPlayerListManager().updatePlayerList(Arrays.asList(message.split("§")));

        } else if (channel.equals(DataKeys.FIELD_UPDATE_TRADE.toString())) {
            int packetServerId = ByteBuffer.wrap(message.substring(0, 4).getBytes(StandardCharsets.ISO_8859_1)).getInt();
            if (packetServerId == RedisTrade.getServerId()) return;

            UUID tradeUUID = UUID.fromString(message.substring(4, 40));
            TradeUpdateType type = TradeUpdateType.valueOf(message.charAt(40));
            String value = message.substring(41);

            if (type == null) throw new IllegalStateException("Unexpected value: " + null);
            plugin.getTradeManager().getTrade(tradeUUID).ifPresent(trade -> {
                plugin.getTradeManager().setTradeServerOwner(packetServerId, tradeUUID);
                switch (type) {
                    case TRADER_MONEY -> trade.setPrice(Double.parseDouble(value), true);
                    case TRADER_ITEM -> {
                        String[] split = value.split("§;");
                        int slot = Integer.parseInt(split[0]);
                        trade.updateTraderItem(slot, Utils.deserialize(split[1])[0], false);
                        trade.retrievePhase(true,false);
                    }
                    case TRADER_STATUS -> trade.setStatus(OrderInfo.Status.fromByte(Byte.parseByte(value)), true);
                    case TARGET_MONEY -> trade.setPrice(Double.parseDouble(value), false);
                    case TARGET_ITEM -> {
                        String[] split = value.split("§;");
                        int slot = Integer.parseInt(split[0]);
                        trade.updateTargetItem(slot, Utils.deserialize(split[1])[0], false);
                        trade.retrievePhase(false,true);
                    }
                    case TARGET_STATUS -> trade.setStatus(OrderInfo.Status.fromByte(Byte.parseByte(value)), false);
                    default -> throw new IllegalStateException("Unexpected value: " + type);
                }
            });
        } else if (channel.equals(DataKeys.IGNORE_PLAYER_UPDATE.toString())) {
            String[] split = message.split("§");
            plugin.getTradeManager().ignoreUpdate(split[0], split[1], Boolean.parseBoolean(split[2]));
        } else if (channel.equals(DataKeys.NAME_UUIDS.toString())) {
            String[] split = message.split("§");
            plugin.getPlayerListManager().setPlayerNameUUID(split[0], UUID.fromString(split[1]));
        } else if (channel.equals(DataKeys.FULL_TRADE.toString())) {
            ByteBuffer bb = ByteBuffer.wrap(message.substring(0, 4).getBytes(StandardCharsets.ISO_8859_1));
            int packetServerId = bb.getInt();
            if (packetServerId == RedisTrade.getServerId()) return;

            final NewTrade trade = NewTrade.deserialize(this, message.substring(4).getBytes(StandardCharsets.ISO_8859_1));
            plugin.getTradeManager().initializeTrade(packetServerId, trade);
        } else if (channel.equals(DataKeys.TRADE_QUERY.toString())) {
            int packetServerId = Integer.parseInt(message);
            if (packetServerId == RedisTrade.getServerId()) return;

            plugin.getTradeManager().sendAllCurrentTrades();
        }
    }

    @Override
    public void updateCachePlayerList(String playerName, UUID playerUUID) {
        getConnectionAsync(connection -> connection.publish(DataKeys.NAME_UUIDS.toString(), playerName + "§" + playerUUID))
                .exceptionally(exception -> {
                    plugin.getLogger().warning("Error when publishing nameUUIDs");
                    return null;
                });
    }

    @Override
    public void publishPlayerList(List<String> playerList) {
        getConnectionAsync(connection -> connection.publish(DataKeys.PLAYERLIST.toString(),
                String.join("§", playerList)))
                .toCompletableFuture().orTimeout(1, TimeUnit.SECONDS)
                .exceptionally(exception -> {
                    plugin.getLogger().warning("Error when publishing player list");
                    return -1L;
                });
    }

    @Override
    public CompletionStage<Long> updateTrade(UUID tradeUUID, TradeUpdateType type, Object value) {
        plugin.getTradeManager().setTradeServerOwner(RedisTrade.getServerId(), tradeUUID);
        return getConnectionAsync(connection ->
                connection.publish(DataKeys.FIELD_UPDATE_TRADE.toString(),
                        new String(ByteBuffer.allocate(4).putInt(RedisTrade.getServerId()).array(), StandardCharsets.ISO_8859_1) +
                                tradeUUID.toString() + type + value))
                .exceptionallyAsync(exception -> {
                    plugin.getLogger().warning("Error when publishing trade update");
                    return -1L;
                });

    }

    @Override
    public CompletionStage<Long> sendFullTrade(NewTrade trade) {
        final ByteBuffer bb = ByteBuffer.allocate(4)
                .putInt(RedisTrade.getServerId());//4 for serverId, 8 for UUID = 12 bytes
        return getConnectionAsync(connection -> connection.publish(DataKeys.FULL_TRADE.toString(),
                new String(bb.array(), StandardCharsets.ISO_8859_1) +
                        new String(trade.serialize(), StandardCharsets.ISO_8859_1)));
    }

    @Override
    public void sendQuery() {
        getConnectionAsync(connection -> connection.publish(DataKeys.TRADE_QUERY.toString(),
                String.valueOf(RedisTrade.getServerId()))).whenComplete((aLong, throwable) -> {
            if (throwable != null) {
                plugin.getLogger().warning("Error when sending query");
            }
            if (Settings.instance().debug) {
                plugin.getLogger().info("Query sent to " + aLong + " servers");
            }
        });
    }

    @Getter
    public enum TradeUpdateType {
        TRADE_CREATE('S'),
        TRADER_MONEY('M'),
        TRADER_ITEM('I'),
        TRADER_STATUS('C'),
        TARGET_MONEY('m'),
        TARGET_ITEM('i'),
        TARGET_STATUS('c');

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
                case 'S' -> TRADE_CREATE;
                case 'M' -> TRADER_MONEY;
                case 'I' -> TRADER_ITEM;
                case 'C' -> TRADER_STATUS;
                case 'm' -> TARGET_MONEY;
                case 'i' -> TARGET_ITEM;
                case 'c' -> TARGET_STATUS;
                default -> null;
            };
        }
    }
}
