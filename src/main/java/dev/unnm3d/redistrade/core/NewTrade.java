package dev.unnm3d.redistrade.core;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.GuiSettings;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.enums.*;
import dev.unnm3d.redistrade.data.Database;
import dev.unnm3d.redistrade.guis.ReceiptButton;
import dev.unnm3d.redistrade.utils.Utils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.window.Window;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

@Getter
@EqualsAndHashCode
@ToString
public class NewTrade {
    private CompletionTimer completionTimer;

    private final UUID uuid;
    private final TradeSide traderSide;
    private final TradeSide customerSide;


    public NewTrade(UUID traderUUID, UUID targetUUID, String traderName, String targetName) {
        this(UUID.randomUUID(), new TradeSide(traderUUID, traderName, new OrderInfo(20), true),
                new TradeSide(targetUUID, targetName, new OrderInfo(20), false));
    }

    public NewTrade(UUID uuid, TradeSide traderSide, TradeSide customerSide) {
        this.completionTimer = null;
        this.uuid = uuid;
        this.traderSide = traderSide;
        this.customerSide = customerSide;
    }

    public Actor getViewerType(UUID playerUUID) {
        if (traderSide.getTraderUUID().equals(playerUUID)) return Actor.TRADER;
        if (customerSide.getTraderUUID().equals(playerUUID)) return Actor.CUSTOMER;
        return Actor.SPECTATOR;
    }

    public TradeSide getTradeSide(Actor actor) {
        return switch (actor) {
            case TRADER -> traderSide;
            case CUSTOMER -> customerSide;
            case SPECTATOR -> null;
        };
    }

    //GETTERS
    public boolean isTrader(UUID playerUUID) {
        return traderSide.getTraderUUID().equals(playerUUID);
    }

    public boolean isTarget(UUID playerUUID) {
        return customerSide.getTraderUUID().equals(playerUUID);
    }

    public OrderInfo getOrderInfo(Actor actor) {
        return switch (actor) {
            case TRADER -> traderSide.getOrder();
            case CUSTOMER -> customerSide.getOrder();
            case SPECTATOR -> null;
        };
    }

    /**
     * Set the price of the trade
     *
     * @param price The price to set
     * @param actor If the price is for the trader side
     */
    public void setPrice(String currencyName, double price, Actor actor) {
        getTradeSide(actor).setPrice(currencyName, price);
        getTradeSide(actor).notifyOppositePrice();
        getTradeSide(actor.opposite()).notifyOppositePrice();
        RedisTrade.debug(uuid + " Setting price to " + price + " for " + actor.name());
    }


    /**
     * Set the price of the trade and send the update to the database/cache
     *
     * @param price The price to set
     * @param actor If the price is for the trader side
     */
    public void setAndSendPrice(String currencyName, double price, Actor actor) {
        setPrice(currencyName, price, actor);
        final ViewerUpdate updateType = ViewerUpdate.valueOf(actor, UpdateType.PRICE);
        RedisTrade.getInstance().getDataCache().updateTrade(uuid,
                updateType,
                currencyName + ":" + price);
    }

    /**
     * Set the status of the trade
     *
     * @param status     The status to set
     * @param viewerSide The side of the trade to set the status for
     */
    public void setStatus(StatusActor status, Actor viewerSide) {
        getTradeSide(viewerSide).setStatus(status.getStatus());
        //Notify the confirm button on the opposite side and the current side
        getTradeSide(viewerSide.opposite()).notifyOppositeStatus();
        confirmPhase();

        RedisTrade.debug(uuid + " Setting status to " + status.getStatus().name() + " for " + viewerSide.name());
        //If you receive a remote retrieved status, try to finish and delete the trade
        if (status.getStatus() == Status.RETRIEVED) {
            RedisTrade.getInstance().getTradeManager().closeTrade(uuid, status.getViewerActor());
        }else if (status.getStatus() == Status.COMPLETED && Settings.instance().deliverReceipt) {
            getTradeSide(viewerSide).getSidePerspective().setItem(13, new ReceiptButton(this));
        }
    }

