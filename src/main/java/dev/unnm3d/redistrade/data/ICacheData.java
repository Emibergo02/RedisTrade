package dev.unnm3d.redistrade.data;

import dev.unnm3d.redistrade.core.NewTrade;

import java.util.List;
import java.util.UUID;

public interface ICacheData {

    void publishPlayerList(List<String> playerList);

    void updateCachePlayerList(String playerName, UUID playerUUID);

    void updateTrade(UUID tradeUUID, RedisDataManager.TradeUpdateType type, Object value);

    void createTrade(NewTrade trade);

    void close();

    static ICacheData createEmpty() {
        return new ICacheData() {
            @Override
            public void publishPlayerList(List<String> playerList) {
            }

            @Override
            public void updateCachePlayerList(String playerName, UUID playerUUID) {
            }

            @Override
            public void updateTrade(UUID tradeUUID, RedisDataManager.TradeUpdateType type, Object value) {
            }

            @Override
            public void createTrade(NewTrade trade) {
            }

            @Override
            public void close() {
            }
        };
    }
}
