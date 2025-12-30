package dev.unnm3d.redistrade.api;

import dev.unnm3d.redistrade.api.enums.Actor;
import dev.unnm3d.redistrade.api.invites.IInviteManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface ITradeManager {

    IInviteManager getInviteManager();

    CompletionStage<Optional<INewTrade>> startTrade(final @NonNull Player traderPlayer, final @NotNull UUID targetUUID, @Nullable String targetName);

    void startAcceptInviteAndOpen(Player player, UUID targetUUID, @NonNull String targetName, boolean force);

    /**
     * Instantly opens the trade window for the player
     *
     * @param trade  The trade to open
     * @param player The player to open the window for
     * @param force  Whether to force open the window (bypass restrictions)
     */
    void openWindow(INewTrade trade, Player player, boolean force);

    /**
     * Send all current trades to the other servers
     * This is called when a new server joins the network (aka a query is received)
     */
    void sendAllCurrentTrades();

    void setTradeServerOwner(UUID tradeUUID, int serverId);

    boolean isOwner(UUID tradeUUID);

    List<INewTrade> getPlayerActiveTrades(@NotNull UUID playerUUID);

    void openArchivedTrades(Player player, UUID targetUUID, LocalDateTime start, LocalDateTime end);

    void openActiveTrades(Player player);

    /**
     * Finish the trade and disconnect the player name from the trade UUID.
     * If both trader and target are disconnected, remove the trade from tradeguis
     * Show the ReviewGUI button
     *
     * @param tradeUUID The trade to be close
     * @param actorSide The actor side that is closing the trade
     */
    void finishTrade(UUID tradeUUID, Actor actorSide);

    /**
     * Give back the items to the player and try to close the trade
     * It is closed only if the sides are empty
     *
     * @param player    The player to give back the items
     * @param tradeUUID The trade to close
     */
    void collectItemsAndClose(Player player, UUID tradeUUID);

    Optional<INewTrade> getLatestTrade(UUID playerId);

    void ignorePlayer(String playerName, String targetName, boolean ignore);

    boolean isIgnoring(String playerName, String targetName);

    Set<String> getIgnoredPlayers(String playerName);

    List<INewTrade> getAllTrades();

    Optional<INewTrade> getTrade(UUID tradeUUID);

    void removeTrade(INewTrade toRemove);

    /**
     * Initialize a trade from a remote server
     * This is called when a new trade is created
     * Or when the current server receives trades from other servers during query time
     *
     * @param serverId The server that is owning the trade right now
     * @param trade    The trade to initialize
     */
    void initializeTrade(int serverId, INewTrade trade);

}
