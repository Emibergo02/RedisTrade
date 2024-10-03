package dev.unnm3d.redistrade.guis.maingui;

import dev.unnm3d.redistrade.Settings;
import dev.unnm3d.redistrade.guis.MoneySelectorGUI;
import dev.unnm3d.redistrade.objects.trade.TradeGUI;
import dev.unnm3d.redistrade.guis.maingui.tradeslot.AbstractItemSlot;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemWrapper;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SuppliedItem;

import java.util.Optional;

public class TraderGui extends AbstractTradeGui {


    public TraderGui(TradeGUI trade) {
        super(trade);
    }

    @Override
    public void drawOrNotifyConfirm() {
        //Update opposite confirm button
        Optional.ofNullable(getItem(8)).ifPresentOrElse(Item::notifyWindows, () ->
                setItem(8, new SuppliedItem(() -> {
                    if (trade.isTargetConfirmed()) {
                        return new ItemBuilder(Material.GREEN_WOOL).setDisplayName("Confirmed");
                    } else {
                        return new ItemBuilder(Material.RED_WOOL).setDisplayName("Not confirmed");
                    }
                }, null)));

        //Draw local confirm button
        if (getItem(0) != null) {
            return;
        }
        setItem(0, new SuppliedItem(() -> {
            if (trade.isTraderConfirmed()) {
                return new ItemBuilder(Settings.instance().buttons.get(Settings.ButtonType.CONFIRM_BUTTON));
            }
            return new ItemBuilder(Settings.instance().buttons.get(Settings.ButtonType.REFUTE_BUTTON));
        }, (inventoryClickEvent) -> {
            trade.setTraderConfirm(!trade.getTraderSideInfo().isConfirmed());
            return true;
        }));
    }

    @Override
    public void drawOrNotifyMoney() {
        Optional.ofNullable(getItem(1)).ifPresentOrElse(Item::notifyWindows, () ->
                setItem(1, new SuppliedItem(() -> new ItemBuilder(Material.GOLD_NUGGET)
                        .setDisplayName(trade.getTraderPrice() + "$")
                        .addLoreLines("Seller price"), clickEvent -> {
                    new MoneySelectorGUI(this, trade.getTraderPrice(), clickEvent.getPlayer());
                    return false;
                })));

        Optional.ofNullable(getItem(7)).ifPresentOrElse(Item::notifyWindows, () ->
                setItem(7, new SuppliedItem(() -> new ItemBuilder(Material.GOLD_NUGGET)
                        .setDisplayName(trade.getTargetPrice() + "$")
                        .addLoreLines("Seller price"), null)));
    }


    @Override
    public void drawRightSide() {
        for (int i = 0; i < trade.getTargetSideInfo().getItems().length; i++) {
            int finalI = i;
            int x = 8 - (i % 4);
            int y = i / 4 + 1;
            System.out.println("Drawing item at x: " + x + " y: " + y + " index: " + i);
            setItem(x, y, new SuppliedItem(() -> trade.getTargetItem(finalI) == null ?
                    new ItemWrapper(new ItemStack(Material.AIR)) :
                    new ItemWrapper(trade.getTargetItem(finalI)),
                    null));
        }
    }

    @Override
    public void drawLeftSide() {
        for (int row = 1; row < getHeight(); row++) {
            int startingIndex = 9 * row;
            for (int slotN = startingIndex; slotN < startingIndex + 4; slotN++) {
                setItem(slotN, new AbstractItemSlot() {
                    @Override
                    public void updateItem(int slot, ItemStack item) {
                        int x = slot % 9;
                        int y = slot / 9;
                        trade.updateTraderItem(x, y, item);
                    }
                });
            }
        }
    }
}
