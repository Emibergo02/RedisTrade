package dev.unnm3d.redistrade.objects;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.Settings;
import dev.unnm3d.redistrade.Utils;
import dev.unnm3d.redistrade.data.RedisDataManager;
import dev.unnm3d.redistrade.guis.MoneySelectorGUI;
import dev.unnm3d.redistrade.guis.OrderInfo;
import dev.unnm3d.redistrade.guis.TradeGuiImpl;
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
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Getter
@EqualsAndHashCode
@ToString
public class NewTrade {

    private final RedisDataManager dataManager;
    private CompletionTimer completionTimer;

    private final UUID uuid;
    private final UUID traderUUID;
    private final UUID targetUUID;
    private final String traderName;
    private final String targetName;
    private final OrderInfo traderSideInfo;
    private final OrderInfo targetSideInfo;

    private final TradeGuiImpl traderGui;
    private final TradeGuiImpl targetGui;


    public NewTrade(RedisDataManager dataManager, UUID traderUUID, UUID targetUUID, String traderName, String targetName) {
        this(dataManager, UUID.randomUUID(), traderUUID, targetUUID, traderName, targetName, new OrderInfo(20), new OrderInfo(20));
    }

    public NewTrade(RedisDataManager dataManager, UUID uuid, UUID traderUUID, UUID targetUUID, String traderName, String targetName, OrderInfo traderSideInfo, OrderInfo targetSideInfo) {
        this.dataManager = dataManager;
        this.completionTimer = null;

        this.uuid = uuid;
        this.traderUUID = traderUUID;
        this.targetUUID = targetUUID;
        this.traderName = traderName;
        this.targetName = targetName;
        this.traderSideInfo = traderSideInfo;
        this.targetSideInfo = targetSideInfo;


        traderSideInfo.getVirtualInventory().setPreUpdateHandler(event -> {
            if (virtualInventoryListener(event, true)) {
                event.setCancelled(true);
            } else {
                updateTraderItem(event.getSlot(), event.getNewItem(), true);
            }
        });
        traderSideInfo.getVirtualInventory().setPostUpdateHandler(event -> retrievePhase(true, false));

        targetSideInfo.getVirtualInventory().setPreUpdateHandler(event -> {
            if (virtualInventoryListener(event, false)) {
                event.setCancelled(true);
            } else {
                updateTargetItem(event.getSlot(), event.getNewItem(), true);
            }
        });
        targetSideInfo.getVirtualInventory().setPostUpdateHandler(event -> retrievePhase(false, false));

        System.out.println("VirtualInventory trader hash: " + traderSideInfo.getVirtualInventory().hashCode());
        System.out.println("VirtualInventory target hash: " + targetSideInfo.getVirtualInventory().hashCode());

        this.traderGui = createTraderGui();
        this.targetGui = createTargetGui();
    }

    //TRADER SIDE
    public void setTraderPrice(double price) {
        traderSideInfo.setProposed(price);
        notifyItem(1, 0, true);
        notifyItem(7, 0, false);
    }


    public void setAndSendTraderPrice(double price) {
        setTraderPrice(price);
        dataManager.updateTrade(uuid, RedisDataManager.TradeUpdateType.MONEY_TRADER, price);
    }

    public void setTraderStatus(OrderInfo.Status status) {
        traderSideInfo.setStatus(status);
        //Check if both are confirmed, then start the completion timer for the opposite player
        confirmPhase();
        notifyItem(0, 0, true);
        notifyItem(8, 0, false);
    }

    public void setAndSendTraderStatus(OrderInfo.Status status) {
        setTraderStatus(status);
        dataManager.updateTrade(uuid, RedisDataManager.TradeUpdateType.CONFIRM_TRADER, status.getStatusByte());
    }

    //TARGET SIDE
    public void setTargetPrice(double price) {
        targetSideInfo.setProposed(price);
        notifyItem(7, 0, true);
        notifyItem(1, 0, false);
    }

    public void setAndSendTargetPrice(double price) {
        setTargetPrice(price);
        dataManager.updateTrade(uuid, RedisDataManager.TradeUpdateType.MONEY_TARGET, price);
    }


    public void setTargetStatus(OrderInfo.Status status) {
        targetSideInfo.setStatus(status);
        confirmPhase();
        notifyItem(8, 0, true);
        notifyItem(0, 0, false);
    }

    public void setAndSendTargetStatus(OrderInfo.Status status) {
        setTargetStatus(status);
        dataManager.updateTrade(uuid, RedisDataManager.TradeUpdateType.CONFIRM_TARGET, status.getStatusByte());
    }

