package dev.unnm3d.redistrade.guis;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.GuiSettings;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.core.OrderInfo;
import dev.unnm3d.redistrade.core.TradeSide;
import lombok.Getter;
import lombok.Setter;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.Structure;
import xyz.xenondevs.invui.inventory.VirtualInventory;
import xyz.xenondevs.invui.inventory.event.ItemPreUpdateEvent;
import xyz.xenondevs.invui.inventory.event.PlayerUpdateReason;
import xyz.xenondevs.invui.item.Item;

import java.util.UUID;

@Setter
@Getter
public final class TradeGuiBuilder {
    private final NewTrade trade;
    private final ViewerType viewerType;

    public TradeGuiBuilder(NewTrade trade, ViewerType viewerType) {
        this.trade = trade;
        this.viewerType = viewerType;
    }

    public Gui build() {
        final Structure structure = new Structure(GuiSettings.instance().tradeGuiStructure.toArray(new String[0]));

        initializeVirtualInventory(trade.getTradeSide(viewerType).getOrder().getVirtualInventory(), viewerType);
        initializeVirtualInventory(trade.getTradeSide(viewerType.opposite()).getOrder().getVirtualInventory(), viewerType.opposite());

        structure.addIngredient('L', trade.getOrderInfo(viewerType).getVirtualInventory())
                .addIngredient('R', trade.getOrderInfo(viewerType.opposite()).getVirtualInventory())
                .addIngredient('C', getConfirmButton(viewerType))
                .addIngredient('c', getConfirmButton(viewerType.opposite()))
                .addIngredient('D', getTradeCancelButton(viewerType))
                .addIngredient('x', GuiSettings.instance().separator.toItemBuilder());
        final Gui gui = Gui.normal()
                .setStructure(structure)
                .build();
        int i = 0;
        for (String allowedCurrency : Settings.instance().allowedCurrencies.keySet()) {
            gui.setItem(1 + i, new MoneyEditorButton(trade, viewerType, allowedCurrency));
            gui.setItem(7 - i, new MoneyEditorButton(trade, viewerType.opposite(), allowedCurrency));
            i++;
        }
        return gui;
    }

    private void initializeVirtualInventory(VirtualInventory virtualInventory, ViewerType viewerType) {
        if (virtualInventory.getPreUpdateHandlers().isEmpty()) {
            virtualInventory.addPreUpdateHandler(event -> {
                if (virtualInventoryListener(event, viewerType)) {
                    event.setCancelled(true);
                } else {
                    trade.updateItem(event.getSlot(), event.getNewItem(), viewerType, true);
                }
            });
        }
        if (virtualInventory.getPostUpdateHandlers().isEmpty()) {
            virtualInventory.addPostUpdateHandler(event -> trade.retrievedPhase(viewerType, viewerType.opposite()));
        }
    }

    /**
     * This method is called when an item is updated in the virtual inventory
     *
     * @param event The event that triggered the update
     * @return If the event should be cancelled
     */
    public boolean virtualInventoryListener(ItemPreUpdateEvent event, ViewerType viewerType) {
        if (!(event.getUpdateReason() instanceof PlayerUpdateReason playerUpdateReason)) return false;
        final UUID editingPlayer = playerUpdateReason.player().getUniqueId();
        if (RedisTrade.getInstance().getIntegritySystem().isFaulted()) {
            playerUpdateReason.player().sendRichMessage(Messages.instance().newTradesLock);
            return true;
        }

        if (event.getNewItem() != null)
            for (Settings.BlacklistedItem blacklistedItem : Settings.instance().blacklistedItems) {
                if (blacklistedItem.isSimilar(event.getNewItem())) {
                    playerUpdateReason.player().sendRichMessage(Messages.instance().blacklistedItem);
                    return true;
                }
            }
        final TradeSide operatingSide = trade.getTradeSide(viewerType);
        final TradeSide oppositeSide = trade.getTradeSide(viewerType.opposite());

        //If the trade is completed, the target can modify the trader inventory
        return switch (operatingSide.getOrder().getStatus()) {
            case COMPLETED -> !editingPlayer.equals(oppositeSide.getTraderUUID());
            case CONFIRMED, RETRIEVED -> true;
            //If the trade is not completed, the trader can modify the trader inventory
            case REFUSED -> !editingPlayer.equals(operatingSide.getTraderUUID());
        };
    }


    public Item getConfirmButton(ViewerType viewerType) {
        OrderInfo orderInfo = trade.getOrderInfo(viewerType);
        return Item.builder().setItemProvider(player -> switch (orderInfo.getStatus()) {
            case REFUSED -> GuiSettings.instance().refuseButton.toItemBuilder();
            case CONFIRMED -> GuiSettings.instance().confirmButton.toItemBuilder();
            case COMPLETED -> GuiSettings.instance().completedButton.toItemBuilder();
            case RETRIEVED -> GuiSettings.instance().retrievedButton.toItemBuilder();
        }).addClickHandler((item, click) -> {
            if (trade.getViewerType(click.getPlayer().getUniqueId()) != viewerType) return;
            switch (orderInfo.getStatus()) {
                case REFUSED ->
                        trade.changeAndSendStatus(OrderInfo.Status.CONFIRMED, orderInfo.getStatus(), viewerType);
                case CONFIRMED ->
                        trade.changeAndSendStatus(OrderInfo.Status.REFUSED, orderInfo.getStatus(), viewerType);
                case COMPLETED, RETRIEVED -> {
                }
            }
        }).build();
    }

    public Item getTradeCancelButton(ViewerType viewerType) {
        return Item.builder().setItemProvider(player -> GuiSettings.instance().cancelTradeButton.toItemBuilder())
                .addClickHandler((item, click) -> {
                    if (trade.getOrderInfo(viewerType).getStatus() != OrderInfo.Status.REFUSED) return;

                    //Self-trigger retrieve phase from both sides
                    trade.retrievedPhase(viewerType, viewerType).thenAcceptAsync(result -> {
                        if (result) {
                            refundSide(trade, viewerType);
                            trade.retrievedPhase(viewerType.opposite(), viewerType.opposite()).thenAccept(result1 -> {
                                if (result1) {
                                    refundSide(trade, viewerType.opposite());
                                }
                            });
                        }
                    });
                }).build();
    }

    private void refundSide(NewTrade trade, ViewerType viewerType) {
        TradeSide side = trade.getTradeSide(viewerType);
        side.getOrder().getPrices().forEach((currency, price) -> {
            trade.setAndSendPrice(currency, 0, viewerType);
            RedisTrade.getInstance().getEconomyHook().depositPlayer(side.getTraderUUID(), price,
                    currency, "Trade cancellation");
        });
    }
}