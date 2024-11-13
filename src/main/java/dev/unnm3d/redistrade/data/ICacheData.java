package dev.unnm3d.redistrade.data;

import dev.unnm3d.redistrade.objects.NewTrade;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface ICacheData {

    void publishPlayerList(List<String> playerList);

    CompletionStage<Map<String, UUID>> loadNameUUIDs();
    void updateCachePlayerList(String playerName, UUID playerUUID);

    void updateTrade(UUID tradeUUID, RedisDataManager.TradeUpdateType type, Object value);

    void createTrade(NewTrade trade);

    void close();
}
