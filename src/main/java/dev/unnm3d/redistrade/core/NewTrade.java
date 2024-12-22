package dev.unnm3d.redistrade.core;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.GuiSettings;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.data.Database;
import dev.unnm3d.redistrade.data.ICacheData;
import dev.unnm3d.redistrade.data.RedisDataManager;
import dev.unnm3d.redistrade.guis.TradeGuiImpl;
import dev.unnm3d.redistrade.utils.Utils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.gui.structure.Structure;
import xyz.xenondevs.invui.inventory.event.ItemPreUpdateEvent;
import xyz.xenondevs.invui.inventory.event.PlayerUpdateReason;
import xyz.xenondevs.invui.window.Window;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

@Getter
@EqualsAndHashCode
@ToString
public class NewTrade {

    private final ICacheData dataCacheManager;
    private CompletionTimer completionTimer;

    private final UUID uuid;
    private final TradeSide traderSide;
    private final TradeSide otherSide;

    private TradeGuiImpl traderGui;
    private TradeGuiImpl otherGui;


    public NewTrade(ICacheData dataCacheManager, UUID traderUUID, UUID targetUUID, String traderName, String targetName) {
        this(dataCacheManager, UUID.randomUUID(), new TradeSide(traderUUID, traderName, new OrderInfo(20)),
                new TradeSide(targetUUID, targetName, new OrderInfo(20)));
    }

    public NewTrade(ICacheData dataCacheManager, UUID uuid, TradeSide traderSide, TradeSide otherSide) {
        this.dataCacheManager = dataCacheManager;
        this.completionTimer = null;
        this.uuid = uuid;
        this.traderSide = traderSide;
        this.otherSide = otherSide;
    }

    public void initializeGuis() {
        this.traderSide.getOrder().getVirtualInventory().setPreUpdateHandler(event -> {
            if (virtualInventoryListener(event, true)) {
                event.setCancelled(true);
            } else {
                updateTraderItem(event.getSlot(), event.getNewItem(), true);
            }
        });
        this.traderSide.getOrder().getVirtualInventory().setPostUpdateHandler(event -> retrievedPhase(true, false));

        this.otherSide.getOrder().getVirtualInventory().setPreUpdateHandler(event -> {
            if (virtualInventoryListener(event, false)) {
                event.setCancelled(true);
            } else {
                updateTargetItem(event.getSlot(), event.getNewItem(), true);
            }
        });
        this.otherSide.getOrder().getVirtualInventory().setPostUpdateHandler(event -> retrievedPhase(false, true));

        if (Settings.instance().debug) {
            RedisTrade.getInstance().getLogger()
                    .info("VirtualInventory trader hash: " + traderSide.getOrder().getVirtualInventory().hashCode());
            RedisTrade.getInstance().getLogger()
                    .info("VirtualInventory target hash: " + otherSide.getOrder().getVirtualInventory().hashCode());
            RedisTrade.getInstance().getLogger()
                    .info("OrderInfo trader: " + traderSide.getOrder());
            RedisTrade.getInstance().getLogger()
                    .info("OrderInfo target: " + otherSide.getOrder());
        }

        this.traderGui = new TradeGuiImpl(this, true,
                new Structure(GuiSettings.instance().tradeGuiStructure.toArray(new String[0])));
        this.otherGui = new TradeGuiImpl(this, false,
                new Structure(GuiSettings.instance().tradeGuiStructure.toArray(new String[0])));
    }


    //GETTERS
    public boolean isTrader(UUID playerUUID) {
        return traderSide.getTraderUUID().equals(playerUUID);
    }

    public boolean isTarget(UUID playerUUID) {
        return otherSide.getTraderUUID().equals(playerUUID);
    }

    public OrderInfo getOrderInfo(boolean isTrader) {
        return isTrader ? traderSide.getOrder() : otherSide.getOrder();
    }

    public TradeGuiImpl getGui(boolean isTrader) {
        return isTrader ? traderGui : otherGui;
    }

    /**
     * Set the price of the trade
     *
     * @param price    The price to set
     * @param isTrader If the price is for the trader side
     */
    public void setPrice(String currencyName, double price, boolean isTrader) {
        getOrderInfo(isTrader).setPrice(currencyName, price);
        traderGui.notifyMoneyButton(isTrader);
        otherGui.notifyMoneyButton(isTrader);
    }


    /**
     * Set the price of the trade and send the update to the database/cache
     *
     * @param price    The price to set
     * @param isTrader If the price is for the trader side
     */
    public void setAndSendPrice(String currencyName, double price, boolean isTrader) {
        setPrice(currencyName, price, isTrader);
        dataCacheManager.updateTrade(uuid,
                isTrader ? RedisDataManager.TradeUpdateType.TRADER_MONEY :
                        RedisDataManager.TradeUpdateType.TARGET_MONEY,
                currencyName + ":" + price);
    }

