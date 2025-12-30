package dev.unnm3d.redistrade.core;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.GuiSettings;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.enums.Actor;
import dev.unnm3d.redistrade.core.enums.Status;
import dev.unnm3d.redistrade.core.enums.UpdateType;
import dev.unnm3d.redistrade.core.enums.ViewerUpdate;
import dev.unnm3d.redistrade.core.invites.InviteManager;
import dev.unnm3d.redistrade.data.Database;
import dev.unnm3d.redistrade.guis.TradeGuiBuilder;
import dev.unnm3d.redistrade.guis.browsers.ActiveTradesBrowserGUI;
import dev.unnm3d.redistrade.guis.browsers.ArchivedTradesBrowserGUI;
import dev.unnm3d.redistrade.restriction.RestrictionService;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import xyz.xenondevs.invui.window.Window;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TradeManager {

    private final RedisTrade plugin;
    private final ConcurrentHashMap<UUID, NewTrade> trades;
    private final ConcurrentHashMap<UUID, UUID> latestTrade;
    private final ConcurrentHashMap<String, HashSet<String>> ignorePlayers;
    @Getter
    private final InviteManager inviteManager;

    private final ConcurrentHashMap<UUID, Integer> tradeServerOwners;

    public TradeManager(RedisTrade plugin) {
        this.plugin = plugin;
        this.trades = new ConcurrentHashMap<>();
        this.tradeServerOwners = new ConcurrentHashMap<>();
        this.latestTrade = new ConcurrentHashMap<>();
        this.inviteManager = new InviteManager();
        this.ignorePlayers = new ConcurrentHashMap<>();

        plugin.getDataStorage().restoreTrades().thenAccept(trades -> {
            trades.forEach(this::initializeTrade);
            plugin.getDataCache().sendQuery();
        }).exceptionally(e -> {
            e.printStackTrace();
            return null;
        });
    }

    public CompletionStage<Optional<NewTrade>> startTrade(final @NonNull Player traderPlayer, final @NotNull UUID targetUUID, @Nullable String targetName) {
        if (targetName == null) {
            targetName = plugin.getPlayerListManager().getPlayerName(targetUUID).orElseThrow();
        }

        final UUID traderId = traderPlayer.getUniqueId();
        for (NewTrade trade : trades.values()) {
            if (trade.isParticipant(traderId) && trade.isParticipant(targetUUID)) {
                //already in a trade together
                return CompletableFuture.completedFuture(Optional.of(trade));
            }
        }

        final NewTrade trade = new NewTrade(traderPlayer.getUniqueId(), targetUUID,
          traderPlayer.getName(), targetName);
        return plugin.getDataCache().sendFullTrade(trade)
          .exceptionally(throwable -> {
              RedisTrade.debug("Failed to send trade to other servers: " + throwable.getMessage());
              return -1L;
          }).thenApply(aLong -> {
              if (aLong == -1) return Optional.empty();

              initializeTrade(RedisTrade.getServerId(), trade);
              return Optional.of(trade);
          });
    }

    public void startAcceptInviteAndOpen(Player player, UUID targetUUID, @NonNull String targetName, boolean force) {
        if (!force && RedisTrade.getInstance().getTradeManager().isIgnoring(targetName, player.getName())) {
            player.sendRichMessage(Messages.instance().tradeYouAreIgnored.replace("%player%", targetName));
            return;
        }
        plugin.getTradeManager().startTrade(player, targetUUID, targetName)
          .thenAccept(trade -> trade.ifPresentOrElse(t -> {
              //Open the trade window for the accepting player
              plugin.getTradeManager().getInviteManager().acceptInvitationOf(targetName);
              plugin.getTradeManager().openWindow(t, player, force);

          }, () -> player.sendRichMessage(Messages.instance().newTradesLock)));
    }

    /**
     * Opens the trade window for the player
     *
     * @param trade  The trade to open
     * @param player The player to open the window for
     * @param force  Whether to force open the window (bypass restrictions)
     * @param delay  The delay before opening the window in ticks
     */
    public void openWindow(NewTrade trade, Player player, boolean force, long delay) {
        if (!force) {
            final RestrictionService.Restriction restriction = plugin.getRestrictionService().getRestriction(player, trade);
            if (restriction != null) {
                player.sendRichMessage(Messages.instance().restrictionMessages
                  .getOrDefault(restriction.restrictionName(), Messages.instance().tradeRestricted));
                return;
            }
        }

        final Actor actorSide = trade.getActor(player);
        //Set the trade as opened only if we are in the first stage
        if ((force || trade.getTradeSide(actorSide).getOrder().getStatus() == Status.REFUSED) && actorSide.isParticipant()) {
            openAndSendWindow(trade, actorSide);
        }

        Window.Builder.Normal.Single tradeWindow = Window.single()
          .setTitle(GuiSettings.instance().tradeGuiTitle.replace("%player%",
            trade.getTradeSide(actorSide.opposite()).getTraderName()))
          .setGui(trade.getTradeSide(actorSide).getSidePerspective())
          .addCloseHandler(() -> {
              //If the player is a spectator/admin, don't send the message
              if (!trade.getActor(player).isParticipant()) return;
              final OrderInfo traderOrder = trade.getTradeSide(actorSide).getOrder();
              if (traderOrder.getStatus() != Status.RETRIEVED) {
                  player.sendRichMessage(Messages.instance().tradeRunning
                    .replace("%player%", trade.getTradeSide(actorSide.opposite()).getTraderName()));
              }
              if(Settings.instance().cancelOnClose) collectItemsAndClose(player, trade.getUuid());
          });

        player.getScheduler().runDelayed(RedisTrade.getInstance(), task -> {
            tradeWindow.open(player);
            this.latestTrade.put(player.getUniqueId(), trade.getUuid());
        }, null, delay);
    }

    /**
     * Instantly opens the trade window for the player
     *
     * @param trade  The trade to open
     * @param player The player to open the window for
     * @param force  Whether to force open the window (bypass restrictions)
     */
    public void openWindow(NewTrade trade, Player player, boolean force) {
        openWindow(trade, player, force, 1L);
    }

    /**
     * Locally update information about the trade opening
     *
     * @param trade     The trade to open
     * @param actorSide The actor side that is opening the trade
     */
    private void localOpenWindow(NewTrade trade, Actor actorSide) {
        if (trade.getTradeSide(actorSide).isOpened()) return;
        trade.getTradeSide(actorSide).setOpened(true);
        final TradeSide oppositeSide = trade.getTradeSide(actorSide.opposite());
        if (!oppositeSide.isOpened()) {
            plugin.getServer().getOnlinePlayers().stream()
              .filter(player -> player.getUniqueId().equals(oppositeSide.getTraderUUID()))
              .findFirst().ifPresent(player -> {
                  if (Settings.instance().openTradeOnAccept) {
                      plugin.getTradeManager().openWindow(trade, player, false);
                      return;
                  }
                  player.sendRichMessage(Messages.instance().tradeCustomerJoined
                    .replace("%player%", trade.getTradeSide(actorSide).getTraderName()));
              });
        }
    }

    /**
     * Open the trade window and send the update to other servers
     *
     * @param trade     The trade to open
     * @param actorSide The actor side that is opening the trade
     */
    private void openAndSendWindow(NewTrade trade, Actor actorSide) {
        RedisTrade.getInstance().getDataCache().updateTrade(trade.getUuid(),
          ViewerUpdate.valueOf(actorSide, UpdateType.OPEN), true);
        localOpenWindow(trade, actorSide);
        RedisTrade.debug(trade.getUuid() + " " + trade.getTradeSide(actorSide).getTraderName() + " opened trade window");
    }

    /**
     * Receive a remote open trade request
     * The close trade is handled by the finish trade method
     *
     * @param tradeUUID The UUID of the trade to open
     * @param actorSide The actor side that is opening the trade
     */
    public void receiveOpenWindow(UUID tradeUUID, Actor actorSide) {
        NewTrade trade = trades.get(tradeUUID);
        if (trade == null) {
            RedisTrade.debug("Remote open on " + tradeUUID + ": Trade not found");
            return;
        }
        localOpenWindow(trade, actorSide);
        RedisTrade.debug(trade.getUuid() + " " + trade.getTradeSide(actorSide).getTraderName() + " remotely opened trade window");
        //The close is handler by the finish trade method
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
        if (Settings.instance().tradeDistance < traderPlayer.getLocation().distance(p.getLocation())) {
            traderPlayer.sendRichMessage(Messages.instance().tradeDistance
              .replace("%blocks%", String.valueOf(Settings.instance().tradeDistance)));
            return true;
        }
        return false;
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

    public List<NewTrade> getPlayerActiveTrades(@NotNull UUID playerUUID) {
        return trades.values().stream()
          .filter(t -> t.isParticipant(playerUUID))
          .collect(Collectors.toList());
    }

    public void openArchivedTrades(Player player, UUID targetUUID, LocalDateTime start, LocalDateTime end) {
        if (plugin.getDataStorage() instanceof Database database) {
            database.getArchivedTrades(targetUUID, start, end)
              .thenAccept(trades -> ArchivedTradesBrowserGUI.openBrowser(player, trades))
              .exceptionally(e -> {
                  e.printStackTrace();
                  return null;
              });
        } else {
            player.sendRichMessage(Messages.instance().notSupported
              .replace("%feature%", "Redis database"));
        }
    }

    public void openActiveTrades(Player player) {
        final List<NewTrade> actives = getPlayerActiveTrades(player.getUniqueId());
        actives.removeIf(t->!t.getTradeSide(t.getActor(player)).isOpened());
        if (actives.isEmpty()) {
            player.sendRichMessage(Messages.instance().noPendingTrades);
            return;
        }
        if (actives.size() == 1) {
            NewTrade current = actives.getFirst();
            TradeSide oppositeSide = current.getTradeSide(current.getActor(player).opposite());
            startAcceptInviteAndOpen(player, oppositeSide.getTraderUUID(), oppositeSide.getTraderName(), true);
            return;
        }
        ActiveTradesBrowserGUI.openBrowser(player, player.getUniqueId(), actives);
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

        //If the opposite side is still open, just set the side as closed and return
        if (trade.getTradeSide(actorSide.opposite()).isOpened()) {
            trade.getTradeSide(actorSide).setOpened(false);
            RedisTrade.debug(trade.getUuid() + " trade closed for: " + trade.getTradeSide(actorSide).getTraderName() + ", other side still connected");
            return;
        }
        //TODO: TEST THIS DEEPLY, WE NEED TO MAKE SURE THE TRADE GET CANCELLED PROPERLY
        //Both sides are disconnected, remove the trade

        removeTrade(trade);
        RedisTrade.debug(trade.getUuid() + " trade removed: other side isn't connected to this trade");
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
        //Spectators can't cancel trades
        if(!trade.isParticipant(player.getUniqueId()))return;

        final Actor tradeSide = trade.getActor(player);

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

    public Optional<NewTrade> getLatestTrade(UUID playerId) {
        UUID tradeToOpen = latestTrade.get(playerId);
        if (tradeToOpen == null) return Optional.empty();
        final NewTrade trade = trades.get(tradeToOpen);
        if (trade == null) return Optional.empty();
        return Optional.of(trade);
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

    public List<NewTrade> getAllTrades() {
        return new ArrayList<>(trades.values());
    }

    public Optional<NewTrade> getTrade(UUID tradeUUID) {
        return Optional.ofNullable(trades.get(tradeUUID));
    }

    public void removeTrade(NewTrade toRemove) {
        //Remove trade from manager,owners and storage
        trades.remove(toRemove.getUuid());
        tradeServerOwners.remove(toRemove.getUuid());
        plugin.getDataStorage().removeTradeBackup(toRemove.getUuid());
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

              removeTrade(tradeFound);

              traderViewers.forEach(player -> openWindow(trade, player, true));
              customerViewers.forEach(player -> openWindow(trade, player, true));
          });
        setTradeServerOwner(trade.getUuid(), serverId);
        trades.put(trade.getUuid(), trade);

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
