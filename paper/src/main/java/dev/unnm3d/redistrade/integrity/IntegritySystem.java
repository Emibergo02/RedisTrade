package dev.unnm3d.redistrade.integrity;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.Settings;
import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionStateListener;
import lombok.Getter;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


public class IntegritySystem {
    private final RedisTrade plugin;
    @Getter
    private boolean faulted;

    public IntegritySystem(RedisTrade plugin, RedisClient redisClient) {
        this.plugin = plugin;
        this.faulted = false;
        if (redisClient != null)
            redisClient.addListener(new RedisConnectionStateListener() {
                long lastFaulted = 0;

                @Override
                public void onRedisConnected(RedisChannelHandler<?, ?> connection, SocketAddress socketAddress) {
                    CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
                        if (faulted && System.currentTimeMillis() - lastFaulted > 5000) {
                            plugin.getLogger().warning("Redis connection restored");
                            faulted = false;
                        }
                    });
                }

                @Override
                public void onRedisDisconnected(RedisChannelHandler<?, ?> connection) {
                    faulted = true;
                    lastFaulted = System.currentTimeMillis();
                }
            });
    }

    public void handleStorageException(RedisTradeStorageException exception) {
        if (Settings.instance().debug) {
            exception.printStackTrace();
        }
        switch (exception.getSource()) {
            case BACKUP_TRADE:
                plugin.getLogger().warning("Error in storage system, trying to backup trade " + exception.getTradeUUID());
                break;
            case RESTORE_TRADE:
                plugin.getLogger().warning("Error in storage system, trying to restore trades");
                break;
            case ARCHIVE_TRADE:
                plugin.getLogger().warning("Error in storage system, trying to archive trade " + exception.getTradeUUID());
                break;
            case UNARCHIVE_TRADE:
                plugin.getLogger().warning("Error in storage system, trying to browse trades ");
                break;
            case IGNORED_PLAYER:
                plugin.getLogger().warning("Error in storage system, trying to ignore player");
                break;
            case PLAYERLIST:
                plugin.getLogger().warning("Error in storage system, trying to update player list");
                break;
            case SERIALIZATION:
                plugin.getLogger().warning("Error in deserialization or serialization of trade " + exception.getTradeUUID());
                plugin.getLogger().warning("The data is probably from a different version of Minecraft or RedisTrade");
                break;
        }
    }
}
