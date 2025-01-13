package dev.unnm3d.redistrade.guis;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.GuiSettings;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.core.OrderInfo;
import dev.unnm3d.redistrade.core.TradeSide;
import dev.unnm3d.redistrade.core.enums.Actor;
import dev.unnm3d.redistrade.core.enums.Status;
import dev.unnm3d.redistrade.core.enums.StatusActor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
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
    private final Actor actor;
    private ItemStack receiptItem;

    public TradeGuiBuilder(NewTrade trade, Actor actor) {
        this.trade = trade;
        this.actor = actor;
    }

    public Gui build() {
        final Structure structure = new Structure(GuiSettings.instance().tradeGuiStructure.toArray(new String[0]));

        initializeVirtualInventory(trade.getTradeSide(actor).getOrder().getVirtualInventory(), actor);
        initializeVirtualInventory(trade.getTradeSide(actor.opposite()).getOrder().getVirtualInventory(), actor.opposite());

        structure.addIngredient('L', trade.getOrderInfo(actor).getVirtualInventory())
                .addIngredient('R', trade.getOrderInfo(actor.opposite()).getVirtualInventory())
                .addIngredient('C', getConfirmButton(actor))
                .addIngredient('c', getConfirmButton(actor.opposite()))
                .addIngredient('D', getTradeCancelButton(actor))
                .addIngredient('x', GuiSettings.instance().separator.toItemBuilder());
        final Gui gui = Gui.normal()
                .setStructure(structure)
                .build();
        int i = 0;
        for (String allowedCurrency : Settings.instance().allowedCurrencies.keySet()) {
            gui.setItem(1 + i, new MoneyEditorButton(trade, actor, allowedCurrency));
            gui.setItem(7 - i, new MoneyEditorButton(trade, actor.opposite(), allowedCurrency));
            i++;
        }
        return gui;
    }

    private void initializeVirtualInventory(VirtualInventory virtualInventory, Actor actor) {
        if (virtualInventory.getPreUpdateHandler() == null) {
            virtualInventory.setPreUpdateHandler(event -> {
                if (virtualInventoryListener(event, actor)) {
                    event.setCancelled(true);
                } else {
                    trade.updateItem(event.getSlot(), event.getNewItem(), actor, true);
                }
            });
        }
        if (virtualInventory.getPostUpdateHandler() == null) {
            virtualInventory.setPostUpdateHandler(event -> trade.retrievedPhase(actor, actor.opposite()));
        }
    }

    /**
     * This method is called when an item is updated in the virtual inventory
     *
     * @param event The event that triggered the update
     * @return If the event should be cancelled
     */
    public boolean virtualInventoryListener(ItemPreUpdateEvent event, Actor actor) {
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
        final TradeSide operatingSide = trade.getTradeSide(actor);
        final TradeSide oppositeSide = trade.getTradeSide(actor.opposite());

        //If the trade is completed, the target can modify the trader inventory
        return switch (operatingSide.getOrder().getStatus()) {
            //If the trade is completed, the target can only move items out of the trade
            case COMPLETED -> !(editingPlayer.equals(oppositeSide.getTraderUUID()) && event.getNewItem() == null);
            case CONFIRMED, RETRIEVED -> true;
            //If the trade is not completed, the trader can modify the trader inventory
            case REFUSED -> !editingPlayer.equals(operatingSide.getTraderUUID());
        };
    }


    public Item getConfirmButton(Actor actor) {
        final OrderInfo orderInfo = trade.getOrderInfo(actor);

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
                if (trade.getViewerType(player.getUniqueId()) != actor) return;
                switch (orderInfo.getStatus()) {
                    case REFUSED ->
                            trade.changeAndSendStatus(StatusActor.valueOf(actor, Status.CONFIRMED), orderInfo.getStatus(), actor);
                    case CONFIRMED ->
                            trade.changeAndSendStatus(StatusActor.valueOf(actor, Status.REFUSED), orderInfo.getStatus(), actor);
                    case COMPLETED, RETRIEVED -> {
                    }
                }
            }
        };
    }

    public Item getTradeCancelButton(Actor actor) {
        return new AbstractItem() {
            @Override
            public ItemProvider getItemProvider(Player player) {
                return GuiSettings.instance().cancelTradeButton.toItemBuilder();
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                if (trade.getOrderInfo(actor).getStatus() != Status.REFUSED) return;

                //Self-trigger retrieve phase from both sides
                trade.retrievedPhase(actor, actor).thenAcceptAsync(result -> {
                    if (result) {
                        refundSide(trade, actor);
                        trade.retrievedPhase(actor.opposite(), actor.opposite()).thenAccept(result1 -> {
                            if (result1) {
                                refundSide(trade, actor.opposite());
                            }
                        });
                    }
                });
            }
        };
    }

    private void refundSide(NewTrade trade, Actor actor) {
        TradeSide side = trade.getTradeSide(actor);
        side.getOrder().getPrices().forEach((currency, price) -> {
            trade.setAndSendPrice(currency, 0, actor);
            RedisTrade.getInstance().getEconomyHook().depositPlayer(side.getTraderUUID(), price,
                    currency, "Trade cancellation");
            RedisTrade.debug(trade.getUuid() + " Refunded " + price + " " + currency + " to " + side.getTraderUUID());
        });
    }
}