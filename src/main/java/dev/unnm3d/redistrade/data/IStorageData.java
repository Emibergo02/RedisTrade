package dev.unnm3d.redistrade.data;

import dev.unnm3d.redistrade.core.ArchivedTrade;
import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.core.enums.Actor;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface IStorageData {
    void backupTrade(@NotNull NewTrade trade);

    void removeTradeBackup(@NotNull UUID tradeUUID);

    void updateStoragePlayerList(@NotNull String playerName, @NotNull UUID playerUUID);

    void ignorePlayer(@NotNull String playerName, @NotNull String targetName, boolean ignore);

    void rateTrade(@NotNull NewTrade archivedTrade, @NotNull Actor actor, int rating);

    CompletionStage<Map<Integer, NewTrade>> restoreTrades();

    CompletionStage<Map<String, UUID>> loadNameUUIDs();

    CompletionStage<Set<String>> getIgnoredPlayers(@NotNull String playerName);

    CompletionStage<SQLiteDatabase.TradeRating> getTradeRating(@NotNull UUID tradeUUID);

    CompletableFuture<SQLiteDatabase.MeanRating> getMeanRating(@NotNull UUID playerUUID);

    /**
     * Archive a trade, not available for REDIS storage type
     *
     * @param trade the trade to archive
     * @return true if the trade was archived successfully
     */
    default CompletableFuture<Boolean> archiveTrade(@NotNull NewTrade trade) {
        return CompletableFuture.completedFuture(Boolean.FALSE);
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
    default CompletableFuture<List<ArchivedTrade>> getArchivedTrades(@NotNull UUID playerUUID, @NotNull LocalDateTime startTimestamp, @NotNull LocalDateTime endTimestamp) {
        return CompletableFuture.completedFuture(new ArrayList<>());
    }

    default CompletableFuture<Optional<ArchivedTrade>> getArchivedTrade(@NotNull UUID tradeUUID) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    void close();

    record TradeRating(int traderRating, int customerRating) {
    }

    record MeanRating(String playerName, double mean, int countedTrades) {
    }
}
