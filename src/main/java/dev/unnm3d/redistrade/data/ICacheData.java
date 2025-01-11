package dev.unnm3d.redistrade.data;

import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.core.enums.ViewerUpdate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface ICacheData {

    void publishPlayerList(List<String> playerList);

    void updateCachePlayerList(String playerName, UUID playerUUID);

    /**
     * Update a trade in the cache
     *
     * @param tradeUUID the trade to update
     * @param type      the type of update (price, item, status)
     * @param value     the value to update
     * @return the number of subscribers that received the message
     */
    CompletionStage<Long> updateTrade(UUID tradeUUID, ViewerUpdate type, Object value);

    /**
     * Broadcast a full trade to all servers in the network
     *
     * @param trade the trade to broadcast
     * @return the number of subscribers that received the message
     */
    CompletionStage<Long> sendFullTrade(NewTrade trade);

    /**
     * Send a query to all servers
     * The servers will respond by sending all their owned trades via sendFullTrade
     */
    void sendQuery();

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
            public CompletionStage<Long> updateTrade(UUID tradeUUID, ViewerUpdate type, Object value) {
                return CompletableFuture.completedFuture(0L);
            }

            @Override
            public CompletionStage<Long> sendFullTrade(NewTrade trade) {
                return CompletableFuture.completedFuture(0L);
            }

            @Override
            public void sendQuery() {
            }

            @Override
            public void close() {
            }
        };
    }
}
