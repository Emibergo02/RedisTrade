package dev.unnm3d.redistrade.guis;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.GuiSettings;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.core.NewTrade;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.window.AnvilWindow;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;

public class MoneySelectorGUI implements Listener {
    private static final DecimalFormat df = new DecimalFormat("#.##");

    private final Gui currentGui;

    private final NewTrade trade;
    private final Player viewer;
    private final boolean isTrader;
    private final String currencyName;
    private String changingPriceString;

    public MoneySelectorGUI(NewTrade trade, Player viewer, String currencyName) {
        this.trade = trade;
        this.currentGui = Gui.empty(3, 1);
        this.isTrader = trade.isTrader(viewer.getUniqueId());
        this.viewer = viewer;
        this.currencyName = currencyName;
        this.changingPriceString = df.format(getPreviousPrice());
        currentGui.setItem(0, getMoneyDisplay());
        currentGui.setItem(2, getConfirmDisplay());
        AnvilWindow.single()
                .setRenameHandlers(List.of(this::handleRename))
                .setGui(currentGui)
                .setTitle("Money editor")
                .setCloseable(true)
                .open(viewer);
    }

    private double getPreviousPrice() {
        return trade.getOrderInfo(isTrader).getPrice(currencyName);
    }

    private void handleRename(String moneyString) {
        this.changingPriceString = moneyString;
        notifyItem(2);
    }

    private void notifyItem(int index) {
        Optional.ofNullable(currentGui.getItem(index)).ifPresent(Item::notifyWindows);
    }

    public Item getMoneyDisplay() {
        return new AbstractItem() {
            @Override
            public ItemProvider getItemProvider() {
                return GuiSettings.instance().moneyDisplay
                        .toItemBuilder().setLegacyDisplayName(changingPriceString);
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                event.setCancelled(true);
            }
        };
    }

    private AbstractItem getConfirmDisplay() {
        return new AbstractItem() {
            @Override
            public ItemProvider getItemProvider() {
                return GuiSettings.instance().moneyConfirmButton.toItemBuilder()
                        .setMiniMessageItemName(Messages.instance().confirmMoneyDisplay
                                .replace("%amount%", changingPriceString)
                                .replace("%symbol%", RedisTrade.getInstance().getEconomyHook()
                                        .getCurrencySymbol(currencyName))
                        );
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                try {
                    double nextPrice = Math.abs(Double.parseDouble(changingPriceString));
                    double balance = RedisTrade.getInstance().getEconomyHook()
                            .getBalance(viewer.getUniqueId(), currencyName);
                    //The price is the difference between the previous price and the new price
                    double deducedPrice = getPreviousPrice() - nextPrice;

                    //Subtract the new price from the previous price
                    boolean response;
                    if (deducedPrice < 0) {//If the price is negative, then the player will pay more
                        response = RedisTrade.getInstance().getEconomyHook().withdrawPlayer(
                                player.getUniqueId(), Math.abs(deducedPrice),
                                currencyName, "Trade price");
                    } else {//If the price is positive, then the player will be refunded
                        response = RedisTrade.getInstance().getEconomyHook().depositPlayer(
                                player.getUniqueId(), deducedPrice,
                                currencyName, "Trade price");
                    }
                    if (response) {
                        trade.setAndSendPrice(currencyName, nextPrice, isTrader);

                        player.getInventory().addItem(player.getItemOnCursor()).values().forEach(
                                itemStack -> player.getWorld().dropItem(player.getLocation(), itemStack));
                        player.setItemOnCursor(null);
                        trade.openWindow(viewer.getUniqueId(), isTrader);
                        return;
                    }

                    viewer.sendRichMessage(Messages.instance().notEnoughMoney
                            .replace("%amount%", df.format(nextPrice)));
                    //Set the price as the maximum withdrawable from the player account
                    changingPriceString = df.format(balance);
                    notifyItem(0);
                    notifyItem(2);

                } catch (NumberFormatException ignored) {
                    changingPriceString = df.format(getPreviousPrice());
                    notifyItem(0);
                    notifyItem(2);
                }
            }
        };
    }
}
