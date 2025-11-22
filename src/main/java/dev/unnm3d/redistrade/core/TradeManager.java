package dev.unnm3d.redistrade.core;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.enums.Actor;
import dev.unnm3d.redistrade.core.enums.Status;
import dev.unnm3d.redistrade.data.Database;
import dev.unnm3d.redistrade.guis.TradeBrowserGUI;
import dev.unnm3d.redistrade.guis.TradeGuiBuilder;
import dev.unnm3d.redistrade.restriction.RestrictionService;
import dev.unnm3d.redistrade.utils.ReceiptBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import xyz.xenondevs.invui.item.Item;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public class TradeManager {

    private final RedisTrade plugin;
    private final ConcurrentHashMap<UUID, NewTrade> trades;
    private final ConcurrentHashMap<UUID, UUID> playerTrades;
    private final ConcurrentHashMap<String, HashSet<String>> ignorePlayers;

    private final ConcurrentHashMap<UUID, Integer> tradeServerOwners;

    public TradeManager(RedisTrade plugin) {
        this.plugin = plugin;
        this.trades = new ConcurrentHashMap<>();
        this.tradeServerOwners = new ConcurrentHashMap<>();
        this.playerTrades = new ConcurrentHashMap<>();
        this.ignorePlayers = new ConcurrentHashMap<>();

        plugin.getDataStorage().restoreTrades().thenAccept(trades -> {
            trades.forEach(this::initializeTrade);
            plugin.getDataCache().sendQuery();
        }).exceptionally(e -> {
            e.printStackTrace();
            return null;
        });
    }


    public CompletionStage<Optional<NewTrade>> startTrade(Player traderPlayer, String targetName) {
        if (traderPlayer.getName().equals(targetName)) {
            traderPlayer.sendRichMessage(Messages.instance().tradeWithYourself);
            return CompletableFuture.completedFuture(Optional.empty());
        }

        final Optional<NewTrade> noTradeOpt = Optional.empty();
        //Create Trade and send update (and open inventories)
        return plugin.getPlayerListManager().getPlayerUUID(targetName)
                .map(uuid -> {
                    final Optional<NewTrade> alreadyTrade = Optional.ofNullable(openAlreadyStarted(traderPlayer, uuid));
                    if (alreadyTrade.isPresent()) return CompletableFuture.completedFuture(alreadyTrade);

                    if (checkInvalidDistance(traderPlayer, uuid)) {
                        traderPlayer.sendRichMessage(Messages.instance().tradeDistance
                                .replace("%blocks%", String.valueOf(Settings.instance().tradeDistance)));

                        return CompletableFuture.completedFuture(noTradeOpt);
                    }

                    final NewTrade trade = new NewTrade(traderPlayer.getUniqueId(), uuid,
                            traderPlayer.getName(), targetName);
                    //Update trade calls invite message remotely
                    return plugin.getDataCache().sendFullTrade(trade)
                            .exceptionally(throwable -> {
                                traderPlayer.sendRichMessage(Messages.instance().newTradesLock);
                                return -1L;
                            }).thenApply(aLong -> {
                                if (aLong == -1) return noTradeOpt;

                                initializeTrade(RedisTrade.getServerId(), trade);
                                traderPlayer.sendRichMessage(Messages.instance().tradeCreated.replace("%player%", targetName));
                                plugin.getServer().getScheduler().runTask(plugin, () -> openWindow(trade, traderPlayer));
                                return Optional.of(trade);
                            });
                }).orElseGet(() -> {
                    traderPlayer.sendRichMessage(Messages.instance().playerNotFound.replace("%player%", targetName));
                    return CompletableFuture.completedFuture(noTradeOpt);
                });
    }

    /**
     * Check if the distance is invalid
     *
     * @param traderPlayer The first player
     * @param otherPlayer  The second player
     * @return true if invalid, false otherwise
     */
    public boolean checkInvalidDistance(Player traderPlayer, UUID otherPlayer) {
        if (Settings.instance().tradeDistance < 0) return false;
        final Player p = plugin.getServer().getPlayer(otherPlayer);
        if (p == null) return true;
        if (Settings.instance().tradeDistance == 0 && traderPlayer.getWorld().equals(p.getWorld())) return false;
        return Settings.instance().tradeDistance < traderPlayer.getLocation().distance(p.getLocation());
    }

    /**
     * Send all current trades to the other servers
     * This is called when a new server joins the network (aka a query is received)
     */
    public void sendAllCurrentTrades() {
        tradeServerOwners.entrySet().stream().filter(record -> record.getValue() == RedisTrade.getServerId())
                .forEach(record -> {
                    NewTrade trade = trades.get(record.getKey());
                    if (trade == null) return;
                    plugin.getDataCache().sendFullTrade(trade);
                    RedisTrade.debug("Sending trade query response: " + trade.getUuid());
                });
    }

    public void setTradeServerOwner(UUID tradeUUID, int serverId) {
        tradeServerOwners.put(tradeUUID, serverId);
    }

    public boolean isOwner(UUID tradeUUID) {
        return tradeServerOwners.containsKey(tradeUUID) && tradeServerOwners.get(tradeUUID) == RedisTrade.getServerId();
    }

    public Optional<NewTrade> getActiveTrade(UUID playerUUID) {
        return Optional.ofNullable(playerTrades.get(playerUUID))
                .map(trades::get);
    }

    public void openBrowser(Player player, UUID targetUUID, LocalDateTime start, LocalDateTime end) {
        if (plugin.getDataStorage() instanceof Database database) {
            database.getArchivedTrades(targetUUID, start, end)
                    .thenAcceptAsync(trades -> {
                        final List<Item> receiptItems = trades.entrySet().stream()
                                .map(entry -> ReceiptBuilder.buildReceipt(entry.getValue(), entry.getKey()))
                                .toList();
                        Bukkit.getScheduler().runTask(plugin, () ->
                                new TradeBrowserGUI(receiptItems).openWindow(player));
                    })
                    .exceptionally(e -> {
                        e.printStackTrace();
                        return null;
                    });
        } else {
            player.sendRichMessage(Messages.instance().notSupported
                    .replace("%feature%", "Redis database"));
        }
    }

    /**
     * If the target has a running trade with the trader, open the trade window for the trader
     * If the trader has a running trade with the target, open the trade window for the trader
     *
     * @return the trade if it exists
     */
    @Nullable
    public NewTrade openAlreadyStarted(Player traderPlayer, UUID targetUUID) {
        final NewTrade traderTrade = Optional.ofNullable(playerTrades.get(traderPlayer.getUniqueId()))
                .map(trades::get)
                .orElse(null);
        if (traderTrade != null) {
            if (traderTrade.isCustomer(targetUUID) || traderTrade.isTrader(targetUUID)) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        openWindow(traderTrade, traderPlayer));
                return traderTrade;
            }

            plugin.getPlayerListManager().getPlayerName(traderTrade.getCustomerSide().getTraderUUID())
                    .ifPresent(name -> traderPlayer.sendRichMessage(Messages.instance().alreadyInTrade
                            .replace("%player%", name)));

            return traderTrade;
        }

        final NewTrade targetTrade = Optional.ofNullable(playerTrades.get(targetUUID))
                .map(trades::get)
                .orElse(null);
        if (targetTrade != null) {
            if (targetTrade.isCustomer(traderPlayer.getUniqueId()) || targetTrade.isTrader(traderPlayer.getUniqueId())) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        openWindow(targetTrade, traderPlayer));
                return targetTrade;
            }

            plugin.getPlayerListManager().getPlayerName(targetUUID)
                    .ifPresent(name -> traderPlayer.sendRichMessage(Messages.instance().targetAlreadyInTrade
                            .replace("%player%", name)));
            return targetTrade;
        }
        return null;
    }

    /**
     * Finish the trade and disconnect the player name from the trade UUID.
     * If both trader and target are disconnected, remove the trade from tradeguis
     * Show the ReviewGUI button
     *
     * @param tradeUUID The trade to be close
     * @param actorSide The actor side that is closing the trade
     */
    public void finishTrade(UUID tradeUUID, Actor actorSide) {
        //Remove the trade only if both trader and target are removed from the current trades
        NewTrade trade = trades.get(tradeUUID);
        if (trade == null) return;
        trade.setOpened(false, actorSide);
        final TradeSide actorTradeSide = trade.getTradeSide(actorSide);

        if (!playerTrades.containsKey(trade.getTradeSide(actorSide.opposite()).getTraderUUID())) {
            removeTrade(trade.getUuid());
            RedisTrade.debug(trade.getUuid() + " trade removed: other side isn't connected to this trade");
        } else {
            playerTrades.remove(actorTradeSide.getTraderUUID());
            RedisTrade.debug(trade.getUuid() + " trade closed for: " + actorTradeSide.getTraderName());
        }

    }

    /**
     * Give back the items to the player and try to close the trade
     * It is closed only if the sides are empty
     *
     * @param player    The player to give back the items
     * @param tradeUUID The trade to close
     */
    public void collectItemsAndClose(Player player, UUID tradeUUID) {
        final NewTrade trade = trades.get(tradeUUID);
        if (trade == null) { //If the trade is not found, it has been already closed
            player.closeInventory();
            return;
        }
        final Actor tradeSide = trade.getActor(player);
        //Spectators can't cancel trades
        if (tradeSide == Actor.SPECTATOR) return;

        //If the actor side is still in the first stage, return the items and try retrieve phase (AKA close trade)
        final TradeSide actorSide = trade.getTradeSide(tradeSide);
        final TradeSide oppositeTradeSide = trade.getTradeSide(tradeSide.opposite());
        if (actorSide.getOrder().getStatus() == Status.REFUSED) {
            //Self-trigger retrieve phase from both sides
            short returnedItems = trade.returnItems(player, tradeSide);
            RedisTrade.debug(trade.getUuid() + " Returned " + returnedItems + " items to " + player.getName());

            if (actorSide.getOrder().getStatus() == Status.RETRIEVED) {
                player.closeInventory();
                return;
            }
            trade.retrievedPhase(tradeSide, tradeSide).thenAcceptAsync(finalStatus -> {
                if (finalStatus != Status.RETRIEVED) {
                    player.sendRichMessage(Messages.instance().tradeRunning
                            .replace("%player%", trade.getTradeSide(tradeSide.opposite()).getTraderName()));
                    return;
                }

                trade.refundSide(tradeSide);
                actorSide.getSidePerspective().findAllCurrentViewers()
                        .stream().filter(he -> trade.getActor(he) != Actor.ADMIN)
                        .forEach(Player::closeInventory);

                if (oppositeTradeSide.getOrder().getStatus() == Status.RETRIEVED) {
                    player.closeInventory();
                    return;
                }
                trade.retrievedPhase(tradeSide.opposite(), tradeSide.opposite()).thenAccept(finalStatus2 -> {
                    if (finalStatus2 == Status.RETRIEVED) {
                        trade.refundSide(tradeSide.opposite());
                    }
                });

            });
            return;
        }
        if (oppositeTradeSide.getOrder().getStatus() == Status.RETRIEVED) {
            player.closeInventory();
            return;
        }
        //If the opposite side has completed the trade, return the items and try retrieve phase (AKA close trade)
        if (oppositeTradeSide.getOrder().getStatus() == Status.COMPLETED) {
            short returnedItems = trade.returnItems(player, tradeSide.opposite());
            trade.retrievedPhase(tradeSide.opposite(), tradeSide).thenAccept(result -> {
                if (result != Status.RETRIEVED) {
                    player.sendRichMessage(Messages.instance().tradeRunning
                            .replace("%player%", trade.getTradeSide(tradeSide.opposite()).getTraderName()));
                    return;
                }
                actorSide.getSidePerspective().findAllCurrentViewers()
                        .stream().filter(he -> trade.getActor(he) != Actor.ADMIN)
                        .forEach(Player::closeInventory);
            });
            RedisTrade.debug(trade.getUuid() + " Returned " + returnedItems + " items to " + player.getName());
            return;
        }
        player.sendRichMessage(Messages.instance().tradeRunning
                .replace("%player%", trade.getTradeSide(tradeSide.opposite()).getTraderName()));
    }


    public void openWindow(NewTrade trade, Player player) {
        final RestrictionService.Restriction restriction = plugin.getRestrictionService().getRestriction(player, trade);
        if (restriction != null) {
            player.sendRichMessage(Messages.instance().restrictionMessages
                    .getOrDefault(restriction.restrictionName(), Messages.instance().tradeRestricted));
            return;
        }
        final Actor actorSide = trade.getActor(player);
        //Set the trade as opened only if we are in the first stage
        if (trade.getTradeSide(actorSide).getOrder().getStatus() == Status.REFUSED && actorSide.isParticipant()) {
            playerTrades.put(player.getUniqueId(), trade.getUuid());
            trade.setOpened(true, actorSide);
            RedisTrade.debug(trade.getUuid() + " " + trade.getTradeSide(actorSide).getTraderName() + " opened trade window");
        }
        trade.openWindow(player, actorSide);
    }

    /**
     * Receive a remote open trade request
     * The close trade is handled by the finish trade method
     *
     * @param tradeUUID The UUID of the trade to open
     * @param actorSide The actor side that is opening the trade
     */
    public void remoteOpenTrade(UUID tradeUUID, Actor actorSide) {
        NewTrade trade = trades.get(tradeUUID);
        if (trade == null) {
            RedisTrade.debug("Remote open on " + tradeUUID + ": Trade not found");
            return;
        }

        playerTrades.put(trade.getTradeSide(actorSide).getTraderUUID(), tradeUUID);
        trade.getTradeSide(actorSide).setOpened(true);
        RedisTrade.debug(tradeUUID + " " + trade.getTradeSide(actorSide).getTraderName() + " accepted to open trade");
        //The close is handler by the finish trade method
    }

    public void removeTrade(UUID tradeUUID) {
        trades.remove(tradeUUID);
        playerTrades.values().removeIf(uuid -> uuid.equals(tradeUUID));
        tradeServerOwners.remove(tradeUUID);
        plugin.getDataStorage().removeTradeBackup(tradeUUID);
    }

    public void loadIgnoredPlayers(String playerName) {
        plugin.getDataStorage().getIgnoredPlayers(playerName)
                .thenAccept(ignoredPlayers -> ignorePlayers.put(playerName, new HashSet<>(ignoredPlayers)));
    }

    public void ignorePlayerCloud(String playerName, String targetName, boolean ignore) {
        ignoreUpdate(playerName, targetName, ignore);
        plugin.getDataStorage().ignorePlayer(playerName, targetName, ignore);
    }

    public boolean isIgnoring(String playerName, String targetName) {
        return ignorePlayers.containsKey(playerName) && ignorePlayers.get(playerName).contains(targetName);
    }

    public Set<String> getIgnoredPlayers(String playerName) {
        return ignorePlayers.getOrDefault(playerName, new HashSet<>());
    }

    public Optional<NewTrade> getTrade(UUID tradeUUID) {
        return Optional.ofNullable(trades.get(tradeUUID));
    }

    /**
     * Initialize a trade from a remote server
     * This is called when a new trade is created
     * Or when the current server receives trades from other servers during query time
     *
     * @param serverId The server that is owning the trade right now
     * @param trade    The trade to initialize
     */
    public void initializeTrade(int serverId, NewTrade trade) {
        RedisTrade.debug("Trade initialized: " + trade.getUuid());
        try {
            trade.getTradeSide(Actor.TRADER).setSidePerspective(
                    new TradeGuiBuilder(trade, Actor.TRADER).build());
            trade.getTradeSide(Actor.CUSTOMER).setSidePerspective(
                    new TradeGuiBuilder(trade, Actor.CUSTOMER).build());
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Remove the trade if it already exists
        //This happens when the trade is taken from backup table,
        // and we receive a full trade update from the query system
        Optional.ofNullable(trades.get(trade.getUuid()))
                .ifPresent(tradeFound -> {

                    final Set<Player> traderViewers = tradeFound.getTradeSide(Actor.TRADER).getSidePerspective()
                            .findAllCurrentViewers();
                    final Set<Player> customerViewers = tradeFound.getTradeSide(Actor.CUSTOMER).getSidePerspective()
                            .findAllCurrentViewers();
                    tradeFound.getTradeSide(Actor.TRADER).getSidePerspective().closeForAllViewers();
                    tradeFound.getTradeSide(Actor.CUSTOMER).getSidePerspective().closeForAllViewers();

                    removeTrade(tradeFound.getUuid());

                    traderViewers.forEach(player -> trade.openWindow(player, Actor.TRADER));
                    customerViewers.forEach(player -> trade.openWindow(player, Actor.CUSTOMER));
                });
        setTradeServerOwner(trade.getUuid(), serverId);
        trades.put(trade.getUuid(), trade);

        if (trade.getTraderSide().isOpened()) {
            RedisTrade.debug(trade.getUuid() + " Set opened flag for trader: " + trade.getTraderSide().getTraderName());
            playerTrades.put(trade.getTraderSide().getTraderUUID(), trade.getUuid());
        }
        if (trade.getCustomerSide().isOpened()) {
            RedisTrade.debug(trade.getUuid() + " Set opened flag for customer: " + trade.getCustomerSide().getTraderName());
            playerTrades.put(trade.getCustomerSide().getTraderUUID(), trade.getUuid());
            return;
        }

        //Send trade invite message
        if (isIgnoring(trade.getCustomerSide().getTraderName(), trade.getTraderSide().getTraderName())) return;
        Player foundPlayer = plugin.getServer().getPlayer(trade.getCustomerSide().getTraderUUID());
        if (foundPlayer != null) {
            plugin.getPlayerListManager().getPlayerName(trade.getTraderSide().getTraderUUID())
                    .ifPresent(name -> foundPlayer.sendRichMessage(Messages.instance().tradeReceived
                            .replace("%player%", name)));
        }
    }

    public void ignoreUpdate(String playerName, String targetName, boolean ignore) {
        ignorePlayers.compute(playerName, (key, value) -> {
            if (value == null) {
                value = new HashSet<>();
            }
            if (ignore) {
                value.add(targetName);
            } else {
                value.remove(targetName);
            }
            return value;
        });
    }

    public void close() {
        plugin.getLogger().info("Saving active trades to database");
        trades.values().forEach(trade -> plugin.getDataStorage().backupTrade(trade));
        plugin.getLogger().info("Finished saving active trades to database");
    }
}
