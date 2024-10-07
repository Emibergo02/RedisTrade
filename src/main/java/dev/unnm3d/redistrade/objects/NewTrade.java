package dev.unnm3d.redistrade.objects;

import dev.unnm3d.redistrade.Settings;
import dev.unnm3d.redistrade.Utils;
import dev.unnm3d.redistrade.data.RedisDataManager;
import dev.unnm3d.redistrade.guis.MoneySelectorGUI;
import dev.unnm3d.redistrade.guis.OrderInfo;
import dev.unnm3d.redistrade.guis.TradeGuiImpl;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SuppliedItem;
import xyz.xenondevs.invui.window.Window;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;

@Getter
@EqualsAndHashCode
@ToString
public class NewTrade {

    private static final String[] structureStrings = new String[]{
            "CM#####mc",
            "LLLL#RRRR",
            "LLLL#RRRR",
            "LLLL#RRRR",
            "LLLL#RRRR",
            "LLLL#RRRR"};

    private final UUID uuid;
    private final OrderInfo traderSideInfo;
    private final OrderInfo targetSideInfo;

    private transient final RedisDataManager dataManager;
    private transient final Gui traderGui;
    private transient final Gui targetGui;

    public NewTrade(RedisDataManager dataManager) {
        this.dataManager = dataManager;
        this.uuid = UUID.randomUUID();
        this.traderSideInfo = new OrderInfo(20);
        this.targetSideInfo = new OrderInfo(20);

        traderSideInfo.getVirtualInventory().setPreUpdateHandler(event ->
                dataManager.updateTrade(uuid, RedisDataManager.TradeUpdateType.ITEM_TRADER, event.getSlot() + "§;" + Utils.serialize(event.getNewItem())));
        targetSideInfo.getVirtualInventory().setPreUpdateHandler(event ->
                dataManager.updateTrade(uuid, RedisDataManager.TradeUpdateType.ITEM_TARGET, event.getSlot() + "§;" + Utils.serialize(event.getNewItem())));

        this.traderGui = createTraderGui();
        this.targetGui = createTargetGui();
    }

    public NewTrade(RedisDataManager dataManager, UUID uuid, OrderInfo traderSideInfo, OrderInfo targetSideInfo) {
        this.dataManager = dataManager;
        this.uuid = uuid;
        this.traderSideInfo = traderSideInfo;
        this.targetSideInfo = targetSideInfo;
        System.out.println("Deserialize " + this);

        traderSideInfo.getVirtualInventory().setPreUpdateHandler(event ->
                dataManager.updateTrade(uuid, RedisDataManager.TradeUpdateType.ITEM_TRADER, event.getSlot() + "§;" + Utils.serialize(event.getNewItem())));
        targetSideInfo.getVirtualInventory().setPreUpdateHandler(event ->
                dataManager.updateTrade(uuid, RedisDataManager.TradeUpdateType.ITEM_TARGET, event.getSlot() + "§;" + Utils.serialize(event.getNewItem())));

        this.traderGui = createTraderGui();
        this.targetGui = createTargetGui();
    }

    //TRADER SIDE
    public void setTraderPrice(double price) {
        traderSideInfo.setProposed(price);
        notifyTraderItem(1, 0);
        notifyTargetItem(7, 0);
    }


    public void setAndSendTraderPrice(double price) {
        setTraderPrice(price);
        dataManager.updateTrade(uuid, RedisDataManager.TradeUpdateType.MONEY_TRADER, price);
    }

    public void setTraderConfirm(boolean confirm) {
        traderSideInfo.setConfirmed(confirm);
        notifyTraderItem(0, 0);
        notifyTargetItem(8, 0);
    }

    public void setAndSendTraderConfirm(boolean confirm) {
        setTraderConfirm(confirm);
        dataManager.updateTrade(uuid, RedisDataManager.TradeUpdateType.CONFIRM_TRADER, confirm);
    }

    //TARGET SIDE
    public void setTargetPrice(double price) {
        targetSideInfo.setProposed(price);
        notifyTraderItem(7, 0);
        notifyTargetItem(1, 0);
    }

    public void setAndSendTargetPrice(double price) {
        setTargetPrice(price);
        dataManager.updateTrade(uuid, RedisDataManager.TradeUpdateType.MONEY_TARGET, price);
    }


    public void setTargetConfirm(boolean confirm) {
        targetSideInfo.setConfirmed(confirm);
        notifyTraderItem(8, 0);
        notifyTargetItem(0, 0);
    }

    public void setAndSendTargetConfirm(boolean confirm) {
        setTargetConfirm(confirm);
        dataManager.updateTrade(uuid, RedisDataManager.TradeUpdateType.CONFIRM_TARGET, confirm);
    }

    public void updateTraderItem(int slot, ItemStack item) {
        traderSideInfo.getVirtualInventory().setItemSilently(slot, item);
    }

    public void updateTargetItem(int slot, ItemStack item) {
        targetSideInfo.getVirtualInventory().setItemSilently(slot, item);
    }


    public void openRemoteWindow(String playerName, boolean isTrader) {
        if (!openWindow(playerName, isTrader)) {
            dataManager.openRemoteWindow(playerName, isTrader, this.uuid);
        }
    }

