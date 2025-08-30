package dev.unnm3d.redistrade.guis;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.GuiSettings;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.core.enums.Actor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.window.AnvilWindow;

import java.text.ParseException;
import java.util.List;

public class MoneySelectorGUI extends MoneySelector {

    private final Gui currentGui;
    private final Item moneyDisplayItem;
    private final Item moneyConfirmButton;

    public MoneySelectorGUI(NewTrade trade, Actor playerSide, Player player, String currencyName) {
        super(trade, playerSide, player, currencyName);
        this.currentGui = Gui.empty(3, 1);
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
                        .setMiniMessageDisplayName(changingPriceString);
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
                                .replace("%symbol%", RedisTrade.getInstance().getIntegrationManager()
                                        .getCurrencyHook(currencyName).getCurrencySymbol()));
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player guiPlayer, @NotNull InventoryClickEvent event) {
                event.setCancelled(true);
                handleConfirm();
            }
        };
    }

    @Override
    public void openWindow() {
        AnvilWindow.single()
                .setRenameHandlers(List.of(stringRename -> {
                    this.changingPriceString = stringRename;
                    this.moneyConfirmButton.notifyWindows();
                }))
                .setGui(currentGui)
                .setCloseable(true)
                .setTitle(new AdventureComponentWrapper(MiniMessage.miniMessage().deserialize(
                        GuiSettings.instance().moneyEditorTitle.replace("%currency%", currencyName))))
                .addCloseHandler(this::handleClose)
                .open(player);
    }

    public void handleConfirm() {
        try {
            double nextPrice = parseNextPrice();
            double balance = RedisTrade.getInstance()
                    .getIntegrationManager().getCurrencyHook(currencyName)
                    .getBalance(player.getUniqueId());
            double priceDifference = previousPrice - nextPrice;

            if (processTransaction(priceDifference)) {
                if (priceDifference != 0) {
                    trade.setAndSendPrice(currencyName, nextPrice, playerSide);
                }
                player.closeInventory();
                return;
            }

            notifyInsufficientFunds(nextPrice, balance + previousPrice);

        } catch (NumberFormatException | ParseException ignored) {
            changingPriceString = Messages.instance().invalidFormat;
        }

        notifyButtons();
    }

    private void notifyButtons() {
        this.moneyDisplayItem.notifyWindows();
        this.moneyConfirmButton.notifyWindows();
    }

    private double parseNextPrice() throws ParseException, NumberFormatException {
        if (changingPriceString.equalsIgnoreCase("NaN")) {
            throw new NumberFormatException("NaN is not a supported notation");
        }
        return Math.abs(Settings.getDecimalFormat().parse(changingPriceString).doubleValue());
    }

    public void handleClose() {
        player.getInventory().addItem(player.getItemOnCursor()).values().forEach(itemStack ->
                player.getWorld().dropItem(player.getLocation(), itemStack)
        );
        player.setItemOnCursor(null);
        // Delay the reopening of the trade window to avoid skipping the modifications made up in this method
        RedisTrade.getInstance().getServer().getScheduler().runTaskLater(RedisTrade.getInstance(), () ->
                trade.openWindow(player, playerSide), 1);

    }
}