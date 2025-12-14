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
import dev.unnm3d.redistrade.guis.buttons.CancelButton;
import dev.unnm3d.redistrade.guis.buttons.MoneyEditorButton;
import dev.unnm3d.redistrade.guis.buttons.ProfileDisplay;
import dev.unnm3d.redistrade.guis.buttons.ReviewButton;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.inventory.VirtualInventory;
import xyz.xenondevs.invui.inventory.event.ItemPreUpdateEvent;
import xyz.xenondevs.invui.inventory.event.PlayerUpdateReason;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.AbstractItem;

@Setter
@Getter
public final class TradeGuiBuilder {
    private final NewTrade trade;
    private final Actor actorSide;
    private ItemStack receiptItem;

    public TradeGuiBuilder(NewTrade trade, Actor actorSide) {
        this.trade = trade;
        this.actorSide = actorSide;
    }

    public MutableGui build() {

        initializeVirtualInventory(actorSide);
        initializeVirtualInventory(actorSide.opposite());
        TradeSide actorTradeSide = trade.getTradeSide(actorSide);
        TradeSide oppositeTradeSide = trade.getTradeSide(actorSide.opposite());

        final MutableGui gui = new MutableGui(GuiSettings.instance().tradeGuiStructure.toArray(new String[0]))
          .setIngredient('L', actorTradeSide.getOrder().getVirtualInventory())
          .setIngredient('R', oppositeTradeSide.getOrder().getVirtualInventory())
          .setIngredient('C', getConfirmButton(actorSide))
          .setIngredient('c', getConfirmButton(actorSide.opposite()))
          .setIngredient('D', new CancelButton(trade, actorSide))
          .setIngredient('v', new ProfileDisplay(oppositeTradeSide))
          .setIngredient('V', new ReviewButton(trade.getUuid(), oppositeTradeSide))
          //Set the money editor buttons and rating GUI as background by default
          .setIngredients("MNOPQmnopqrx", GuiSettings.instance().separator.toItemBuilder());
        int i = 'M'; //Cycle MNOPQ... and mnopq... as many currencies are allowed
        for (String currencyName : RedisTrade.getInstance().getIntegrationManager().getCurrencyNames()) {
            gui.setIngredient((char) i, new MoneyEditorButton(trade, actorSide, currencyName));
            //i+32 translates the upper case to lower case ascii
            gui.setIngredient((char) (i + 32), new MoneyEditorButton(trade, actorSide.opposite(), currencyName));
            i++;
        }
        return gui;
    }

    private void initializeVirtualInventory(Actor actorSide) {
        final VirtualInventory virtualInventory = trade.getTradeSide(actorSide).getOrder().getVirtualInventory();
        if (virtualInventory.getPreUpdateHandler() == null) {
            virtualInventory.setPreUpdateHandler(event -> {
                if (virtualInventoryListener(event, actorSide)) {
                    event.setCancelled(true);
                } else {
                    checkXpBottle(event, actorSide);
                    trade.updateItem(event.getSlot(), event.getNewItem(), actorSide, true);
                }
            });
        }
        if (virtualInventory.getPostUpdateHandler() == null) {
            virtualInventory.setPostUpdateHandler(event -> trade.retrievedPhase(actorSide, actorSide.opposite()));
        }
    }

