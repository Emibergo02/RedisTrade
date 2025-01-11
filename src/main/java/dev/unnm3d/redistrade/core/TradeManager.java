package dev.unnm3d.redistrade.core;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.data.Database;
import dev.unnm3d.redistrade.core.enums.Status;
import dev.unnm3d.redistrade.guis.TradeBrowserGUI;
import dev.unnm3d.redistrade.guis.TradeGuiBuilder;
import dev.unnm3d.redistrade.core.enums.Actor;
import dev.unnm3d.redistrade.utils.ReceiptBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.item.Item;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public class TradeManager {

    private final RedisTrade plugin;
    final ConcurrentHashMap<UUID, NewTrade> trades;
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

        //Create Trade and send update (and open inventories)
        return plugin.getPlayerListManager().getPlayerUUID(targetName)
                .map(uuid -> {
                    final Optional<NewTrade> alreadyTrade = Optional.ofNullable(openAlreadyStarted(traderPlayer, uuid));
                    if (alreadyTrade.isPresent()) return CompletableFuture.completedFuture(alreadyTrade);

                    final NewTrade trade = new NewTrade(traderPlayer.getUniqueId(), uuid,
                            traderPlayer.getName(), targetName);
                    //Update trade calls invite message remotely
                    return plugin.getDataCache().sendFullTrade(trade)
                            .exceptionally(throwable -> {
                                traderPlayer.sendRichMessage(Messages.instance().newTradesLock);
                                return -1L;
                            }).thenApply(aLong -> {
                                Optional<NewTrade> tradeOptional = Optional.empty();
                                if (aLong == -1) return tradeOptional;
                                tradeOptional = Optional.of(trade);

                                initializeTrade(RedisTrade.getServerId(), trade);
                                traderPlayer.sendRichMessage(Messages.instance().tradeCreated.replace("%player%", targetName));
                                plugin.getServer().getScheduler().runTask(plugin, () -> openWindow(trade, traderPlayer.getUniqueId()));
                                return tradeOptional;
                            });
                }).orElseGet(() -> {
                    traderPlayer.sendRichMessage(Messages.instance().playerNotFound.replace("%player%", targetName));
                    return CompletableFuture.completedFuture(Optional.empty());
                });
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
                    if (Settings.instance().debug) {
                        plugin.getLogger().info("Sending trade query response: " + trade.getUuid());
                    }
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
     * @return true if a trade window is opened
     */
    public NewTrade openAlreadyStarted(Player traderPlayer, UUID targetUUID) {
        final NewTrade traderTrade = Optional.ofNullable(playerTrades.get(traderPlayer.getUniqueId()))
                .map(trades::get)
                .orElse(null);
        if (traderTrade != null) {
            if (traderTrade.isTarget(targetUUID) || traderTrade.isTrader(targetUUID)) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        openWindow(traderTrade, traderPlayer.getUniqueId()));
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
            if (targetTrade.isTarget(traderPlayer.getUniqueId()) || targetTrade.isTrader(traderPlayer.getUniqueId())) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        openWindow(targetTrade, traderPlayer.getUniqueId()));
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
     *
     * @param tradeUUID     The trade to be close
     * @param actorSide     The actor side that is closing the trade
     */
    public void closeTrade(UUID tradeUUID, Actor actorSide) {
        //Remove the trade only if both trader and target are removed from the current trades
        NewTrade trade = trades.get(tradeUUID);
        if (trade == null) return;
        trade.setOpened(false, actorSide);

        if (!playerTrades.containsKey(trade.getTradeSide(actorSide.opposite()).getTraderUUID())) {
            removeTrade(trade.getUuid());
            if (Settings.instance().debug) {
                plugin.getLogger().info("Trade removed: " + trade.getUuid() + " target isn't in current trades");
            }
        } else {
            playerTrades.remove(trade.getTradeSide(actorSide).getTraderUUID());
            if (Settings.instance().debug) {
                plugin.getLogger().info("Trade closed for: " + trade.getTradeSide(actorSide).getTraderName());
            }
        }
    }


    public boolean openWindow(NewTrade trade, UUID playerUUID) {
        Actor actor = trade.getViewerType(playerUUID);
        //If the other side is empty, do not add the trade to the current trades
        //Because the player has already retrieved his items
        if (trade.getOrderInfo(actor.opposite()).getStatus() != Status.RETRIEVED) {
            playerTrades.put(playerUUID, trade.getUuid());
        }

        return trade.openWindow(playerUUID, actor);
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

        if (Settings.instance().debug) {
            plugin.getLogger().info("initializeTrade: " + trade.getUuid());
        }

        trade.getTradeSide(Actor.TRADER).setSidePerspective(
                new TradeGuiBuilder(trade, Actor.TRADER).build());
        trade.getTradeSide(Actor.CUSTOMER).setSidePerspective(
                new TradeGuiBuilder(trade, Actor.CUSTOMER).build());

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

                    traderViewers.forEach(player -> trade.openWindow(player.getUniqueId(), Actor.TRADER));
                    customerViewers.forEach(player -> trade.openWindow(player.getUniqueId(), Actor.CUSTOMER));
                });
        setTradeServerOwner(trade.getUuid(), serverId);
        playerTrades.put(trade.getTraderSide().getTraderUUID(), trade.getUuid());

        trades.put(trade.getUuid(), trade);

        //Skip invitation if the player has already opened the trade
        if (trade.getCustomerSide().isOpened()) {
            if (Settings.instance().debug) {
                plugin.getLogger().info("Open customer side for: " + trade.getCustomerSide().getTraderName());
            }
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
