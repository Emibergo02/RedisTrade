package dev.unnm3d.redistrade.data;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.Utils;
import dev.unnm3d.redistrade.guis.OrderInfo;
import dev.unnm3d.redistrade.objects.NewTrade;
import dev.unnm3d.redistrade.redistools.RedisAbstract;
import io.lettuce.core.RedisClient;
import lombok.Getter;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class RedisDataManager extends RedisAbstract {
    private static final int serverId = new Random().nextInt();
    private final RedisTrade plugin;

    public RedisDataManager(RedisTrade plugin, RedisClient client, int poolSize) {
        super(client, poolSize);
        this.plugin = plugin;
        registerSub(DataKeys.FIELD_UPDATE_TRADE.toString(),
                DataKeys.PLAYERLIST.toString(),
                DataKeys.IGNORE_PLAYER_UPDATE.toString(),
                DataKeys.NAME_UUIDS.toString()
        );
    }

    @Override
    public void receiveMessage(String channel, String message) {
        if (channel.equals(DataKeys.PLAYERLIST.toString())) {
            if (plugin.getPlayerListManager() != null)
                plugin.getPlayerListManager().updatePlayerList(Arrays.asList(message.split("§")));

        } else if (channel.equals(DataKeys.FIELD_UPDATE_TRADE.toString())) {
            int packetServerId = ByteBuffer.wrap(message.substring(0, 4).getBytes(StandardCharsets.ISO_8859_1)).getInt();
            if (packetServerId == serverId) return;
            UUID tradeUUID = UUID.fromString(message.substring(4, 40));
            TradeUpdateType type = TradeUpdateType.valueOf(message.charAt(40));
            String value = message.substring(41);
            
            if (type == TradeUpdateType.TRADE_START) {
                plugin.getTradeManager().tradeUpdate(
                        NewTrade.deserializeEmpty(this, value.getBytes(StandardCharsets.ISO_8859_1)));
                return;
            }
            plugin.getTradeManager().getTrade(tradeUUID).ifPresent(trade -> {
                switch (type) {
                    case MONEY_TRADER -> trade.setTraderPrice(Double.parseDouble(value));
                    case ITEM_TRADER -> {
                        String[] split = value.split("§;");
                        int slot = Integer.parseInt(split[0]);
                        trade.updateTraderItem(slot, Utils.deserialize(split[1])[0], false);
                    }
                    case CONFIRM_TRADER -> trade.setTraderStatus(OrderInfo.Status.fromByte(Byte.parseByte(value)));
                    case MONEY_TARGET -> trade.setTargetPrice(Double.parseDouble(value));
                    case ITEM_TARGET -> {
                        String[] split = value.split("§;");
                        int slot = Integer.parseInt(split[0]);
                        trade.updateTargetItem(slot, Utils.deserialize(split[1])[0], false);
                    }
                    case CONFIRM_TARGET -> trade.setTargetStatus(OrderInfo.Status.fromByte(Byte.parseByte(value)));
                    case null -> throw new IllegalStateException("Unexpected value: " + type);
                    default -> throw new IllegalStateException("Unexpected value: " + type);
                }
            });
        } else if (channel.equals(DataKeys.IGNORE_PLAYER_UPDATE.toString())) {
            String[] split = message.split("§");
            plugin.getTradeManager().ignoreUpdate(split[0], split[1], Boolean.parseBoolean(split[2]));
        } else if (channel.equals(DataKeys.NAME_UUIDS.toString())) {
            String[] split = message.split("§");
            plugin.getPlayerListManager().setPlayerNameUUID(split[0], UUID.fromString(split[1]));
        }
    }

    public CompletionStage<Map<String, UUID>> loadNameUUIDs() {
        return getConnectionAsync(connection -> connection.hgetall(DataKeys.NAME_UUIDS.toString()))
                .thenApply(map -> {
                    Map<String, UUID> nameUUIDs = new HashMap<>();
                    for (Map.Entry<String, String> stringStringEntry : map.entrySet()) {
                        nameUUIDs.put(stringStringEntry.getKey(), UUID.fromString(stringStringEntry.getValue()));
                    }
                    return nameUUIDs;
                });
    }

    public void publishPlayerList(List<String> playerList) {
        getConnectionAsync(connection -> connection.publish(DataKeys.PLAYERLIST.toString(),
                        String.join("§", playerList))
                .toCompletableFuture().orTimeout(1, TimeUnit.SECONDS)
                .exceptionally(exception -> {
                    exception.printStackTrace();
                    plugin.getLogger().warning("Error when publishing player list");
                    return 0L;
                }));
    }

    public void updateOfflinePlayerList(String playerName, UUID playerUUID) {
        getConnectionPipeline(connection -> {
            connection.hset(DataKeys.NAME_UUIDS.toString(), playerName, playerUUID.toString())
                    .exceptionally(exception -> {
                        exception.printStackTrace();
                        plugin.getLogger().warning("Error when publishing nameUUIDs");
                        return null;
                    });
            return connection.publish(DataKeys.NAME_UUIDS.toString(), playerName + "§" + playerUUID)
                    .exceptionally(exception -> {
                        exception.printStackTrace();
                        plugin.getLogger().warning("Error when publishing nameUUIDs");
                        return null;
                    });
        });
    }

//    public void openRemoteWindow(@NotNull String name, boolean traderView, UUID tradeUUID) {
//        getConnectionAsync(connection ->
//                connection.publish(DataKeys.OPEN_WINDOW.toString(), name + "§" + traderView + "§" + tradeUUID)
//                        .toCompletableFuture().orTimeout(1, TimeUnit.SECONDS)
//                        .exceptionally(exception -> {
//                            exception.printStackTrace();
//                            plugin.getLogger().warning("Error when publishing trade update");
//                            return 0L;
//                        })
//        );
//    }

    public void restoreTrades() {
        getConnectionAsync(connection -> connection.hgetall(DataKeys.TRADES.toString())
                .thenAccept(map -> {
                    for (Map.Entry<String, String> stringStringEntry : map.entrySet()) {
                        try {
                            NewTrade trade = NewTrade.deserialize(this, decompress(stringStringEntry.getValue().getBytes(StandardCharsets.ISO_8859_1)));
                            plugin.getTradeManager().tradeUpdate(trade);
                        } catch (DataFormatException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }));
    }

    public void backupTrade(NewTrade trade, boolean sendToArchive) {
        getConnectionAsync(connection -> {
            if (sendToArchive) {
                return connection.zadd(DataKeys.TRADE_ARCHIVE.toString(),
                                System.currentTimeMillis(),
                                new String(compress(trade.serialize()), StandardCharsets.ISO_8859_1))
                        //If the content added to zset is 1, it's new
                        .thenApply(longres -> longres == 1);
            } else return connection.hset(
                    DataKeys.TRADES.toString(),
                    trade.getUuid().toString(), new String(compress(trade.serialize()), StandardCharsets.ISO_8859_1));
        });
    }

    public void removeTrade(UUID tradeUUID) {
        getConnectionAsync(connection -> connection.hdel(DataKeys.TRADES.toString(),
                tradeUUID.toString()))
                .exceptionally(exception -> {
                    exception.printStackTrace();
                    plugin.getLogger().warning("Error when publishing trade update");
                    return 0L;
                });
    }

    public void updateTrade(UUID tradeUUID, TradeUpdateType type, Object value) {
        getConnectionAsync(connection ->
                connection.publish(DataKeys.FIELD_UPDATE_TRADE.toString(),
                                new String(ByteBuffer.allocate(4).putInt(serverId).array(), StandardCharsets.ISO_8859_1) +
                                        tradeUUID.toString() + type + value)
                        .toCompletableFuture().orTimeout(1, TimeUnit.SECONDS)
                        .exceptionally(exception -> {
                            exception.printStackTrace();
                            plugin.getLogger().warning("Error when publishing trade update");
                            return 0L;
                        })
        );
    }

    public void createTrade(NewTrade trade) {
        getConnectionAsync(connection ->
                connection.publish(DataKeys.FIELD_UPDATE_TRADE.toString(),
                                new String(ByteBuffer.allocate(4).putInt(serverId).array(), StandardCharsets.ISO_8859_1) +
                                        trade.getUuid().toString() + TradeUpdateType.TRADE_START +
                                        new String(trade.serializeEmpty(), StandardCharsets.ISO_8859_1))
                        .toCompletableFuture().orTimeout(1, TimeUnit.SECONDS)
                        .exceptionally(exception -> {
                            exception.printStackTrace();
                            plugin.getLogger().warning("Error when publishing trade update");
                            return 0L;
                        })
        );
    }

    public void ignorePlayer(String playerName, String targetName, boolean ignore) {
        getConnectionPipeline(connection -> {
            if (ignore) {
                connection.sadd(DataKeys.IGNORE_PLAYER_PREFIX + playerName, targetName);
            } else {
                connection.srem(DataKeys.IGNORE_PLAYER_PREFIX + playerName, targetName);
            }
            return connection.publish(DataKeys.IGNORE_PLAYER_UPDATE.toString(), playerName + "§" + targetName + "§" + ignore);
        });
    }

    public CompletionStage<Set<String>> getIgnoredPlayers(String playerName) {
        return getConnectionAsync(connection -> connection.smembers(DataKeys.IGNORE_PLAYER_PREFIX + playerName));
    }

    public static byte[] compress(byte[] input) {
        Deflater deflater = new Deflater();
        deflater.setInput(input);
        deflater.finish();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];

        while (!deflater.finished()) {
            int compressedSize = deflater.deflate(buffer);
            outputStream.write(buffer, 0, compressedSize);
        }
        System.out.println("Compressed " + input.length + " to " + outputStream.size());
        return outputStream.toByteArray();
    }

    public static byte[] decompress(byte[] input) throws DataFormatException {
        Inflater inflater = new Inflater();
        inflater.setInput(input);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];

        while (!inflater.finished()) {
            int decompressedSize = inflater.inflate(buffer);
            outputStream.write(buffer, 0, decompressedSize);
        }

        return outputStream.toByteArray();
    }

    @Getter
    public enum TradeUpdateType {
        TRADE_START('S'),
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
                case 'S' -> TRADE_START;
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
