package dev.unnm3d.redistrade.core;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.data.Database;
import dev.unnm3d.redistrade.guis.TradeBrowserGUI;
import dev.unnm3d.redistrade.utils.ReceiptBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.item.Item;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TradeManager {

    private final RedisTrade plugin;
    final ConcurrentHashMap<UUID, NewTrade> trades;
    private final ConcurrentHashMap<UUID, UUID> playerTrades;
    private final ConcurrentHashMap<String, HashSet<String>> ignorePlayers;

    private final ConcurrentHashMap<Integer, UUID> tradeServerOwners;
    private long lastQuery;

    public TradeManager(RedisTrade plugin) {
        this.plugin = plugin;
        this.trades = new ConcurrentHashMap<>();
        this.tradeServerOwners = new ConcurrentHashMap<>();
        this.playerTrades = new ConcurrentHashMap<>();
        this.ignorePlayers = new ConcurrentHashMap<>();

        plugin.getDataStorage().restoreTrades().thenAccept(trades -> {
                    trades.forEach(this::initializeTrade);
                    plugin.getDataCache().sendQuery();
                    lastQuery = System.currentTimeMillis();
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
    }

    public void startTrade(Player traderPlayer, String targetName) {
        if (traderPlayer.getName().equals(targetName)) {
            traderPlayer.sendRichMessage(Messages.instance().tradeWithYourself);
            return;
        }

        //Create Trade and send update (and open inventories)
        plugin.getPlayerListManager().getPlayerUUID(targetName)
                .ifPresentOrElse(uuid -> {
                    if (openAlreadyStarted(traderPlayer, uuid)) return;

                    final NewTrade trade = new NewTrade(plugin.getDataCache(), traderPlayer.getUniqueId(), uuid,
                            traderPlayer.getName(), targetName);
                    //Update trade calls invite message remotely
                    plugin.getDataCache().sendFullTrade(trade).whenComplete((aLong, throwable) -> {
                        if (throwable == null) {
                            //Update trade calls invite message locally
                            initializeTrade(RedisTrade.getServerId(), trade);

                            plugin.getServer().getScheduler().runTask(plugin, () ->
                                    openWindow(trade, traderPlayer.getUniqueId()));
                            return;
                        }

                        traderPlayer.sendRichMessage(Messages.instance().newTradesLock);
                    });

                    traderPlayer.sendRichMessage(Messages.instance().tradeCreated
                            .replace("%player%", targetName));

                }, () -> traderPlayer.sendRichMessage(Messages.instance().playerNotFound
                        .replace("%player%", targetName)));
    }

    /**
     * Send all current trades to the other servers
     * This is called when a new server joins the network (aka a query is received)
     */
    public void sendAllCurrentTrades() {
        tradeServerOwners.forEach((serverId, tradeUUID) -> {
            if (serverId != RedisTrade.getServerId()) return;
            NewTrade trade = trades.get(tradeUUID);
            if (trade == null) return;
            plugin.getDataCache().sendFullTrade(trade);
            if (Settings.instance().debug) {
                plugin.getLogger().info("Sending trade query response: " + trade.getUuid());
            }
        });
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
    public boolean openAlreadyStarted(Player traderPlayer, UUID targetUUID) {
        final NewTrade traderTrade = Optional.ofNullable(playerTrades.get(traderPlayer.getUniqueId()))
                .map(trades::get)
                .orElse(null);
        if (traderTrade != null) {
            if (traderTrade.isTarget(targetUUID) || traderTrade.isTrader(targetUUID)) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        openWindow(traderTrade, traderPlayer.getUniqueId()));
                return true;
            }

            plugin.getPlayerListManager().getPlayerName(traderTrade.getOtherSide().getTraderUUID())
                    .ifPresent(name -> traderPlayer.sendRichMessage(Messages.instance().alreadyInTrade
                            .replace("%player%", name)));

            return true;
        }

        final NewTrade targetTrade = Optional.ofNullable(playerTrades.get(targetUUID))
                .map(trades::get)
                .orElse(null);
        if (targetTrade != null) {
            if (targetTrade.isTarget(traderPlayer.getUniqueId()) || targetTrade.isTrader(traderPlayer.getUniqueId())) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        openWindow(targetTrade, traderPlayer.getUniqueId()));
                return true;
            }

            plugin.getPlayerListManager().getPlayerName(targetUUID)
                    .ifPresent(name -> traderPlayer.sendRichMessage(Messages.instance().targetAlreadyInTrade
                            .replace("%player%", name)));
            return true;
        }
        return false;
    }

    /**
     * Finish the trade and disconnect the player name from the trade UUID.
     * If both trader and target are disconnected, remove the trade from tradeguis
     *
     * @param playerUUID The trader to be removed
     */
    public void finishTrade(UUID playerUUID) {
        //Remove the trade only if both trader and target are removed from the current trades
        UUID removedTradeUUID = playerTrades.remove(playerUUID);
        if (removedTradeUUID == null) return;
        NewTrade trade = trades.get(removedTradeUUID);
        if (trade == null) return;
        //Check if Current trade does not contain the target
        if (trade.isTrader(playerUUID)) {
            if (!playerTrades.containsKey(trade.getOtherSide().getTraderUUID())) {
                removeTrade(trade.getUuid());
                if (Settings.instance().debug) {
                    plugin.getLogger().info("Trade removed: " + trade.getUuid() + " target isn't in current trades");
                }
            }
        } else {
            //Check if Current trade does not contain the trader
            if (!playerTrades.containsKey(trade.getTraderSide().getTraderUUID())) {
                removeTrade(trade.getUuid());
                if (Settings.instance().debug) {
                    plugin.getLogger().info("Trade removed: " + trade.getUuid() + " trader isn't in current trades");
                }
            }
        }
    }

    public void cancelAllTimers() {
        trades.values().forEach(trade -> {
            trade.getCompletionTimer().cancel();
            if (trade.getTraderSide().getOrder().getStatus() == OrderInfo.Status.CONFIRMED) {
                trade.setStatus(OrderInfo.Status.REFUTED, true);
            }
            if (trade.getOtherSide().getOrder().getStatus() == OrderInfo.Status.CONFIRMED) {
                trade.setStatus(OrderInfo.Status.REFUTED, false);
            }
        });
    }


    public boolean openWindow(NewTrade trade, UUID playerUUID) {
        boolean isTrader = trade.isTrader(playerUUID);
        //If the other side is empty, do not add the trade to the current trades
        //Because the player has already retrieved his items
        if (trade.getOrderInfo(!isTrader).getStatus() != OrderInfo.Status.RETRIEVED) {
            playerTrades.put(playerUUID, trade.getUuid());
        }

        return trade.openWindow(playerUUID, isTrader);
    }

    public void removeTrade(UUID tradeUUID) {
        trades.remove(tradeUUID);
        playerTrades.values().removeIf(uuid -> uuid.equals(tradeUUID));
        tradeServerOwners.values().removeIf(uuid -> uuid.equals(tradeUUID));
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

        trade.initializeGuis();
        tradeServerOwners.put(serverId, trade.getUuid());
        playerTrades.put(trade.getTraderSide().getTraderUUID(), trade.getUuid());
        trades.put(trade.getUuid(), trade);

        //Send trade invite message
        if (isIgnoring(trade.getOtherSide().getTraderName(), trade.getTraderSide().getTraderName())) return;
        Player foundPlayer = plugin.getServer().getPlayer(trade.getOtherSide().getTraderUUID());
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
        trades.values().forEach(trade ->
                plugin.getDataStorage().backupTrade(trade));
    }


}
