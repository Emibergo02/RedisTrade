package dev.unnm3d.redistrade.guis;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.GuiSettings;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.core.OrderInfo;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.AbstractGui;
import xyz.xenondevs.invui.gui.structure.Structure;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SuppliedItem;

@Setter
@Getter
public final class TradeGuiImpl extends AbstractGui {
    private final NewTrade trade;
    private final boolean isTraderGui;

    public TradeGuiImpl(NewTrade trade, boolean isTraderGui, @NotNull Structure structure) {
        super(structure.getWidth(), structure.getHeight());
        structure.addIngredient('L', trade.getOrderInfo(isTraderGui).getVirtualInventory())
                .addIngredient('R', trade.getOrderInfo(!isTraderGui).getVirtualInventory())
                .addIngredient('x', GuiSettings.instance().separator.toItemBuilder());
        applyStructure(structure);
        this.trade = trade;
        this.isTraderGui = isTraderGui;

        setItem(0, getConfirmButton(isTraderGui));
        setItem(8, getConfirmButton(!isTraderGui));
        setItem(4, getTradeCancelButton());

        int i = 0;
        for (String allowedCurrency : Settings.instance().allowedCurrencies.keySet()) {
            setItem(1 + i, new MoneyEditorButton(trade, isTraderGui, allowedCurrency));
            setItem(7 - i, new MoneyEditorButton(trade, !isTraderGui, allowedCurrency));
            i++;
        }
    }


    public Item getConfirmButton(boolean isTraderConfirmation) {
        OrderInfo orderInfo = trade.getOrderInfo(isTraderConfirmation);
        return new SuppliedItem(() ->
                switch (orderInfo.getStatus()) {
                    case REFUSED -> GuiSettings.instance().refuseButton.toItemBuilder();
                    case CONFIRMED -> GuiSettings.instance().confirmButton.toItemBuilder();
                    case COMPLETED -> GuiSettings.instance().completedButton.toItemBuilder();
                    case RETRIEVED -> GuiSettings.instance().retrievedButton.toItemBuilder();
                }, (inventoryClickEvent) -> switch (orderInfo.getStatus()) {
            case REFUSED -> {
                trade.changeAndSendStatus(OrderInfo.Status.CONFIRMED, orderInfo.getStatus(), isTraderConfirmation);
                yield true;
            }
            case CONFIRMED -> {
                trade.changeAndSendStatus(OrderInfo.Status.REFUSED, orderInfo.getStatus(), isTraderConfirmation);
                yield true;
            }
            case COMPLETED, RETRIEVED -> false;
        });
    }

    public Item getTradeCancelButton() {
        return new AbstractItem() {
            @Override
            public ItemProvider getItemProvider() {
                return GuiSettings.instance().cancelTradeButton.toItemBuilder();
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                // Self cancel the side of the player and cancel the other side from the other player
                // Players should have their side empty to trigger retrieved phase
                trade.retrievedPhase(false, false);
                trade.retrievedPhase(true, true);

                // Refund the player (this is only if the trade is cancelled)
                trade.getOrderInfo(isTraderGui).getPrices().forEach((currency, price) -> {
                    trade.setAndSendPrice(currency, 0, isTraderGui);
                    RedisTrade.getInstance().getEconomyHook().depositPlayer(player.getUniqueId(), price,
                            currency, "Trade cancellation");

                });
            }
        };
    }

    public void notifyConfirm(boolean isTraderConfirmButton) {
        Item item = getItem(isTraderConfirmButton == isTraderGui ? 0 : 8);
        if (item != null) {
            item.notifyWindows();
        }
    }

    public void notifyMoneyButton(boolean isTraderMoneyButton) {
        int startindex = isTraderMoneyButton == isTraderGui ? 1 : 5;
        for (int i = startindex; i < startindex + 3; i++) {
            Item item = getItem(i);
            if (item != null) {
                item.notifyWindows();
            }
        }
    }

    @Override
    public void handleClick(int slotNumber, Player player, ClickType clickType, InventoryClickEvent event) {
        int x = slotNumber % 9;
        int y = slotNumber / 9;

        if (clickType == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            return;
        }
        if (x > 4 && y == 0) {
            event.setCancelled(true);
            return;
        }
        //Player can't cancel a trade that is not in the refused phase
        if (trade.getOrderInfo(isTraderGui).getStatus() != OrderInfo.Status.REFUSED && x == 4) {
            event.setCancelled(true);
            return;
        }
        super.handleClick(slotNumber, player, clickType, event);
    }

}