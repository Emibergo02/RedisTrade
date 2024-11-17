package dev.unnm3d.redistrade.guis;

import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.utils.ReceiptBuilder;
import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.objects.NewTrade;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.item.Item;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TradeManager {

    private final RedisTrade plugin;
    private final ConcurrentHashMap<UUID, NewTrade> trades;
    private final ConcurrentHashMap<UUID, UUID> playerTrades;
    private final ConcurrentHashMap<String, HashSet<String>> ignorePlayers;

    public TradeManager(RedisTrade plugin) {
        this.plugin = plugin;
        this.trades = new ConcurrentHashMap<>();
        this.playerTrades = new ConcurrentHashMap<>();
        this.ignorePlayers = new ConcurrentHashMap<>();

        plugin.getDataStorage().restoreTrades().thenAccept(trades ->
                trades.forEach(this::tradeUpdate));
    }

    public void startTrade(Player traderPlayer, String targetName) {
        if (traderPlayer.getName().equals(targetName)) {
            traderPlayer.sendRichMessage(Messages.instance().tradeWithYourself);
            return;
        }
        //Create Trade and send update (and open inventories)
        plugin.getServer().getScheduler().runTask(plugin, () ->
                plugin.getPlayerListManager().getPlayerUUID(targetName)
                        .ifPresentOrElse(uuid -> {
                            if (openAlreadyStarted(traderPlayer, uuid)) return;
                            final NewTrade trade = new NewTrade(plugin.getDataCache(), traderPlayer.getUniqueId(), uuid,
                                    traderPlayer.getName(), targetName);
                            tradeUpdate(trade);
                            //Update trade calls invite message
                            plugin.getDataCache().createTrade(trade);

                            traderPlayer.sendRichMessage(Messages.instance().tradeCreated
                                    .replace("%player%", targetName));


                            openWindow(trade, traderPlayer.getUniqueId(), true);
                        }, () -> traderPlayer.sendRichMessage(Messages.instance().playerNotFound
                                .replace("%player%", targetName))));
    }

    public Optional<NewTrade> getActiveTrade(UUID playerUUID) {
        return Optional.ofNullable(playerTrades.get(playerUUID))
                .map(trades::get);
    }

    public void openBrowser(Player player, UUID targetUUID, LocalDateTime start, LocalDateTime end) {
        System.out.println("Opening browser");
        plugin.getDataStorage().getArchivedTrades(targetUUID, start, end)
                .thenAcceptAsync(trades -> {
                    final List<Item> receiptItems = trades.entrySet().stream()
                            .map(entry -> ReceiptBuilder.buildReceipt(entry.getValue(), entry.getKey()))
                            .toList();
                    System.out.println("Opening browser with " + receiptItems.size() + " items");
                    Bukkit.getScheduler().runTask(plugin, () ->
                            new TradeBrowserGUI(receiptItems).openWindow(player));
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
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
            //If the trade target is the current target open the window for the trader
            if (traderTrade.isTarget(targetUUID)) {
                openWindow(traderTrade, traderPlayer.getUniqueId(), true);
                return true;
            }
            //If the trade target is the current target open the window for the trader
            if (traderTrade.isTrader(targetUUID)) {
                openWindow(traderTrade, traderPlayer.getUniqueId(), false);
                return true;
            }

            plugin.getPlayerListManager().getPlayerName(traderTrade.getTargetUUID())
                    .ifPresent(name -> traderPlayer.sendRichMessage(Messages.instance().alreadyInTrade
                            .replace("%player%", name)));

            return true;
        }
        System.out.println("Trader trade is null");

        final NewTrade targetTrade = Optional.ofNullable(playerTrades.get(targetUUID))
                .map(trades::get)
                .orElse(null);
        if (targetTrade != null) {
            if (targetTrade.isTarget(traderPlayer.getUniqueId())) {
                openWindow(targetTrade, traderPlayer.getUniqueId(), false);
                return true;
            }
            if (targetTrade.isTrader(traderPlayer.getUniqueId())) {
                openWindow(targetTrade, traderPlayer.getUniqueId(), true);
                return true;
            }
            plugin.getPlayerListManager().getPlayerName(targetUUID)
                    .ifPresent(name -> traderPlayer.sendRichMessage(Messages.instance().targetAlreadyInTrade
                            .replace("%player%", name)));
            return true;
        }
        System.out.println("Target trade is null");
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
            System.out.println("Cancelling trade. Current trades contains target: " + playerTrades.containsKey(trade.getTargetUUID()));
            if (!playerTrades.containsKey(trade.getTargetUUID())) {
                removeTrade(trade.getUuid());
            }
        } else {
            //Check if Current trade does not contain the trader
            System.out.println("Cancelling trade. Current trades contains trader: " + playerTrades.containsKey(trade.getTraderUUID()));
            if (!playerTrades.containsKey(trade.getTraderUUID())) {
                removeTrade(trade.getUuid());
            }
        }
    }

    public boolean openWindow(NewTrade trade, UUID playerUUID, boolean isTrader) {
        //If the other side is empty, do not add the trade to the current trades
        //Because the player has already retrieved his items
        if (trade.getOrderInfo(!isTrader).getStatus() != OrderInfo.Status.RETRIEVED) {
            playerTrades.put(playerUUID, trade.getUuid());
            System.out.println("Adding player to current trades");
        }

        return trade.openWindow(playerUUID, isTrader);
    }

    private void removeTrade(UUID tradeUUID) {
        trades.remove(tradeUUID);
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

    public void tradeUpdate(NewTrade trade) {
        if (trades.containsKey(trade.getUuid())) return;
        trades.put(trade.getUuid(), trade);
        playerTrades.put(trade.getTraderUUID(), trade.getUuid());

        if (isIgnoring(trade.getTargetName(), trade.getTraderName())) return;

        Player foundPlayer = plugin.getServer().getPlayer(trade.getTargetUUID());
        if (foundPlayer != null) {
            plugin.getPlayerListManager().getPlayerName(trade.getTraderUUID())
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
