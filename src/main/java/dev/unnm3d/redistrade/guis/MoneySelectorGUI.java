package dev.unnm3d.redistrade.guis;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.GuiSettings;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.core.enums.Actor;
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

import java.text.ParseException;
import java.util.List;
import java.util.Optional;

public class MoneySelectorGUI implements Listener {
    private final Gui currentGui;

    private final NewTrade trade;
    private final String currencyName;
    private final double previousPrice;
    private String changingPriceString;

    public MoneySelectorGUI(NewTrade trade, Actor actor, String currencyName) {
        this.trade = trade;
        this.currentGui = Gui.empty(3, 1);
        this.currencyName = currencyName;
        this.previousPrice = trade.getOrderInfo(actor).getPrice(currencyName);
        this.changingPriceString = Settings.getDecimalFormat().format(previousPrice);
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
        return new AbstractItem() {
            @Override
            public ItemProvider getItemProvider() {
                return GuiSettings.instance().moneyDisplay.toItemBuilder()
                        .setLegacyDisplayName(changingPriceString);
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                event.setCancelled(true);
            }
        };
    }

    private Item getConfirmDisplay() {
        return new AbstractItem() {

            @Override
            public ItemProvider getItemProvider() {
                return GuiSettings.instance().moneyConfirmButton.toItemBuilder()
                        .setMiniMessageItemName(Messages.instance().confirmMoneyDisplay
                                .replace("%amount%", changingPriceString)
                                .replace("%symbol%", RedisTrade.getInstance().getEconomyHook()
                                        .getCurrencySymbol(currencyName)));
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                try {
                    // Parse the next price and ensure it's positive
                    double nextPrice = Math.abs(Settings.getDecimalFormat().parse(changingPriceString).doubleValue());
                    double balance = RedisTrade.getInstance().getEconomyHook().getBalance(player.getUniqueId(), currencyName);
                    double priceDifference = previousPrice - nextPrice; // Calculate price difference

                    boolean transactionSuccessful = true;

                    // Handle price adjustment (deduction or refund)
                    if (priceDifference < 0) {
                        transactionSuccessful = RedisTrade.getInstance().getEconomyHook().withdrawPlayer(
                                player.getUniqueId(), Math.abs(priceDifference), currencyName, "Trade price");
                    } else if (priceDifference > 0) {
                        transactionSuccessful = RedisTrade.getInstance().getEconomyHook().depositPlayer(
                                player.getUniqueId(), priceDifference, currencyName, "Trade price");
                    }

                    // Proceed if the transaction was successful
                    if (transactionSuccessful) {
                        if (priceDifference != 0) {
                            trade.setAndSendPrice(currencyName, nextPrice, trade.getViewerType(player.getUniqueId()));
                        }

                        // Return the cursor item to the player's inventory or drop it if full
                        player.getInventory().addItem(player.getItemOnCursor()).values().forEach(itemStack ->
                                player.getWorld().dropItem(player.getLocation(), itemStack)
                        );
                        player.setItemOnCursor(null);

                        // Reopen the trade window
                        trade.openWindow(player.getUniqueId(), trade.getViewerType(player.getUniqueId()));
                        return;
                    }

                    // Notify the player if they lack sufficient funds
                    player.sendRichMessage(Messages.instance().notEnoughMoney
                            .replace("%amount%", Settings.getDecimalFormat().format(nextPrice)));

                    // Adjust the price string to reflect the player's maximum balance
                    changingPriceString = Settings.getDecimalFormat().format(balance);

                } catch (NumberFormatException | ParseException ignored) {
                    // Revert to the previous price on error
                    changingPriceString = Settings.getDecimalFormat().format(previousPrice);
                }

                //Update item display in the end
                notifyItem(0);
                notifyItem(2);
            }
        };
    }
}
