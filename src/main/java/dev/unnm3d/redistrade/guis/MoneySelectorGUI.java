package dev.unnm3d.redistrade.guis;

import dev.unnm3d.redistrade.Messages;
import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.Settings;
import dev.unnm3d.redistrade.objects.NewTrade;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.window.AnvilWindow;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;

public class MoneySelectorGUI implements Listener {
    private static final DecimalFormat df = new DecimalFormat("#.##");

    private final NewTrade trade;
    private final Gui currentGui;
    private final boolean isTrader;
    private final Player viewer;
    private final double previousPrice;
    private String changingPriceString;

    public MoneySelectorGUI(NewTrade trade, boolean isTrader, double currentPrice, Player viewer) {
        this.trade = trade;
        this.isTrader = isTrader;
        this.currentGui = Gui.empty(3, 1);
        this.previousPrice = currentPrice;
        this.changingPriceString = df.format(currentPrice);
        this.viewer = viewer;
        currentGui.setItem(0, getMoneyDisplay());
        currentGui.setItem(2, getConfirmDisplay());
        AnvilWindow.single()
                .setRenameHandlers(List.of(this::handleRename))
                .setGui(currentGui)
                .setTitle("Money editor")
                .setCloseable(true)
                .open(viewer);
    }

    private void handleRename(String moneyString) {
        this.changingPriceString = moneyString;
        updateItems();
    }

    private AbstractItem getMoneyDisplay() {
        return new AbstractItem() {
            @Override
            public ItemProvider getItemProvider() {
                return new ItemBuilder(Settings.instance().getButton(Settings.ButtonType.MONEY_DISPLAY))
                        .setDisplayName(changingPriceString);
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                event.setCancelled(true);
            }
        };
    }

    private void updateItems() {
        Optional.ofNullable(currentGui.getItem(2)).ifPresent(Item::notifyWindows);
    }

    private AbstractItem getConfirmDisplay() {
        return new AbstractItem() {
            @Override
            public ItemProvider getItemProvider() {
                return new ItemBuilder(Settings.instance().getButton(Settings.ButtonType.MONEY_CONFIRM_BUTTON))
                        .setDisplayName("§aConfirm " + changingPriceString);
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                try {
                    double nextPrice = Math.abs(Double.parseDouble(changingPriceString));
                    double balance = RedisTrade.getInstance().getEconomyHook().getBalance(viewer.getUniqueId(), "default");
                    double deducedPrice = previousPrice - nextPrice;
                    //If the player balance plus the money already present is less than the new price
                    if (balance + previousPrice < nextPrice) {
                        viewer.sendRichMessage(Messages.instance().notEnoughMoney
                                .replace("%amount%", df.format(nextPrice)));
                        MoneySelectorGUI.this.changingPriceString = df.format(balance);
                        updateItems();
                        return;
                    }
                    //Subtract the new price from the previous price
                    //If the price is negative, then the player will pay more
                    //If the price is positive, then the player will be refunded
                    boolean response;
                    if (deducedPrice < 0) {
                        response = RedisTrade.getInstance().getEconomyHook().withdrawPlayer(
                                player.getUniqueId(), Math.abs(deducedPrice),
                                "default", "Trade price");
                    } else {
                        response = RedisTrade.getInstance().getEconomyHook().depositPlayer(
                                player.getUniqueId(), deducedPrice,
                                "default", "Trade price");
                    }
                    if (response) {
                        if (isTrader) {
                            trade.setAndSendTraderPrice(nextPrice);
                        } else {
                            trade.setAndSendTargetPrice(nextPrice);
                        }
                        player.getInventory().addItem(player.getItemOnCursor()).values().forEach(
                                itemStack -> player.getWorld().dropItem(player.getLocation(), itemStack));
                        player.setItemOnCursor(null);
                        trade.openWindow(viewer.getName(), isTrader);
                        return;
                    }

                    player.sendRichMessage(Messages.instance().notEnoughMoney
                            .replace("%amount%", df.format(nextPrice)));

                } catch (NumberFormatException ignored) {
                    MoneySelectorGUI.this.changingPriceString = df.format(previousPrice);
                    updateItems();
                }
            }
        };
    }
}
