package dev.unnm3d.redistrade.core;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.data.Database;
import dev.unnm3d.redistrade.data.ICacheData;
import dev.unnm3d.redistrade.data.RedisDataManager;
import dev.unnm3d.redistrade.guis.MoneySelectorGUI;
import dev.unnm3d.redistrade.guis.TradeGuiImpl;
import dev.unnm3d.redistrade.utils.Utils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.inventory.event.ItemPreUpdateEvent;
import xyz.xenondevs.invui.inventory.event.PlayerUpdateReason;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SuppliedItem;
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
        this.otherSide.getOrder().getVirtualInventory().setPostUpdateHandler(event -> retrievePhase(true, false));

        this.otherSide.getOrder().getVirtualInventory().setPreUpdateHandler(event -> {
            if (virtualInventoryListener(event, false)) {
                event.setCancelled(true);
            } else {
                updateTargetItem(event.getSlot(), event.getNewItem(), true);
            }
        });
        this.otherSide.getOrder().getVirtualInventory().setPostUpdateHandler(event -> retrievePhase(false, true));

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

        this.traderGui = createTraderGui();
        this.otherGui = createTargetGui();
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


    /**
     * Set the price of the trade
     *
     * @param price    The price to set
     * @param isTrader If the price is for the trader side
     */
    public void setPrice(double price, boolean isTrader) {
        OrderInfo order = isTrader ? traderSide.getOrder() : otherSide.getOrder();
        order.setProposed(price);
        notifyItem(isTrader ? 1 : 7, 0, true);
        notifyItem(isTrader ? 7 : 1, 0, false);
    }

    /**
     * Set the price of the trade and send the update to the database/cache
     *
     * @param price    The price to set
     * @param isTrader If the price is for the trader side
     */
    public void setAndSendPrice(double price, boolean isTrader) {
        setPrice(price, isTrader);
        dataCacheManager.updateTrade(uuid,
                isTrader ? RedisDataManager.TradeUpdateType.TRADER_MONEY :
                        RedisDataManager.TradeUpdateType.TARGET_MONEY,
                price);

    }

    /**
     * Set the status of the trade
     *
     * @param status   The status to set
     * @param isTrader If the status is for the trader side
     */
    public void setStatus(OrderInfo.Status status, boolean isTrader) {
        OrderInfo order = isTrader ? traderSide.getOrder() : otherSide.getOrder();
        order.setStatus(status);
        //Notify the confirm button on the opposite side and the current side
        notifyItem(isTrader ? 0 : 8, 0, true);
        notifyItem(isTrader ? 8 : 0, 0, false);
        confirmPhase();

        if(status == OrderInfo.Status.RETRIEVED){
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
     * Terminates the completion timer if one is refuted
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
                RedisTrade.getInstance().getEconomyHook()
                        .depositPlayer(traderSide.getTraderUUID(), otherSide.getOrder().getProposed(),
                                "default", "Trade price");
                RedisTrade.getInstance().getEconomyHook()
                        .depositPlayer(otherSide.getTraderUUID(), traderSide.getOrder().getProposed(),
                                "default", "Trade price");
                retrievePhase(true, false);
                retrievePhase(false, true);
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
    public void retrievePhase(boolean traderSide, boolean isTrader) {
        final TradeSide operatingSide = traderSide ? this.traderSide : this.otherSide;

        //If the inventory is empty and the player is editing the opposite side
        if ((operatingSide.getOrder().getStatus() == OrderInfo.Status.REFUTED && traderSide == isTrader) ||
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
        if (isTrader) {
            //If the trade is completed, the target can modify the trader inventory
            return switch (traderSide.getOrder().getStatus()) {
                case COMPLETED -> !editingPlayer.equals(otherSide.getTraderUUID());
                case CONFIRMED, RETRIEVED -> true;
                //If the trade is not completed, the trader can modify the trader inventory
                case REFUTED -> !editingPlayer.equals(traderSide.getTraderUUID());
            };
        }
        return switch (otherSide.getOrder().getStatus()) {
            case COMPLETED -> !editingPlayer.equals(traderSide.getTraderUUID());
            case CONFIRMED, RETRIEVED -> true;
            //If the trade is not completed, the target can modify the trader inventory
            case REFUTED -> !editingPlayer.equals(otherSide.getTraderUUID());
        };
    }

    public boolean openWindow(UUID playerUUID, boolean isTrader) {
        Optional<? extends Player> optionalPlayer = Optional.ofNullable(RedisTrade.getInstance().getServer().getPlayer(playerUUID));
        if (optionalPlayer.isPresent()) {
            Window.single()
                    .setTitle(Settings.instance().tradeGuiTitle.replace("%player%", isTrader ? otherSide.getTraderName() : traderSide.getTraderName()))
                    .setGui(isTrader ? traderGui : otherGui)
                    .open(optionalPlayer.get());
            return true;
        }
        return false;
    }

    public TradeGuiImpl createTraderGui() {
        return new TradeGuiImpl.Builder()
                .setStructure(Settings.instance().tradeGuiStructure.toArray(new String[0]))
                .addIngredient('L', getTraderSide().getOrder().getVirtualInventory())
                .addIngredient('R', getOtherSide().getOrder().getVirtualInventory())
                .addIngredient('C', getTraderConfirmButton())
                .addIngredient('M', getMoneyButton(true, true))
                .addIngredient('m', getMoneyButton(false, false))
                .addIngredient('c', getTargetConfirmButton())
                .addIngredient('D', getTradeCancelButton(true))
                .addIngredient('x', Settings.instance().getButton(Settings.ButtonType.SEPARATOR))
                .build();
    }

    public TradeGuiImpl createTargetGui() {
        return new TradeGuiImpl.Builder()
                .setStructure(Settings.instance().tradeGuiStructure.toArray(new String[0]))
                .addIngredient('L', getOtherSide().getOrder().getVirtualInventory())
                .addIngredient('R', getTraderSide().getOrder().getVirtualInventory())
                .addIngredient('C', getTargetConfirmButton())
                .addIngredient('M', getMoneyButton(false, true))
                .addIngredient('m', getMoneyButton(true, false))
                .addIngredient('c', getTraderConfirmButton())
                .addIngredient('D', getTradeCancelButton(false))
                .addIngredient('x', Settings.instance().getButton(Settings.ButtonType.SEPARATOR))
                .build();
    }

    /**
     * Get the money button for the trade GUI
     *
     * @param isTrader   If the button links to the trader money
     * @param playerSide If the button is for the right side or the left side
     * @return The item for the money button
     */
    public Item getMoneyButton(boolean isTrader, boolean playerSide) {
        final OrderInfo order = isTrader ? traderSide.getOrder() : otherSide.getOrder();
        final String displayName = playerSide ? Messages.instance().playerMoneyDisplay : Messages.instance().otherPlayerMoneyDisplay;

        return new AbstractItem() {
            @Override
            public ItemProvider getItemProvider() {
                return new ItemBuilder(Settings.instance().getButton(Settings.ButtonType.MONEY_BUTTON))
                        .setDisplayName(displayName.replace("%amount%", Utils.parseDoubleFormat(order.getProposed())));
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                if (order.getStatus() == OrderInfo.Status.REFUTED)
                    new MoneySelectorGUI(NewTrade.this, isTrader, order.getProposed(), (Player) event.getWhoClicked());
            }
        };
    }

    public Item getTraderConfirmButton() {
        return new SuppliedItem(() ->
                switch (traderSide.getOrder().getStatus()) {
                    case REFUTED -> new ItemBuilder(Settings.instance().getButton(Settings.ButtonType.REFUTE_BUTTON));
                    case CONFIRMED ->
                            new ItemBuilder(Settings.instance().getButton(Settings.ButtonType.CONFIRM_BUTTON));
                    case COMPLETED ->
                            new ItemBuilder(Settings.instance().getButton(Settings.ButtonType.COMPLETED_BUTTON));
                    case RETRIEVED ->
                            new ItemBuilder(Settings.instance().getButton(Settings.ButtonType.RETRIEVED_BUTTON));
                }, (inventoryClickEvent) -> {

            if (traderSide.getOrder().getStatus() == OrderInfo.Status.COMPLETED ||
                    traderSide.getOrder().getStatus() == OrderInfo.Status.RETRIEVED) return false;
            OrderInfo.Status newStatus;
            if (traderSide.getOrder().getStatus() == OrderInfo.Status.REFUTED) {
                newStatus = OrderInfo.Status.CONFIRMED;
            } else {
                newStatus = OrderInfo.Status.REFUTED;
            }
            changeAndSendStatus(newStatus, traderSide.getOrder().getStatus(), true);
            return true;
        });
    }

    public Item getTradeCancelButton(boolean isTrader) {
        return new AbstractItem() {
            @Override
            public ItemProvider getItemProvider() {
                return new ItemBuilder(Settings.instance().getButton(Settings.ButtonType.CANCEL_TRADE_BUTTON));
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                retrievePhase(false, false);
                retrievePhase(true, true);
                RedisTrade.getInstance().getEconomyHook().depositPlayer(player.getUniqueId(),
                        isTrader ? traderSide.getOrder().getProposed() : otherSide.getOrder().getProposed(),
                        "default",
                        "Trade price");
                setAndSendPrice(0, isTrader);

            }
        };
    }

    public Item getTargetConfirmButton() {
        return new SuppliedItem(() ->
                switch (otherSide.getOrder().getStatus()) {
                    case REFUTED -> new ItemBuilder(Settings.instance().getButton(Settings.ButtonType.REFUTE_BUTTON));
                    case CONFIRMED ->
                            new ItemBuilder(Settings.instance().getButton(Settings.ButtonType.CONFIRM_BUTTON));
                    case COMPLETED ->
                            new ItemBuilder(Settings.instance().getButton(Settings.ButtonType.COMPLETED_BUTTON));
                    case RETRIEVED ->
                            new ItemBuilder(Settings.instance().getButton(Settings.ButtonType.RETRIEVED_BUTTON));
                }, (inventoryClickEvent) -> {

            if (traderSide.getOrder().getStatus() == OrderInfo.Status.COMPLETED ||
                    traderSide.getOrder().getStatus() == OrderInfo.Status.RETRIEVED) return false;
            OrderInfo.Status newStatus;
            if (otherSide.getOrder().getStatus() == OrderInfo.Status.REFUTED) {
                newStatus = OrderInfo.Status.CONFIRMED;
            } else {
                newStatus = OrderInfo.Status.REFUTED;
            }
            changeAndSendStatus(newStatus, otherSide.getOrder().getStatus(), false);
            return true;
        });
    }

    public void notifyItem(int x, int y, boolean isTrader) {
        Item item = (isTrader ? traderGui : otherGui).getItem(x, y);
        if (item != null) {
            item.notifyWindows();
        }
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