package dev.unnm3d.redistrade.guis;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.GuiSettings;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.core.enums.Actor;
import org.bukkit.entity.Player;
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

public class MoneySelectorGUI {
    private final NewTrade trade;
    private final Actor playerSide;
    private final Player player;
    private final String currencyName;

    private final Gui currentGui;

    private final double previousPrice;
    private String changingPriceString;
    private final Item moneyDisplayItem;
    private final Item moneyConfirmButton;

    public MoneySelectorGUI(NewTrade trade, Actor playerSide, Player player, String currencyName) {
        this.trade = trade;
        this.playerSide = playerSide;
        this.player = player;
        this.currencyName = currencyName;
        this.currentGui = Gui.empty(3, 1);
        this.previousPrice = trade.getTradeSide(playerSide).getOrder().getPrice(currencyName);
        this.changingPriceString = Settings.getDecimalFormat().format(previousPrice);
        this.moneyDisplayItem = createMoneyDisplayItem();
        this.moneyConfirmButton = createConfirmButtonItem();

        currentGui.setItem(0, this.moneyDisplayItem);
        currentGui.setItem(2, this.moneyConfirmButton);
    }

    /**
     * Opens the money selector GUI for the specified trade, player and currency.
     * The player could be Actor.ADMIN so we need playerSide to determine the side
     *
     * @param trade        The trade to open the money selector GUI for
     * @param playerSide   The side of the player in the trade
     * @param player       The player to open the money selector GUI for
     * @param currencyName The name of the currency to edit the price for
     */
    public static void open(NewTrade trade, Actor playerSide, Player player, String currencyName) {
        new MoneySelectorGUI(trade, playerSide, player, currencyName).openWindow();
    }

    private AbstractItem createMoneyDisplayItem() {
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

    private AbstractItem createConfirmButtonItem() {
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
            public void handleClick(@NotNull ClickType clickType, @NotNull Player guiPlayer, @NotNull InventoryClickEvent event) {
                event.setCancelled(true);
                handleConfirm();
            }
        };
    }

    private void openWindow() {
        AnvilWindow.single()
                .setRenameHandlers(List.of(stringRename -> {
                    this.changingPriceString = stringRename;
                    this.moneyConfirmButton.notifyWindows();
                }))
                .setGui(currentGui)
                .setTitle("Money editor")
                .setCloseable(true)
                .addCloseHandler(this::handleClose)
                .open(player);
    }

    private void handleConfirm() {
        try {
            double nextPrice = parseNextPrice();
            double balance = RedisTrade.getInstance().getEconomyHook().getBalance(player.getUniqueId(), currencyName);
            double priceDifference = previousPrice - nextPrice;

            if (processTransaction(priceDifference)) {
                if (priceDifference != 0) {
                    trade.setAndSendPrice(currencyName, nextPrice, playerSide);
                }
                trade.openWindow(player, playerSide);
                return;
            }

            notifyInsufficientFunds(nextPrice, balance + previousPrice);

        } catch (NumberFormatException | ParseException ignored) {
            changingPriceString = Settings.getDecimalFormat().format(previousPrice);
        }

        this.moneyDisplayItem.notifyWindows();
        this.moneyConfirmButton.notifyWindows();
    }

    private double parseNextPrice() throws ParseException, NumberFormatException {
        if (changingPriceString.equalsIgnoreCase("NaN")) {
            throw new NumberFormatException("NaN is not a supported notation");
        }
        return Math.abs(Settings.getDecimalFormat().parse(changingPriceString).doubleValue());
    }

    private boolean processTransaction(double priceDifference) {
        if (priceDifference < 0) {
            return RedisTrade.getInstance().getEconomyHook().withdrawPlayer(
                    player.getUniqueId(), Math.abs(priceDifference), currencyName, "Trade price");
        } else if (priceDifference > 0) {
            return RedisTrade.getInstance().getEconomyHook().depositPlayer(
                    player.getUniqueId(), priceDifference, currencyName, "Trade price");
        }
        return true;
    }

    private void notifyInsufficientFunds(double nextPrice, double balance) {
        player.sendRichMessage(Messages.instance().notEnoughMoney
                .replace("%amount%", Settings.getDecimalFormat().format(nextPrice)));
        changingPriceString = Settings.getDecimalFormat().format(balance);
    }

    private void handleClose() {
        player.getInventory().addItem(player.getItemOnCursor()).values().forEach(itemStack ->
                player.getWorld().dropItem(player.getLocation(), itemStack)
        );
        player.setItemOnCursor(null);
    }
}