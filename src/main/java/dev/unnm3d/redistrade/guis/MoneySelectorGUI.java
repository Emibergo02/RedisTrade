package dev.unnm3d.redistrade.guis;

import dev.unnm3d.redistrade.guis.maingui.AbstractTradeGui;
import dev.unnm3d.redistrade.guis.maingui.TraderGui;
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
    private static final DecimalFormat df = new DecimalFormat("0.00");
    private final AbstractTradeGui abstractTradeGui;
    private final Gui currentGui;
    private final Player viewer;
    private String moneyString;

    public MoneySelectorGUI(@NotNull AbstractTradeGui abstractTradeGui, double currentPrice, Player viewer) {
        this.abstractTradeGui = abstractTradeGui;
        this.currentGui = Gui.empty(3, 1);
        this.moneyString = df.format(currentPrice);
        this.viewer = viewer;
        currentGui.setItem(0, getMoneyDisplay());
        currentGui.setItem(2, getConfirmDisplay());
        AnvilWindow.single()
                .setRenameHandlers(List.of(this::handleRename))
                .setGui(currentGui)
                .setTitle("Money editor")
                .setCloseable(true)
                .setCloseHandlers(List.of(this::handleClose))
                .open(viewer);
    }

    private void handleRename(String moneyString) {
        this.moneyString = moneyString;
        Optional.ofNullable(currentGui.getItem(2)).ifPresent(Item::notifyWindows);
    }

    private void handleClose() {
        abstractTradeGui.openWindow(viewer);
    }

    private SimpleItem getMoneyDisplay() {
        return new SimpleItem(
                new ItemBuilder(Material.GOLD_NUGGET)
                        .setDisplayName("0")
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
                    if (abstractTradeGui instanceof TraderGui) {
                        abstractTradeGui.getTrade().setTraderPrice(Double.parseDouble(moneyString));
                    } else {
                        abstractTradeGui.getTrade().setTargetPrice(Double.parseDouble(moneyString));
                    }

                    abstractTradeGui.openWindow(player);
                } catch (NumberFormatException ignored) {
                }
            }
        };
    }
}
