package dev.unnm3d.redistrade.guis.buttons;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.GuiSettings;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.core.TradeSide;
import dev.unnm3d.redistrade.core.enums.Actor;
import dev.unnm3d.redistrade.core.enums.Status;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.inventory.VirtualInventory;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.AbstractItem;

@AllArgsConstructor
public class CancelButton extends AbstractItem {

    private final NewTrade trade;
    private final Actor actorSide;

    @Override
    public ItemProvider getItemProvider(Player player) {
        if (trade.getTradeSide(actorSide).getOrder().getStatus() == Status.REFUSED) {
            return GuiSettings.instance().cancelTradeButton.toItemBuilder();
        }
        return GuiSettings.instance().getAllItems.toItemBuilder();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        TradeSide actorTradeSide = trade.getTradeSide(actorSide);
        if (actorTradeSide.getOrder().getStatus() == Status.REFUSED) {
            //Self-trigger retrieve phase from both sides
            short returnedItems = returnItems(player, actorSide);
            RedisTrade.debug(trade.getUuid() + " Returned " + returnedItems + " items to " + player.getName());
            trade.retrievedPhase(actorSide, actorSide).thenAcceptAsync(result -> {
                if (result) {
                    refundSide(trade, actorSide);
                    trade.retrievedPhase(actorSide.opposite(), actorSide.opposite()).thenAccept(result1 -> {
                        if (result1) {
                            refundSide(trade, actorSide.opposite());
                        }
                    });
                }
            });
            return;
        }

        TradeSide oppositeTradeSide = trade.getTradeSide(actorSide.opposite());
        if (oppositeTradeSide.getOrder().getStatus() == Status.COMPLETED) {
            short returnedItems = returnItems(player, actorSide.opposite());
            trade.retrievedPhase(actorSide.opposite(), actorSide);
            RedisTrade.debug(trade.getUuid() + " Returned " + returnedItems + " items to " + player.getName());
        }
    }

    /**
     * Returns the items of the trade to the player
     *
     * @param player    The player to return the items to
     * @param tradeSide The side of the trade to return the items from
     * @return The amount of items returned
     */
    private short returnItems(Player player, Actor tradeSide) {
        short returnedItems = 0;
        if (RedisTrade.getInstance().getIntegritySystem().isFaulted()) {
            player.sendRichMessage(Messages.instance().newTradesLock);
            return returnedItems;
        }
        Actor actor = trade.getActor(player);
        if (!actor.isParticipant()) return returnedItems;

        final VirtualInventory traderInventory = trade.getTradeSide(tradeSide).getOrder().getVirtualInventory();
        for (int i = 0; i < traderInventory.getItems().length; i++) {
            final ItemStack item = traderInventory.getItem(i);
            if (item == null) continue;
            traderInventory.setItemSilently(i, null);
            trade.updateItem(i, null, tradeSide, true);
            player.getInventory().addItem(item).forEach((slot, itemStack) ->
                    player.getWorld().dropItem(player.getLocation(), itemStack));
            returnedItems++;
        }
        return returnedItems;
    }

    private void refundSide(NewTrade trade, Actor refundActorSide) {
        TradeSide side = trade.getTradeSide(refundActorSide);
        side.getOrder().getPrices().forEach((currency, price) -> {
            trade.setAndSendPrice(currency, 0, refundActorSide);
            RedisTrade.getInstance().getEconomyHook().depositPlayer(side.getTraderUUID(), price,
                    currency, "Trade cancellation");
            RedisTrade.debug(trade.getUuid() + " Refunded " + price + " " + currency + " to " + side.getTraderUUID());
        });
    }
}
