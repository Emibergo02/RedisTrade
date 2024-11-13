package dev.unnm3d.redistrade.data;

import dev.unnm3d.redistrade.objects.NewTrade;

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

    boolean archiveTrade(NewTrade trade);

    CompletableFuture<Map<Long, NewTrade>> getArchivedTrades(UUID playerUUID, Date startTimestamp, Date endTimestamp);
    void connect();
    void close();
}
