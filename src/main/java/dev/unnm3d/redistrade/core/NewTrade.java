package dev.unnm3d.redistrade.core;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.GuiSettings;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.data.Database;
import dev.unnm3d.redistrade.data.TradeUpdateType;
import dev.unnm3d.redistrade.guis.ViewerType;
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

    public ViewerType getViewerType(UUID playerUUID) {
        if (traderSide.getTraderUUID().equals(playerUUID)) return ViewerType.TRADER;
        if (customerSide.getTraderUUID().equals(playerUUID)) return ViewerType.CUSTOMER;
        return ViewerType.SPECTATOR;
    }

    public TradeSide getTradeSide(ViewerType viewerType) {
        return switch (viewerType) {
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

    public OrderInfo getOrderInfo(ViewerType viewerType) {
        return switch (viewerType) {
            case TRADER -> traderSide.getOrder();
            case CUSTOMER -> customerSide.getOrder();
            case SPECTATOR -> null;
        };
    }

    /**
     * Set the price of the trade
     *
     * @param price      The price to set
     * @param viewerType If the price is for the trader side
     */
    public void setPrice(String currencyName, double price, ViewerType viewerType) {
        getTradeSide(viewerType).setPrice(currencyName, price);
        getTradeSide(viewerType).notifyOppositePrice();
        getTradeSide(viewerType.opposite()).notifyOppositePrice();
    }


    /**
     * Set the price of the trade and send the update to the database/cache
     *
     * @param price      The price to set
     * @param viewerType If the price is for the trader side
     */
    public void setAndSendPrice(String currencyName, double price, ViewerType viewerType) {
        setPrice(currencyName, price, viewerType);
        final TradeUpdateType updateType = TradeUpdateType.valueOf(viewerType, TradeUpdateType.UpdateType.PRICE);
        RedisTrade.getInstance().getDataCache().updateTrade(uuid,
                updateType,
                currencyName + ":" + price);
    }

    /**
     * Set the status of the trade
     *
     * @param status     The status to set
     * @param viewerType If the status is for the trader side
     */
    public void setStatus(OrderInfo.Status status, ViewerType viewerType) {
        getTradeSide(viewerType).setStatus(status);
        //Notify the confirm button on the opposite side and the current side
        getTradeSide(viewerType.opposite()).notifyOppositeStatus();
        confirmPhase();

        //If you receive a remote retrieved status, try to finish and delete the trade
        if (status == OrderInfo.Status.RETRIEVED) {
            RedisTrade.getInstance().getTradeManager().finishTrade(getTradeSide(viewerType.opposite()).getTraderUUID());
        }
    }

    /**
     * Set the status of the trade and send the update to the database/cache
     *
     * @param newStatus      The status to set
     * @param previousStatus The previous status
     * @param viewerType     If the status is for the trader side
     * @return The new status
     */
    public CompletionStage<OrderInfo.Status> changeAndSendStatus(OrderInfo.Status newStatus, OrderInfo.Status previousStatus, ViewerType viewerType) {
        if (newStatus == previousStatus) return CompletableFuture.completedFuture(newStatus);
        TradeUpdateType updateType = TradeUpdateType
                .valueOf(viewerType, TradeUpdateType.UpdateType.STATUS);
        return RedisTrade.getInstance().getDataCache().updateTrade(uuid, updateType, newStatus.getStatusByte())
                .thenApply(aLong -> {
                    if (aLong != -1) {
                        setStatus(newStatus, viewerType);
                        return newStatus;
                    } else {
                        setStatus(previousStatus, viewerType);
                        return previousStatus;
                    }
                });
    }

    public void setOpened(boolean opened, ViewerType viewerType) {
        getTradeSide(viewerType).setOpened(opened);
    }

    public void updateItem(int slot, ItemStack item, ViewerType viewerType, boolean sendUpdate) {
        final TradeUpdateType updateType = TradeUpdateType.valueOf(viewerType, TradeUpdateType.UpdateType.ITEM);
        if (sendUpdate) {
            RedisTrade.getInstance().getDataCache().updateTrade(uuid, updateType, slot + "§;" + Utils.serialize(item));
        } else {
            getTradeSide(viewerType).getOrder().getVirtualInventory().setItemSilently(slot, item);
        }
    }

    /**
     * Called when a status is changed
     * Starts the completion timer if both are confirmed
     * Terminates the completion timer if one is refused
     */
    public void confirmPhase() {
        if (traderSide.getOrder().getStatus() == OrderInfo.Status.CONFIRMED && customerSide.getOrder().getStatus() == OrderInfo.Status.CONFIRMED) {
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
        final BiConsumer<OrderInfo.Status, OrderInfo.Status> finallyConsumer = (status1, status2) -> {
            if (status1 == OrderInfo.Status.COMPLETED && status2 == OrderInfo.Status.COMPLETED) {
                if (RedisTrade.getInstance().getTradeManager().isOwner(uuid)) {
                    for (Map.Entry<String, Double> currencyPrice : customerSide.getOrder().getPrices().entrySet()) {
                        RedisTrade.getInstance().getEconomyHook()
                                .depositPlayer(traderSide.getTraderUUID(), currencyPrice.getValue(), currencyPrice.getKey(), "Trade completion");
                        if (Settings.instance().debug) {
                            RedisTrade.getInstance().getLogger().info("Depositing trader " + currencyPrice.getValue() + " " + currencyPrice.getKey() + " to " + traderSide.getTraderName());
                        }
                    }
                    for (Map.Entry<String, Double> currencyPrice : traderSide.getOrder().getPrices().entrySet()) {
                        RedisTrade.getInstance().getEconomyHook()
                                .depositPlayer(customerSide.getTraderUUID(), currencyPrice.getValue(), currencyPrice.getKey(), "Trade completion");
                        if (Settings.instance().debug) {
                            RedisTrade.getInstance().getLogger().info("Depositing customer " + currencyPrice.getValue() + " " + currencyPrice.getKey() + " to " + traderSide.getTraderName());
                        }
                    }
                }
                retrievedPhase(ViewerType.TRADER, ViewerType.CUSTOMER);
                retrievedPhase(ViewerType.CUSTOMER, ViewerType.TRADER);
                //Archive the completed trade
                if (RedisTrade.getInstance().getDataStorage() instanceof Database database) {
                    database.archiveTrade(this);
                }
            }
        };
        //Check if both sides are confirmed
        changeAndSendStatus(OrderInfo.Status.COMPLETED, traderSide.getOrder().getStatus(), ViewerType.TRADER)
                .thenAcceptBothAsync(
                        changeAndSendStatus(OrderInfo.Status.COMPLETED, customerSide.getOrder().getStatus(), ViewerType.CUSTOMER),
                        finallyConsumer);
    }

    /**
     * Check if an inventory is empty and the status is completed
     * Then it resets the trade for the player and locks the whole GUI
     *
     * @param tradeSideType The side of the player
     * @param whoIsEditing  The side of the player that is editing
     * @return If the phase was successful
     */
    public CompletionStage<Boolean> retrievedPhase(ViewerType tradeSideType, ViewerType whoIsEditing) {
        TradeSide operatingSide = getTradeSide(tradeSideType);
        //If the inventory is empty and the player is editing the opposite side
        if ((operatingSide.getOrder().getStatus() == OrderInfo.Status.REFUSED && tradeSideType == whoIsEditing) ||
                operatingSide.getOrder().getStatus() == OrderInfo.Status.COMPLETED) {
            if (operatingSide.getOrder().getVirtualInventory().isEmpty()) {
                return changeAndSendStatus(OrderInfo.Status.RETRIEVED, operatingSide.getOrder().getStatus(), tradeSideType)
                        .thenApply(status -> status == OrderInfo.Status.RETRIEVED);
            }
        }
        return CompletableFuture.completedFuture(false);
    }

    public boolean openWindow(UUID playerUUID, ViewerType viewerType) {
        Optional<? extends Player> optionalPlayer = Optional.ofNullable(RedisTrade.getInstance().getServer().getPlayer(playerUUID));
        if (optionalPlayer.isPresent()) {
            Window.single()
                    .setTitle(GuiSettings.instance().tradeGuiTitle.replace("%player%",
                            getTradeSide(viewerType.opposite()).getTraderName()))
                    .setGui(getTradeSide(viewerType).getSidePerspective())
                    .addCloseHandler(() -> {
                        final OrderInfo traderOrder = getTradeSide(viewerType).getOrder();
                        if (traderOrder.getStatus() != OrderInfo.Status.RETRIEVED) {
                            optionalPlayer.get().sendRichMessage(Messages.instance().tradeRunning
                                    .replace("%player%", getTradeSide(viewerType.opposite()).getTraderName()));
                        }
                    })
                    .open(optionalPlayer.get());
            setOpened(true, viewerType);
            return true;
        }
        return false;
    }

    public byte[] serialize() {
        if (Settings.instance().debug) {
            RedisTrade.getInstance().getLogger()
                    .info("Serializing OrderInfo trader: " + traderSide.getOrder());
            RedisTrade.getInstance().getLogger()
                    .info("Serializing OrderInfo target: " + customerSide.getOrder());
        }
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

        return new NewTrade(uuid, TradeSide.deserialize(traderSideData), TradeSide.deserialize(customerSideData));
    }

}
//This is a XOR operation
// isTrader selfRetrieve
// 1 1 -> 1
// 1 0 -> 0
// 0 1 -> 0
// 0 0 -> 1
// where 1 is the trader uuid and 0 is the other uuid
// trader is editing trader side

// status is completed then drop
// status is not completed then forward

// target is editing target side
//same

// trader is editing target side

// status is completed then check if empty and set RETRIEVED IN TARGET SIDE
// and remove target from playerTrades

// target is editing trader side

// status is completed then check if empty and set RETRIEVED IN TRADER SIDE
// and remove target from playerTrades