package dev.unnm3d.redistrade.data;

import dev.unnm3d.redistrade.objects.NewTrade;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface IStorageData {
    void backupTrade(NewTrade trade);

    void removeTradeBackup(UUID tradeUUID);

    void updateStoragePlayerList(String playerName, UUID playerUUID);

    void ignorePlayer(String playerName, String targetName, boolean ignore);

    CompletionStage<List<NewTrade>> restoreTrades();

    CompletionStage<Map<String, UUID>> loadNameUUIDs();

    CompletionStage<Set<String>> getIgnoredPlayers(String playerName);

    /**
     * Archive a trade, not available for REDIS storage type
     *
     * @param trade the trade to archive
     * @return true if the trade was archived successfully
     */
    default boolean archiveTrade(NewTrade trade) {
        return true;
    }

    /**
     * Get the archived trades for a player between two timestamps
     * Not available for REDIS storage type
     *
     * @param playerUUID     the player to get the trades for
     * @param startTimestamp the start timestamp
     * @param endTimestamp   the end timestamp
     * @return a map of timestamps and trades
     */
    default CompletableFuture<Map<Long, NewTrade>> getArchivedTrades(UUID playerUUID, LocalDateTime startTimestamp, LocalDateTime endTimestamp) {
        return CompletableFuture.completedFuture(new HashMap<>());
    }

    void close();
}