    public boolean openWindow(String playerName, boolean isTrader) {
        Optional<? extends Player> optionalPlayer = Bukkit.getServer().getOnlinePlayers().stream()
                .filter(player -> player.getName().equalsIgnoreCase(playerName))
                .findFirst();
        if (optionalPlayer.isPresent()) {
            Window.single()
                    .setTitle((isTrader ? "Trader" : "Target") + " View")
                    .setGui(isTrader ? traderGui : targetGui)
                    .open(optionalPlayer.get());
            return true;
        }
        return false;
    }

    public Gui createTraderGui() {
        return new TradeGuiImpl.Builder()
                .setStructure(structureStrings)
                .addIngredient('L', traderSideInfo.getVirtualInventory())
                .addIngredient('R', targetSideInfo.getVirtualInventory())
                .addIngredient('C', new SuppliedItem(() -> {
                    if (traderSideInfo.isConfirmed()) {
                        return new ItemBuilder(Settings.instance().buttons.get(Settings.ButtonType.CONFIRM_BUTTON));
                    }
                    return new ItemBuilder(Settings.instance().buttons.get(Settings.ButtonType.REFUTE_BUTTON));
                }, (inventoryClickEvent) -> {
                    setAndSendTraderConfirm(!traderSideInfo.isConfirmed());
                    return true;
                }))
                .addIngredient('M', new SuppliedItem(() -> new ItemBuilder(Material.GOLD_NUGGET)
                        .setDisplayName(traderSideInfo.getProposed() + "$")
                        .addLoreLines("Seller price"), clickEvent -> {
                    new MoneySelectorGUI(this, true, traderSideInfo.getProposed(), clickEvent.getPlayer());
                    return false;
                }))
                .addIngredient('m', new SuppliedItem(() -> new ItemBuilder(Material.GOLD_NUGGET)
                        .setDisplayName(targetSideInfo.getProposed() + "$")
                        .addLoreLines("Seller price"), null))
                .addIngredient('c', new SuppliedItem(() -> {
                    if (targetSideInfo.isConfirmed()) {
                        return new ItemBuilder(Material.GREEN_WOOL).setDisplayName("Confirmed");
                    } else {
                        return new ItemBuilder(Material.RED_WOOL).setDisplayName("Not confirmed");
                    }
                }, null))
                .build();
    }

    public Gui createTargetGui() {
        return new TradeGuiImpl.Builder()
                .setStructure(structureStrings)
                .addIngredient('L', targetSideInfo.getVirtualInventory())
                .addIngredient('R', traderSideInfo.getVirtualInventory())
                .addIngredient('C', new SuppliedItem(() -> {
                    if (targetSideInfo.isConfirmed()) {
                        return new ItemBuilder(Settings.instance().buttons.get(Settings.ButtonType.CONFIRM_BUTTON));
                    }
                    return new ItemBuilder(Settings.instance().buttons.get(Settings.ButtonType.REFUTE_BUTTON));
                }, (inventoryClickEvent) -> {
                    setAndSendTargetConfirm(!targetSideInfo.isConfirmed());
                    return true;
                }))
                .addIngredient('M', new SuppliedItem(() -> new ItemBuilder(Material.GOLD_NUGGET)
                        .setDisplayName(targetSideInfo.getProposed() + "$")
                        .addLoreLines("Seller price"), clickEvent -> {
                    new MoneySelectorGUI(this, false, targetSideInfo.getProposed(), clickEvent.getPlayer());
                    return false;
                }))
                .addIngredient('m', new SuppliedItem(() -> new ItemBuilder(Material.GOLD_NUGGET)
                        .setDisplayName(traderSideInfo.getProposed() + "$")
                        .addLoreLines("Seller price"), null))
                .addIngredient('c', new SuppliedItem(() -> {
                    if (traderSideInfo.isConfirmed()) {
                        return new ItemBuilder(Material.GREEN_WOOL).setDisplayName("Confirmed");
                    } else {
                        return new ItemBuilder(Material.RED_WOOL).setDisplayName("Not confirmed");
                    }
                }, null))
                .build();
    }

    public void notifyTraderItem(int x, int y) {
        Optional.ofNullable(traderGui.getItem(x, y)).ifPresent(Item::notifyWindows);
    }

    public void notifyTargetItem(int x, int y) {
        Optional.ofNullable(targetGui.getItem(x, y)).ifPresent(Item::notifyWindows);
    }

    public byte[] serialize() {
        byte[] traderData = traderSideInfo.serialize();
        byte[] targetData = targetSideInfo.serialize();
        //Allocate bytes for UUID, TraderData size, TargetData size, TraderData, TargetData
        ByteBuffer bb = ByteBuffer.allocate(16 + 4 + 4 + traderData.length + targetData.length);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());

        bb.putInt(traderData.length);
        bb.putInt(targetData.length);

        bb.put(traderData);
        bb.put(targetData);

        return bb.array();
    }

    public static NewTrade deserialize(RedisDataManager dataManager, byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        UUID uuid = new UUID(bb.getLong(), bb.getLong());
        int traderSize = bb.getInt();
        int targetSize = bb.getInt();
        byte[] traderData = new byte[traderSize];
        byte[] targetData = new byte[targetSize];
        bb.get(traderData);
        bb.get(targetData);
        return new NewTrade(dataManager, uuid, OrderInfo.deserialize(traderData), OrderInfo.deserialize(targetData));
    }


}
