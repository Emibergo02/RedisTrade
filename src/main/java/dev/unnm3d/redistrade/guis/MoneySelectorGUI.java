package dev.unnm3d.redistrade.guis;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.GuiSettings;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.core.NewTrade;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.window.AnvilWindow;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;

public class MoneySelectorGUI implements Listener {
    private static final DecimalFormat df = new DecimalFormat("#.##");

    private final Gui currentGui;

    private final NewTrade trade;
    private final String currencyName;
    private final double previousPrice;
    private String changingPriceString;

    public MoneySelectorGUI(NewTrade trade, ViewerType viewerType, String currencyName) {
        this.trade = trade;
        this.currentGui = Gui.empty(3, 1);
        this.currencyName = currencyName;
        this.previousPrice = trade.getOrderInfo(viewerType).getPrice(currencyName);
        this.changingPriceString = df.format(previousPrice);
        currentGui.setItem(0, getMoneyDisplay());
        currentGui.setItem(2, getConfirmDisplay());
    }

    public void openWindow(Player player) {
        AnvilWindow.single()
                .setRenameHandlers(List.of(this::handleRename))
                .setGui(currentGui)
                .setTitle("Money editor")
                .setCloseable(true)
                .open(player);
    }

    private void handleRename(String moneyString) {
        this.changingPriceString = moneyString;
        notifyItem(2);
    }

    private void notifyItem(int index) {
        Optional.ofNullable(currentGui.getItem(index)).ifPresent(Item::notifyWindows);
    }

    public Item getMoneyDisplay() {
        return Item.simple(player -> GuiSettings.instance().moneyDisplay.toItemBuilder()
                .setLegacyDisplayName(changingPriceString));
    }

    private Item getConfirmDisplay() {
        return Item.builder()
                .setItemProvider(player -> GuiSettings.instance().moneyConfirmButton.toItemBuilder()
                        .setMiniMessageItemName(Messages.instance().confirmMoneyDisplay
                                .replace("%amount%", changingPriceString)
                                .replace("%symbol%", RedisTrade.getInstance().getEconomyHook()
                                        .getCurrencySymbol(currencyName))))
                .addClickHandler((item, click) -> {
                    try {
                        double nextPrice = Math.abs(Double.parseDouble(changingPriceString));
                        double balance = RedisTrade.getInstance().getEconomyHook()
                                .getBalance(click.getPlayer().getUniqueId(), currencyName);
                        //The price is the difference between the previous price and the new price
                        double deducedPrice = previousPrice - nextPrice;

                        //Subtract the new price from the previous price
                        boolean response;
                        if (deducedPrice < 0) {//If the price is negative, then the player will pay more
                            response = RedisTrade.getInstance().getEconomyHook().withdrawPlayer(
                                    click.getPlayer().getUniqueId(), Math.abs(deducedPrice),
                                    currencyName, "Trade price");
                        } else {//If the price is positive, then the player will be refunded
                            response = RedisTrade.getInstance().getEconomyHook().depositPlayer(
                                    click.getPlayer().getUniqueId(), deducedPrice,
                                    currencyName, "Trade price");
                        }
                        if (response) {
                            trade.setAndSendPrice(currencyName, nextPrice, trade.getViewerType(click.getPlayer().getUniqueId()));

                            click.getPlayer().getInventory().addItem(click.getPlayer().getItemOnCursor()).values()
                                    .forEach(itemStack -> click.getPlayer().getWorld()
                                            .dropItem(click.getPlayer().getLocation(), itemStack));
                            click.getPlayer().setItemOnCursor(null);
                            trade.openWindow(click.getPlayer().getUniqueId(), trade.getViewerType(click.getPlayer().getUniqueId()));
                            return;
                        }

                        click.getPlayer().sendRichMessage(Messages.instance().notEnoughMoney
                                .replace("%amount%", df.format(nextPrice)));
                        //Set the price as the maximum withdrawable from the player account
                        changingPriceString = df.format(balance);
                        notifyItem(0);
                        notifyItem(2);

                    } catch (NumberFormatException ignored) {
                        changingPriceString = df.format(previousPrice);
                        notifyItem(0);
                        notifyItem(2);
                    }
                }).build();
    }
}
