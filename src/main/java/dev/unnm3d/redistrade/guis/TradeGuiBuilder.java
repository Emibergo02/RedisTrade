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
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.structure.Structure;
import xyz.xenondevs.invui.inventory.VirtualInventory;
import xyz.xenondevs.invui.inventory.event.ItemPreUpdateEvent;
import xyz.xenondevs.invui.inventory.event.PlayerUpdateReason;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.AbstractItem;

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
        if (virtualInventory.getPreUpdateHandler() == null) {
            virtualInventory.setPreUpdateHandler(event -> {
                if (virtualInventoryListener(event, viewerType)) {
                    event.setCancelled(true);
                } else {
                    trade.updateItem(event.getSlot(), event.getNewItem(), viewerType, true);
                }
            });
        }
        if (virtualInventory.getPostUpdateHandler() == null) {
            virtualInventory.setPostUpdateHandler(event -> trade.retrievedPhase(viewerType, viewerType.opposite()));
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
        final UUID editingPlayer = playerUpdateReason.getPlayer().getUniqueId();
        if (RedisTrade.getInstance().getIntegritySystem().isFaulted()) {
            playerUpdateReason.getPlayer().sendRichMessage(Messages.instance().newTradesLock);
            return true;
        }

        if (event.getNewItem() != null)
            for (Settings.BlacklistedItem blacklistedItem : Settings.instance().blacklistedItems) {
                if (blacklistedItem.isSimilar(event.getNewItem())) {
                    playerUpdateReason.getPlayer().sendRichMessage(Messages.instance().blacklistedItem);
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
        final OrderInfo orderInfo = trade.getOrderInfo(viewerType);

        return new AbstractItem() {
            @Override
            public ItemProvider getItemProvider(Player player) {
                return switch (orderInfo.getStatus()) {
                    case REFUSED -> GuiSettings.instance().refuseButton.toItemBuilder();
                    case CONFIRMED -> GuiSettings.instance().confirmButton.toItemBuilder();
                    case COMPLETED -> GuiSettings.instance().completedButton.toItemBuilder();
                    case RETRIEVED -> GuiSettings.instance().retrievedButton.toItemBuilder();
                };
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                if (trade.getViewerType(player.getUniqueId()) != viewerType) return;
                switch (orderInfo.getStatus()) {
                    case REFUSED ->
                            trade.changeAndSendStatus(OrderInfo.Status.CONFIRMED, orderInfo.getStatus(), viewerType);
                    case CONFIRMED ->
                            trade.changeAndSendStatus(OrderInfo.Status.REFUSED, orderInfo.getStatus(), viewerType);
                    case COMPLETED, RETRIEVED -> {
                    }
                }
            }
        };
    }

    public Item getTradeCancelButton(ViewerType viewerType) {
        return new AbstractItem() {
            @Override
            public ItemProvider getItemProvider(Player player) {
                return GuiSettings.instance().cancelTradeButton.toItemBuilder();
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
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
            }
        };
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