    /**
     * Set the status of the trade and send the update to the database/cache
     *
     * @param newStatus      The status to set
     * @param previousStatus The previous status
     * @param viewerSide     The side of the trade to set the status for
     * @return The new status
     */
    public CompletionStage<Status> changeAndSendStatus(StatusActor newStatus, Status previousStatus, Actor viewerSide) {
        if (newStatus.getStatus() == previousStatus) return CompletableFuture.completedFuture(newStatus.getStatus());
        final ViewerUpdate updateType = ViewerUpdate.valueOf(viewerSide, UpdateType.STATUS);
        return RedisTrade.getInstance().getDataCache().updateTrade(uuid, updateType, newStatus.toChar())
                .thenApply(aLong -> {
                    if (aLong != -1) {
                        setStatus(newStatus, viewerSide);
                        return newStatus.getStatus();
                    } else {
                        //If the update failed, revert the status without triggering the completion timer
                        getTradeSide(viewerSide).setStatus(previousStatus);
                        getTradeSide(viewerSide.opposite()).notifyOppositeStatus();
                        return previousStatus;
                    }
                });
    }

    public void setOpened(boolean opened, Actor actor) {
        getTradeSide(actor).setOpened(opened);
    }

    public void updateItem(int slot, ItemStack item, Actor actor, boolean sendUpdate) {
        final ViewerUpdate updateType = ViewerUpdate.valueOf(actor, UpdateType.ITEM);

        if (sendUpdate) {
            RedisTrade.getInstance().getDataCache().updateTrade(uuid, updateType, slot + "§;" + Utils.serialize(item));
            RedisTrade.debug(uuid + " Sending item " + item + " from " + actor.name());
        } else {
            getTradeSide(actor).getOrder().getVirtualInventory().setItemSilently(slot, item);
            RedisTrade.debug(uuid + " Updating item " + item + " from " + actor.name());
        }
    }

    /**
     * Called when a status is changed
     * Starts the completion timer if both are confirmed
     * Terminates the completion timer if one is refused
     */
    public void confirmPhase() {
        if (traderSide.getOrder().getStatus() == Status.CONFIRMED && customerSide.getOrder().getStatus() == Status.CONFIRMED) {
            if (this.completionTimer == null || this.completionTimer.isCancelled()) {
                this.completionTimer = new CompletionTimer(this);
                this.completionTimer.runTask(RedisTrade.getInstance(), traderSide.getTraderUUID(), customerSide.getTraderUUID());
            }
        } else if (this.completionTimer != null && !this.completionTimer.isCancelled()) {
            this.completionTimer.cancel();
        }
    }

    /**
     * This phase is called when the CompletionTimer is finished
     * It switches the sides of trader and target
     */
    public void completePhase() {
        this.completionTimer.cancel();
        final BiConsumer<Status, Status> finallyConsumer = (status1, status2) -> {
            if (status1 == Status.COMPLETED && status2 == Status.COMPLETED) {
                //Apply the economy changes only if the current server is the owner of the trade
                //Owner means the last server that modified the trade
                if (RedisTrade.getInstance().getTradeManager().isOwner(uuid)) {
                    for (Map.Entry<String, Double> currencyPrice : customerSide.getOrder().getPrices().entrySet()) {
                        RedisTrade.getInstance().getEconomyHook()
                                .depositPlayer(traderSide.getTraderUUID(), currencyPrice.getValue(), currencyPrice.getKey(), "Trade completion");
                        RedisTrade.debug(uuid + " Depositing trader " + currencyPrice.getValue() + " " + currencyPrice.getKey() + " to " + traderSide.getTraderName());
                    }
                    for (Map.Entry<String, Double> currencyPrice : traderSide.getOrder().getPrices().entrySet()) {
                        RedisTrade.getInstance().getEconomyHook()
                                .depositPlayer(customerSide.getTraderUUID(), currencyPrice.getValue(), currencyPrice.getKey(), "Trade completion");
                        RedisTrade.debug(uuid + " Depositing customer " + currencyPrice.getValue() + " " + currencyPrice.getKey() + " to " + customerSide.getTraderName());
                    }
                }
                retrievedPhase(Actor.TRADER, Actor.CUSTOMER);
                retrievedPhase(Actor.CUSTOMER, Actor.TRADER);
                //Archive the completed trade
                if (RedisTrade.getInstance().getDataStorage() instanceof Database database) {
                    database.archiveTrade(this);
                }
            }
        };
        //Check if both sides are confirmed

        changeAndSendStatus(StatusActor.valueOf(Actor.TRADER, Status.COMPLETED), traderSide.getOrder().getStatus(), Actor.TRADER)
                .thenAcceptBothAsync(
                        changeAndSendStatus(StatusActor.valueOf(Actor.CUSTOMER, Status.COMPLETED), customerSide.getOrder().getStatus(), Actor.CUSTOMER),
                        finallyConsumer);
    }