    private void checkXpBottle(@NotNull ItemPreUpdateEvent event, Actor actorSide) {
        if (!(event.getUpdateReason() instanceof PlayerUpdateReason pur)) return;
        if (event.getPreviousItem() == null) return;
        Integer xpAmount = event.getPreviousItem().getItemMeta().getPersistentDataContainer()
          .get(new NamespacedKey("redistrade", "xp"), PersistentDataType.INTEGER);
        if (xpAmount == null) return;
        pur.getPlayer().giveExp(xpAmount);
        pur.getPlayer().playSound(pur.getPlayer(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

        TradeSide actorTradeSide = trade.getTradeSide(actorSide);
        trade.setAndSendPrice("xp", Math.max(actorTradeSide.getOrder().getPrice("xp") - xpAmount, 0), actorSide);
        actorTradeSide.getOrder().getVirtualInventory().setItemSilently(event.getSlot(), null);

        //This is needed to update the item correctly outside this method
        event.setNewItem(null);
        event.setCancelled(true);
    }

    /**
     * This method is called when an item is updated in the virtual inventory
     *
     * @param event The event that triggered the update
     * @return If the event should be cancelled
     */
    public boolean virtualInventoryListener(ItemPreUpdateEvent event, Actor actorSide) {
        if (!(event.getUpdateReason() instanceof PlayerUpdateReason playerUpdateReason)) return false;
        //Check integrity system
        if (RedisTrade.getInstance().getIntegritySystem().isFaulted()) {
            playerUpdateReason.getPlayer().sendRichMessage(Messages.instance().newTradesLock);
            return true;
        }
        //Check invalid items
        if (event.getNewItem() != null) {
            final String itemTypeKey = event.getNewItem().getType().getKey().toString(); // example: "minecraft:diamond_sword"
            final String itemAsString = itemTypeKey + event.getNewItem().getItemMeta().getAsComponentString(); // results in: "minecraft:diamond_sword[minecraft:damage=53]"
            for (Settings.BlacklistedItemRegex blacklistedRegex : Settings.instance().blacklistedItemRegexes) {
                boolean matches = blacklistedRegex.containsOnly()
                  ? itemAsString.contains(blacklistedRegex.regex())
                  : itemAsString.matches(blacklistedRegex.regex());

                if (matches) {
                    playerUpdateReason.getPlayer().sendRichMessage(Messages.instance().blacklistedItem);
                    return true;
                }
            }
        }
        //Check distance
        if (RedisTrade.getInstance().getTradeManager().checkInvalidDistance(playerUpdateReason.getPlayer(), trade.getTradeSide(actorSide.opposite()).getTraderUUID())) {
            playerUpdateReason.getPlayer().sendRichMessage(Messages.instance().tradeDistance
              .replace("%blocks%", String.valueOf(Settings.instance().tradeDistance)));
            return true;
        }

        final Actor tradeActor = trade.getActor(playerUpdateReason.getPlayer());
        final TradeSide operatingSide = trade.getTradeSide(actorSide);

        //If the trade is completed, the target can modify the trader inventory
        return switch (operatingSide.getOrder().getStatus()) {
            //If the trade is completed, the target can only move items out of the trade
            case COMPLETED -> !(actorSide.opposite().isSideOf(tradeActor) && event.getNewItem() == null);
            //Only admin can modify in confirmed phase
            case CONFIRMED -> tradeActor != Actor.ADMIN;
            case RETRIEVED -> true;
            //If the trade is not completed, the trader can modify the trader inventory
            //Admin can modify both sides
            case REFUSED -> !actorSide.isSideOf(tradeActor);
        };
    }


    public Item getConfirmButton(Actor confirmActorSide) {
        final OrderInfo orderInfo = trade.getTradeSide(confirmActorSide).getOrder();

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
                final Actor actor = trade.getActor(player);
                if (!confirmActorSide.isSideOf(actor)) return;
                switch (orderInfo.getStatus()) {
                    case REFUSED -> {
                        //Check distance
                        if (RedisTrade.getInstance().getTradeManager().checkInvalidDistance(player, trade.getTradeSide(actor.opposite()).getTraderUUID())) {
                            player.sendRichMessage(Messages.instance().tradeDistance
                              .replace("%blocks%", String.valueOf(Settings.instance().tradeDistance)));
                            return;
                        }
                        trade.changeAndSendStatus(StatusActor.valueOf(confirmActorSide, Status.CONFIRMED), orderInfo.getStatus(), confirmActorSide);
                    }
                    case CONFIRMED ->
                      trade.changeAndSendStatus(StatusActor.valueOf(confirmActorSide, Status.REFUSED), orderInfo.getStatus(), confirmActorSide);
                    case COMPLETED, RETRIEVED -> {
                    }
                }
            }
        };
    }
}