    public void updateTraderItem(int slot, ItemStack item, boolean sendUpdate) {
        if (sendUpdate) {
            dataManager.updateTrade(uuid, RedisDataManager.TradeUpdateType.ITEM_TRADER, slot + "§;" + Utils.serialize(item));
        } else {
            traderSideInfo.getVirtualInventory().setItemSilently(slot, item);
        }
    }

    public void updateTargetItem(int slot, ItemStack item, boolean sendUpdate) {
        if (sendUpdate) {
            dataManager.updateTrade(uuid, RedisDataManager.TradeUpdateType.ITEM_TARGET, slot + "§;" + Utils.serialize(item));
        } else {
            targetSideInfo.getVirtualInventory().setItemSilently(slot, item);
        }
    }

    /**
     * Called when a status is changed
     * Starts the completion timer if both are confirmed
     * Terminates the completion timer if one is refuted
     */
    public void confirmPhase() {
        if (traderSideInfo.getStatus() == OrderInfo.Status.CONFIRMED && targetSideInfo.getStatus() == OrderInfo.Status.CONFIRMED) {

            if (this.completionTimer == null || this.completionTimer.isCancelled()) {
                this.completionTimer = new CompletionTimer(this);
                this.completionTimer.runTask(RedisTrade.getInstance(), traderUUID, targetUUID);
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
        setAndSendTraderStatus(OrderInfo.Status.COMPLETED);
        setAndSendTargetStatus(OrderInfo.Status.COMPLETED);
        this.completionTimer.cancel();
        RedisTrade.getInstance().getEconomyHook()
                .depositPlayer(traderUUID, targetSideInfo.getProposed(),
                        "default", "Trade price");
        RedisTrade.getInstance().getEconomyHook()
                .depositPlayer(targetUUID, traderSideInfo.getProposed(),
                        "default", "Trade price");

        //Archive the completed trade
        this.dataManager.backupTrade(this, true);
        retrievePhase(true, false);
        retrievePhase(false, false);
    }

    /**
     * Check if an inventory is empty and the status is completed
     * Then it resets the trade for the player and locks the whole GUI
     *
     * @param isTrader If the inventory to check is the trader side
     */
    public void retrievePhase(boolean isTrader, boolean selfRetrieve) {
        if (isTrader) {
            if (traderSideInfo.getStatus() != OrderInfo.Status.COMPLETED && !selfRetrieve) return;
            if (traderSideInfo.getVirtualInventory().isEmpty()) {
                setAndSendTraderStatus(OrderInfo.Status.RETRIEVED);
                RedisTrade.getInstance().getTradeManager().finishTrade(
                        selfRetrieve ? traderUUID : targetUUID);
            }
        } else {
            if (targetSideInfo.getStatus() != OrderInfo.Status.COMPLETED && !selfRetrieve) return;
            if (targetSideInfo.getVirtualInventory().isEmpty()) {
                setAndSendTargetStatus(OrderInfo.Status.RETRIEVED);
                RedisTrade.getInstance().getTradeManager().finishTrade(
                        selfRetrieve ? targetUUID : traderUUID);
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
        if (isTrader) {
            //If the trade is completed, the target can modify the trader inventory
            return switch (traderSideInfo.getStatus()) {
                case COMPLETED -> !editingPlayer.equals(targetUUID);
                case CONFIRMED, RETRIEVED -> true;
                //If the trade is not completed, the trader can modify the trader inventory
                case REFUTED -> !editingPlayer.equals(traderUUID);
            };
        }
        return switch (targetSideInfo.getStatus()) {
            case COMPLETED -> !editingPlayer.equals(traderUUID);
            case CONFIRMED, RETRIEVED -> true;
            //If the trade is not completed, the target can modify the trader inventory
            case REFUTED -> !editingPlayer.equals(targetUUID);
        };
    }

    public boolean openWindow(String playerName, boolean isTrader) {
        Optional<? extends Player> optionalPlayer = RedisTrade.getInstance().getPlayer(playerName);
        if (optionalPlayer.isPresent()) {
            Window.single()
                    .setTitle((isTrader ? "Trader" : "Target") + " View")
                    .setGui(isTrader ? traderGui : targetGui)
                    .open(optionalPlayer.get());
            return true;
        }
        return false;
    }

    public TradeGuiImpl createTraderGui() {
        return new TradeGuiImpl.Builder()
                .setStructure(Settings.instance().tradeGuiStructure.toArray(new String[0]))
                .addIngredient('L', traderSideInfo.getVirtualInventory())
                .addIngredient('R', targetSideInfo.getVirtualInventory())
                .addIngredient('C', getTraderConfirmButton())
                .addIngredient('M', getTraderMoneyButton())
                .addIngredient('m', getTargetMoneyButton())
                .addIngredient('c', getTargetConfirmButton())
                .addIngredient('D', getTradeCancelButton(true))
                .addIngredient('x', Settings.instance().getButton(Settings.ButtonType.SEPARATOR))
                .build();
    }

    public TradeGuiImpl createTargetGui() {
        return new TradeGuiImpl.Builder()
                .setStructure(Settings.instance().tradeGuiStructure.toArray(new String[0]))
                .addIngredient('L', targetSideInfo.getVirtualInventory())
                .addIngredient('R', traderSideInfo.getVirtualInventory())
                .addIngredient('C', getTargetConfirmButton())
                .addIngredient('M', getTargetMoneyButton())
                .addIngredient('m', getTraderMoneyButton())
                .addIngredient('c', getTraderConfirmButton())
                .addIngredient('D', getTradeCancelButton(false))
                .addIngredient('x', Settings.instance().getButton(Settings.ButtonType.SEPARATOR))
                .build();
    }

    public Item getTraderMoneyButton() {
        return new AbstractItem() {
            @Override
            public ItemProvider getItemProvider() {
                return new ItemBuilder(Settings.instance().getButton(Settings.ButtonType.MONEY_BUTTON))
                        .setDisplayName("§aProposed " + traderSideInfo.getProposed() + "$");
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                if (traderSideInfo.getStatus() == OrderInfo.Status.REFUTED)
                    new MoneySelectorGUI(NewTrade.this, true, traderSideInfo.getProposed(), (Player) event.getWhoClicked());
            }
        };
    }

    public Item getTargetMoneyButton() {
        return new AbstractItem() {
            @Override
            public ItemProvider getItemProvider() {
                return new ItemBuilder(Settings.instance().getButton(Settings.ButtonType.MONEY_BUTTON))
                        .setDisplayName("§aProposed " + targetSideInfo.getProposed() + "$");
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                if (targetSideInfo.getStatus() == OrderInfo.Status.REFUTED)
                    new MoneySelectorGUI(NewTrade.this, false, targetSideInfo.getProposed(), (Player) event.getWhoClicked());
            }
        };
    }

    public Item getTraderConfirmButton() {
        return new SuppliedItem(() ->
                switch (traderSideInfo.getStatus()) {
                    case REFUTED -> new ItemBuilder(Settings.instance().getButton(Settings.ButtonType.REFUTE_BUTTON));
                    case CONFIRMED ->
                            new ItemBuilder(Settings.instance().getButton(Settings.ButtonType.CONFIRM_BUTTON));
                    case COMPLETED ->
                            new ItemBuilder(Settings.instance().getButton(Settings.ButtonType.COMPLETED_BUTTON));
                    case RETRIEVED ->
                            new ItemBuilder(Settings.instance().getButton(Settings.ButtonType.RETRIEVED_BUTTON));
                }, (inventoryClickEvent) -> {

            if (traderSideInfo.getStatus() == OrderInfo.Status.COMPLETED ||
                    traderSideInfo.getStatus() == OrderInfo.Status.RETRIEVED) return false;
            OrderInfo.Status newStatus;
            if (traderSideInfo.getStatus() == OrderInfo.Status.REFUTED) {
                newStatus = OrderInfo.Status.CONFIRMED;
            } else {
                newStatus = OrderInfo.Status.REFUTED;
            }
            setAndSendTraderStatus(newStatus);
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
                retrievePhase(isTrader, true);
                RedisTrade.getInstance().getEconomyHook().depositPlayer(player.getUniqueId(),
                        isTrader ? traderSideInfo.getProposed() : targetSideInfo.getProposed(),
                        "default",
                        "Trade price");
                if (isTrader) {
                    setAndSendTraderPrice(0);
                } else {
                    setAndSendTargetPrice(0);
                }
            }
        };
    }

    public Item getTargetConfirmButton() {
        return new SuppliedItem(() ->
                switch (targetSideInfo.getStatus()) {
                    case REFUTED -> new ItemBuilder(Settings.instance().getButton(Settings.ButtonType.REFUTE_BUTTON));
                    case CONFIRMED ->
                            new ItemBuilder(Settings.instance().getButton(Settings.ButtonType.CONFIRM_BUTTON));
                    case COMPLETED ->
                            new ItemBuilder(Settings.instance().getButton(Settings.ButtonType.COMPLETED_BUTTON));
                    case RETRIEVED ->
                            new ItemBuilder(Settings.instance().getButton(Settings.ButtonType.RETRIEVED_BUTTON));
                }, (inventoryClickEvent) -> {

            if (traderSideInfo.getStatus() == OrderInfo.Status.COMPLETED ||
                    traderSideInfo.getStatus() == OrderInfo.Status.RETRIEVED) return false;
            OrderInfo.Status newStatus;
            if (targetSideInfo.getStatus() == OrderInfo.Status.REFUTED) {
                newStatus = OrderInfo.Status.CONFIRMED;
            } else {
                newStatus = OrderInfo.Status.REFUTED;
            }
            setAndSendTargetStatus(newStatus);
            return true;
        });
    }

    public void notifyItem(int x, int y, boolean isTrader) {
        Optional.ofNullable((isTrader ? traderGui : targetGui).getItem(x, y)).ifPresent(Item::notifyWindows);
    }

    public byte[] serialize() {
        byte[] traderData = traderSideInfo.serialize();
        byte[] targetData = targetSideInfo.serialize();
        //Allocate bytes for TradeUUID, TraderUUID, TargetUUID, TraderName, TargetName, TraderData size, TargetData size, TraderData, TargetData
        ByteBuffer bb = ByteBuffer.allocate(16 + 16 + 16 + 16 + 16 + 4 + 4 + traderData.length + targetData.length);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());

        bb.putLong(traderUUID.getMostSignificantBits());
        bb.putLong(traderUUID.getLeastSignificantBits());

        bb.putLong(targetUUID.getMostSignificantBits());
        bb.putLong(targetUUID.getLeastSignificantBits());

        byte[] paddedTraderName = new byte[16];
        System.arraycopy(traderName.getBytes(StandardCharsets.ISO_8859_1), 0, paddedTraderName, 0, traderName.length());
        bb.put(paddedTraderName);

        byte[] paddedTargetName = new byte[16];
        System.arraycopy(targetName.getBytes(StandardCharsets.ISO_8859_1), 0, paddedTargetName, 0, targetName.length());
        bb.put(paddedTargetName);

        bb.putInt(traderData.length);
        bb.putInt(targetData.length);

        bb.put(traderData);
        bb.put(targetData);

        return bb.array();
    }

    public byte[] serializeEmpty() {
        ByteBuffer bb = ByteBuffer.allocate(16 + 16 + 16 + 16 + 16 + 4 + 4);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());

        bb.putLong(traderUUID.getMostSignificantBits());
        bb.putLong(traderUUID.getLeastSignificantBits());

        bb.putLong(targetUUID.getMostSignificantBits());
        bb.putLong(targetUUID.getLeastSignificantBits());

        byte[] paddedTraderName = new byte[16];
        System.arraycopy(traderName.getBytes(StandardCharsets.ISO_8859_1), 0, paddedTraderName, 0, traderName.length());
        bb.put(paddedTraderName);

        byte[] paddedTargetName = new byte[16];
        System.arraycopy(targetName.getBytes(StandardCharsets.ISO_8859_1), 0, paddedTargetName, 0, targetName.length());
        bb.put(paddedTargetName);
        return bb.array();
    }

    public static NewTrade deserialize(RedisDataManager dataManager, byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        UUID uuid = new UUID(bb.getLong(), bb.getLong());
        UUID traderUUID = new UUID(bb.getLong(), bb.getLong());
        UUID targetUUID = new UUID(bb.getLong(), bb.getLong());

        byte[] traderNameBytes = new byte[16];
        bb.get(traderNameBytes);
        String traderName = new String(traderNameBytes, StandardCharsets.ISO_8859_1).trim();
        byte[] targetNameBytes = new byte[16];
        bb.get(targetNameBytes);
        String targetName = new String(targetNameBytes, StandardCharsets.ISO_8859_1).trim();

        int traderSize = bb.getInt();
        int targetSize = bb.getInt();
        byte[] traderData = new byte[traderSize];
        byte[] targetData = new byte[targetSize];
        bb.get(traderData);
        bb.get(targetData);
        return new NewTrade(dataManager, uuid, traderUUID, targetUUID, traderName, targetName, OrderInfo.deserialize(traderData), OrderInfo.deserialize(targetData));
    }

    public static NewTrade deserializeEmpty(RedisDataManager dataManager, byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        UUID uuid = new UUID(bb.getLong(), bb.getLong());
        UUID traderUUID = new UUID(bb.getLong(), bb.getLong());
        UUID targetUUID = new UUID(bb.getLong(), bb.getLong());

        byte[] traderNameBytes = new byte[16];
        bb.get(traderNameBytes);
        String traderName = new String(traderNameBytes, StandardCharsets.ISO_8859_1).trim();

        byte[] targetNameBytes = new byte[16];
        bb.get(targetNameBytes);
        String targetName = new String(targetNameBytes, StandardCharsets.ISO_8859_1).trim();
        return new NewTrade(dataManager, uuid, traderUUID, targetUUID, traderName, targetName, new OrderInfo(20), new OrderInfo(20));
    }


}
