package dev.unnm3d.redistrade.guis;

import dev.unnm3d.redistrade.objects.NewTrade;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.AnvilWindow;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;

public class MoneySelectorGUI {
    private static final DecimalFormat df = new DecimalFormat("#.##");
    private final NewTrade trade;
    private final boolean isTrader;
    private final Gui currentGui;
    private final Player viewer;
    private String moneyString;

    public MoneySelectorGUI(NewTrade trade, boolean isTrader, double currentPrice, Player viewer) {
        this.trade = trade;
        this.isTrader = isTrader;
        this.currentGui = Gui.empty(3, 1);
        this.moneyString = df.format(currentPrice);
        this.viewer = viewer;
        currentGui.setItem(0, getMoneyDisplay());
        currentGui.setItem(2, getConfirmDisplay());
        AnvilWindow.single()
                .setRenameHandlers(List.of(this::handleRename))
                .setGui(currentGui)
                .setTitle("Money editor")
                .setCloseable(false)
                .open(viewer);
    }

    private void handleRename(String moneyString) {
        this.moneyString = moneyString;
        Optional.ofNullable(currentGui.getItem(2)).ifPresent(Item::notifyWindows);
    }

    private SimpleItem getMoneyDisplay() {
        return new SimpleItem(
                new ItemBuilder(Material.GOLD_NUGGET)
                        .setDisplayName(this.moneyString)
                        .addLoreLines("Set your price"));
    }

    private AbstractItem getConfirmDisplay() {
        return new AbstractItem() {
            @Override
            public ItemProvider getItemProvider() {
                return new ItemBuilder(Material.GREEN_WOOL)
                        .setDisplayName("Confirm price " + moneyString)
                        .addLoreLines("Confirm your trade price");
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                try {
                    if (isTrader) {
                        trade.setAndSendTraderPrice(Double.parseDouble(moneyString));
                        trade.openWindow(viewer.getName(), true);
                    } else {
                        trade.setAndSendTargetPrice(Double.parseDouble(moneyString));
                        trade.openWindow(viewer.getName(), false);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        };
    }
}