    /**
     * Set the status of the trade
     *
     * @param status   The status to set
     * @param isTrader If the status is for the trader side
     */
    public void setStatus(OrderInfo.Status status, boolean isTrader) {
        getOrderInfo(isTrader).setStatus(status);
        //Notify the confirm button on the opposite side and the current side
        traderGui.notifyConfirm(isTrader);
        otherGui.notifyConfirm(isTrader);
        confirmPhase();

        //If you receive a remote retrieved status, try to finish and delete the trade
        if (status == OrderInfo.Status.RETRIEVED) {
            RedisTrade.getInstance().getTradeManager().finishTrade(isTrader ? otherSide.getTraderUUID() : traderSide.getTraderUUID());
        }

    }

    /**
     * Set the status of the trade and send the update to the database/cache
     *
     * @param newStatus      The status to set
     * @param previousStatus The previous status
     * @param isTrader       If the status is for the trader side
     * @return The new status
     */
    public CompletionStage<OrderInfo.Status> changeAndSendStatus(OrderInfo.Status newStatus, OrderInfo.Status previousStatus, boolean isTrader) {
        if (newStatus == previousStatus) return CompletableFuture.completedFuture(newStatus);
        return dataCacheManager.updateTrade(uuid, isTrader ? RedisDataManager.TradeUpdateType.TRADER_STATUS : RedisDataManager.TradeUpdateType.TARGET_STATUS, newStatus.getStatusByte())
                .thenApply(aLong -> {
                    if (aLong != -1) {
                        setStatus(newStatus, isTrader);
                        return newStatus;
                    } else {
                        setStatus(previousStatus, isTrader);
                        return previousStatus;
                    }
                });
    }

    public void updateTraderItem(int slot, ItemStack item, boolean sendUpdate) {
        if (sendUpdate) {
            dataCacheManager.updateTrade(uuid, RedisDataManager.TradeUpdateType.TRADER_ITEM, slot + "§;" + Utils.serialize(item));
        } else {
            traderSide.getOrder().getVirtualInventory().setItemSilently(slot, item);
        }
    }

    public void updateTargetItem(int slot, ItemStack item, boolean sendUpdate) {
        if (sendUpdate) {
            dataCacheManager.updateTrade(uuid, RedisDataManager.TradeUpdateType.TARGET_ITEM, slot + "§;" + Utils.serialize(item));
        } else {
            otherSide.getOrder().getVirtualInventory().setItemSilently(slot, item);
        }
    }

    /**
     * Called when a status is changed
     * Starts the completion timer if both are confirmed
     * Terminates the completion timer if one is refused
     */
    public void confirmPhase() {
        if (traderSide.getOrder().getStatus() == OrderInfo.Status.CONFIRMED && otherSide.getOrder().getStatus() == OrderInfo.Status.CONFIRMED) {
            if (this.completionTimer == null || this.completionTimer.isCancelled()) {
                this.completionTimer = new CompletionTimer(this);
                this.completionTimer.runTask(RedisTrade.getInstance(), traderSide.getTraderUUID(), otherSide.getTraderUUID());
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
                otherSide.getOrder().getPrices().forEach((currency, price) -> {
                    RedisTrade.getInstance().getEconomyHook()
                            .depositPlayer(traderSide.getTraderUUID(), price, currency, "Trade completion");
                });
                traderSide.getOrder().getPrices().forEach((currency, price) -> {
                    RedisTrade.getInstance().getEconomyHook()
                            .depositPlayer(otherSide.getTraderUUID(), price, currency, "Trade completion");
                });
                retrievedPhase(true, false);
                retrievedPhase(false, true);
                //Archive the completed trade
                if (RedisTrade.getInstance().getDataStorage() instanceof Database database) {
                    database.archiveTrade(this);
                }
            }
        };
        //Check if both sides are confirmed
        changeAndSendStatus(OrderInfo.Status.COMPLETED, traderSide.getOrder().getStatus(), true)
                .thenAcceptBothAsync(
                        changeAndSendStatus(OrderInfo.Status.COMPLETED, otherSide.getOrder().getStatus(), false),
                        finallyConsumer);
    }

    /**
     * Check if an inventory is empty and the status is completed
     * Then it resets the trade for the player and locks the whole GUI
     *
     * @param traderSide If the operating side is the trader side
     * @param isTrader   If the player who is editing the inventory is the trader
     */
    public void retrievedPhase(boolean traderSide, boolean isTrader) {
        final TradeSide operatingSide = traderSide ? this.traderSide : this.otherSide;

        //If the inventory is empty and the player is editing the opposite side
        if ((operatingSide.getOrder().getStatus() == OrderInfo.Status.REFUSED && traderSide == isTrader) ||
                operatingSide.getOrder().getStatus() == OrderInfo.Status.COMPLETED) {
            if (operatingSide.getOrder().getVirtualInventory().isEmpty()) {
                changeAndSendStatus(OrderInfo.Status.RETRIEVED, operatingSide.getOrder().getStatus(), traderSide)
                        .thenAccept(status -> {
                            if (status == OrderInfo.Status.RETRIEVED) {
                                RedisTrade.getInstance().getTradeManager().finishTrade(
                                        isTrader ? this.traderSide.getTraderUUID() : otherSide.getTraderUUID());
                            }
                        });
            }
        }
    }

