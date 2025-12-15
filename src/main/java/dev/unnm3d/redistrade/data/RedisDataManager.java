package dev.unnm3d.redistrade.data;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.core.TradeInvite;
import dev.unnm3d.redistrade.core.enums.StatusActor;
import dev.unnm3d.redistrade.core.enums.ViewerUpdate;
import dev.unnm3d.redistrade.redistools.RedisAbstract;
import dev.unnm3d.redistrade.utils.Utils;
import io.lettuce.core.RedisClient;

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

            final UUID tradeUUID = UUID.fromString(message.substring(4, 40));
            final ViewerUpdate viewerUpdate = ViewerUpdate.valueOf(message.charAt(40));
            final String value = message.substring(41);

            plugin.getTradeManager().getTrade(tradeUUID).ifPresent(trade -> {
                plugin.getTradeManager().setTradeServerOwner(tradeUUID, packetServerId);
                switch (viewerUpdate.getUpdateType()) {
                    case OPEN -> plugin.getTradeManager().remoteOpenTrade(tradeUUID, viewerUpdate.getActorSide());
                    case PRICE -> {
                        String[] split = value.split(":");
                        trade.setPrice(split[0], Double.parseDouble(split[1]), viewerUpdate.getActorSide());
                    }
                    case ITEM -> {
                        int slot = value.charAt(0);
                        trade.updateItem(slot, Utils.deserialize(value.substring(1).getBytes(StandardCharsets.ISO_8859_1))[0], viewerUpdate.getActorSide(), false);
                        trade.retrievedPhase(viewerUpdate.getActorSide(), viewerUpdate.getActorSide().opposite());
                    }
                    case STATUS -> trade.setStatus(StatusActor.fromChar(value.charAt(0)), viewerUpdate.getActorSide());
                    case CLOSE -> plugin.getTradeManager().finishTrade(tradeUUID, viewerUpdate.getActorSide());
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
            try {
                final NewTrade trade = NewTrade.deserialize(message.substring(4).getBytes(StandardCharsets.ISO_8859_1));
                plugin.getTradeManager().initializeTrade(packetServerId, trade);
            } catch (Exception e) {
                throw new IllegalStateException("Error when deserializing trade: the received trades is probably from different RedisTrade or Minecraft versions", e);
            }
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
    public CompletionStage<Long> updateTrade(UUID tradeUUID, ViewerUpdate type, Object value) {
        plugin.getTradeManager().setTradeServerOwner(tradeUUID, RedisTrade.getServerId());
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
    public void sendInvite(long timestamp,TradeInvite invite) {
        getConnectionAsync(connection -> connection.publish(DataKeys.INVITE_UPDATE.toString(),
          invite.getInvitedPlayerName() + "§" + invite.getInviterPlayerName() + "§" + invite.isIgnore()))
          .exceptionally(exception -> {
              plugin.getLogger().warning("Error when publishing ignore player update");
              return null;
          });
    }

    @Override
    public void sendQuery() {
        getConnectionAsync(connection -> connection.publish(DataKeys.TRADE_QUERY.toString(),
          String.valueOf(RedisTrade.getServerId()))).whenComplete((aLong, throwable) -> {
            if (throwable != null) {
                plugin.getLogger().warning("Error when sending query");
            }
            RedisTrade.debug("Query sent to " + aLong + " servers");
        });
    }


}