    /**
     * Check if an inventory is empty and the status is completed
     * Then it resets the trade for the player and locks the whole GUI
     *
     * @param tradeSide    The side of the player
     * @param whoIsEditing The side of the player that is editing
     * @return If the phase was successful
     */
    public CompletionStage<Boolean> retrievedPhase(Actor tradeSide, Actor whoIsEditing) {
        final TradeSide operatingSide = getTradeSide(tradeSide);

        if (operatingSide.getOrder().getVirtualInventory().isEmpty()) {
            final StatusActor statusActor = StatusActor.valueOf(whoIsEditing, Status.RETRIEVED);
            if (operatingSide.getOrder().getStatus() == Status.REFUSED && tradeSide == whoIsEditing) {
                return changeAndSendStatus(statusActor, operatingSide.getOrder().getStatus(), tradeSide)
                        .thenApply(returnedStatus -> returnedStatus == Status.RETRIEVED);

            } else if (operatingSide.getOrder().getStatus() == Status.COMPLETED) {
                //If the status is completed, set the status to retrieved
                return changeAndSendStatus(statusActor, operatingSide.getOrder().getStatus(), tradeSide)
                        .thenApply(returnedStatus -> returnedStatus == Status.RETRIEVED);
            }
        }
        return CompletableFuture.completedFuture(false);
    }

    public boolean openWindow(UUID playerUUID, Actor actor) {
        Optional<? extends Player> optionalPlayer = Optional.ofNullable(RedisTrade.getInstance().getServer().getPlayer(playerUUID));
        if (optionalPlayer.isPresent()) {
            Window.single()
                    .setTitle(GuiSettings.instance().tradeGuiTitle.replace("%player%",
                            getTradeSide(actor.opposite()).getTraderName()))
                    .setGui(getTradeSide(actor).getSidePerspective())
                    .addCloseHandler(() -> {
                        final OrderInfo traderOrder = getTradeSide(actor).getOrder();
                        if (traderOrder.getStatus() != Status.RETRIEVED) {
                            optionalPlayer.get().sendRichMessage(Messages.instance().tradeRunning
                                    .replace("%player%", getTradeSide(actor.opposite()).getTraderName()));
                        }
                    })
                    .open(optionalPlayer.get());

            return true;
        }
        return false;
    }

    public byte[] serialize() {
        RedisTrade.debug("Serializing " + uuid + " trader side: " + traderSide);
        RedisTrade.debug("Serializing " + uuid + " customer side: " + customerSide);

        byte[] traderSide = this.traderSide.serialize();
        byte[] customerSide = this.customerSide.serialize();

        //Allocate bytes for TradeUUID, TraderSideSize, OtherSideSize, TraderSide, OtherSide
        ByteBuffer bb = ByteBuffer.allocate(16 + 4 + 4 + traderSide.length + customerSide.length);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());

        bb.putInt(traderSide.length);
        bb.putInt(customerSide.length);

        bb.put(traderSide);
        bb.put(customerSide);

        return bb.array();
    }

    public static NewTrade deserialize(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        UUID uuid = new UUID(bb.getLong(), bb.getLong());
        int traderSideSize = bb.getInt();
        int customerSideSize = bb.getInt();

        byte[] traderSideData = new byte[traderSideSize];
        byte[] customerSideData = new byte[customerSideSize];
        bb.get(traderSideData);
        bb.get(customerSideData);
        final TradeSide traderSide = TradeSide.deserialize(traderSideData);
        final TradeSide customerSide = TradeSide.deserialize(customerSideData);

        RedisTrade.debug("Deserializing " + uuid + " trader side: " + traderSide);
        RedisTrade.debug("Deserializing " + uuid + " customer side: " + customerSide);
        return new NewTrade(uuid, traderSide, customerSide);
    }

}