    /**
     * This method is called when an item is updated in the virtual inventory
     *
     * @param event    The event that triggered the update
     * @param isTrader If the inventory to check is the trader side
     * @return If the event should be cancelled
     */
    public boolean virtualInventoryListener(ItemPreUpdateEvent event, boolean isTrader) {
        if (!(event.getUpdateReason() instanceof PlayerUpdateReason playerUpdateReason)) return false;
        final UUID editingPlayer = playerUpdateReason.getPlayer().getUniqueId();
        if (RedisTrade.getInstance().getIntegritySystem().isFaulted()) {
            playerUpdateReason.getPlayer().sendRichMessage(Messages.instance().newTradesLock);
            return true;
        }

        if (event.getNewItem() != null)
            for (Settings.BlacklistedItem blacklistedItem : Settings.instance().blacklistedItems) {
                if (blacklistedItem.isSimilar(event.getNewItem())) {
                    playerUpdateReason.getPlayer().sendRichMessage(Messages.instance().blacklistedItem);
                    return true;
                }
            }

        if (isTrader) {
            //If the trade is completed, the target can modify the trader inventory
            return switch (traderSide.getOrder().getStatus()) {
                case COMPLETED -> !editingPlayer.equals(otherSide.getTraderUUID());
                case CONFIRMED, RETRIEVED -> true;
                //If the trade is not completed, the trader can modify the trader inventory
                case REFUSED -> !editingPlayer.equals(traderSide.getTraderUUID());
            };
        }
        return switch (otherSide.getOrder().getStatus()) {
            case COMPLETED -> !editingPlayer.equals(traderSide.getTraderUUID());
            case CONFIRMED, RETRIEVED -> true;
            //If the trade is not completed, the target can modify the trader inventory
            case REFUSED -> !editingPlayer.equals(otherSide.getTraderUUID());
        };
    }

    public boolean openWindow(UUID playerUUID, boolean isTrader) {
        Optional<? extends Player> optionalPlayer = Optional.ofNullable(RedisTrade.getInstance().getServer().getPlayer(playerUUID));
        if (optionalPlayer.isPresent()) {
            Window.single()
                    .setTitle(GuiSettings.instance().tradeGuiTitle.replace("%player%", isTrader ? otherSide.getTraderName() : traderSide.getTraderName()))
                    .setGui(getGui(isTrader))
                    .addCloseHandler(() -> {
                        OrderInfo traderOrder = getOrderInfo(isTrader);
                        if (traderOrder.getStatus() != OrderInfo.Status.RETRIEVED) {
                            optionalPlayer.get().sendRichMessage(Messages.instance().tradeRunning
                                    .replace("%player%", isTrader ? otherSide.getTraderName() : traderSide.getTraderName()));
                        }
                    })
                    .open(optionalPlayer.get());
            return true;
        }
        return false;
    }

    public byte[] serialize() {
        if (Settings.instance().debug) {
            RedisTrade.getInstance().getLogger()
                    .info("Serializing OrderInfo trader: " + traderSide.getOrder());
            RedisTrade.getInstance().getLogger()
                    .info("Serializing OrderInfo target: " + otherSide.getOrder());
        }
        byte[] traderSide = this.traderSide.serialize();
        byte[] otherSide = this.otherSide.serialize();

        //Allocate bytes for TradeUUID, TraderSideSize, OtherSideSize, TraderSide, OtherSide
        ByteBuffer bb = ByteBuffer.allocate(16 + 4 + 4 + traderSide.length + otherSide.length);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());

        bb.putInt(traderSide.length);
        bb.putInt(otherSide.length);

        bb.put(traderSide);
        bb.put(otherSide);

        return bb.array();
    }

    public static NewTrade deserialize(ICacheData dataManager, byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        UUID uuid = new UUID(bb.getLong(), bb.getLong());
        int traderSideSize = bb.getInt();
        int otherSideSize = bb.getInt();

        byte[] traderSideData = new byte[traderSideSize];
        byte[] otherSideData = new byte[otherSideSize];
        bb.get(traderSideData);
        bb.get(otherSideData);

        return new NewTrade(dataManager, uuid, TradeSide.deserialize(traderSideData), TradeSide.deserialize(otherSideData));
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