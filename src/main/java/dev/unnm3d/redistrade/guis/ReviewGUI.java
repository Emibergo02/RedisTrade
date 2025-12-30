package dev.unnm3d.redistrade.guis;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.GuiSettings;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.ArchivedTrade;
import dev.unnm3d.redistrade.core.enums.Actor;
import dev.unnm3d.redistrade.utils.Utils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.structure.Structure;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.window.Window;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class ReviewGUI {

    public static void open(UUID tradeUUID, Player player) {
        RedisTrade.getInstance().getDataStorage().getArchivedTrade(tradeUUID).thenAccept(optTrade ->
          optTrade.ifPresent(archivedTrade -> {
              Actor opposite = archivedTrade.getTrade().getActor(player).opposite();
              String title = GuiSettings.instance().ratingMenuTitle.replace(
                "%player%", archivedTrade.getTrade().getTradeSide(opposite).getTraderName());

              Window.Builder.Normal.Single window = Window.single()
                .setGui(Gui.of(createStructure(archivedTrade)))
                .setTitle(title)
                .addCloseHandler(() -> {
                    var cursorItem = player.getItemOnCursor();
                    player.getInventory().addItem(cursorItem).values()
                      .forEach(item -> player.getWorld().dropItem(player.getLocation(), item));
                    player.setItemOnCursor(null);
                });

              player.getScheduler().run(RedisTrade.getInstance(), task -> window.open(player), null);
          })
        );
    }

    private static Structure createStructure(ArchivedTrade archived) {
        return new Structure(
          "#########",
          "##12345##",
          "#########")
          .addIngredient('1', createStarButton(archived,1))
          .addIngredient('2', createStarButton(archived,2))
          .addIngredient('3', createStarButton(archived,3))
          .addIngredient('4', createStarButton(archived,4))
          .addIngredient('5', createStarButton(archived,5));
    }

    private static Item createStarButton(ArchivedTrade archived,int rating) {
        return new AbstractItem() {

            @Override
            public @NotNull ItemProvider getItemProvider() {
                return GuiSettings.instance().rateItem.toItemBuilder()
                  .replacePlaceholders(Map.of("%stars%", Utils.starsOf(rating), "%rating%", String.valueOf(rating)));
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                final Actor oppositeActor = archived.getTrade().getActor(player).opposite();
                if (!oppositeActor.isParticipant()) {
                    player.sendRichMessage(Messages.instance().noPermission);
                    return;
                }
                //Check if the review time has expired
                if (archived.getArchivedAt().toInstant().isBefore(
                  Instant.now().minusSeconds(Settings.instance().tradeReviewTime))) {
                    player.sendRichMessage(Messages.instance().noPermission);
                    return;
                }

                RedisTrade.getInstance().getDataStorage().rateTrade(archived.getTrade(), oppositeActor, rating);
                player.closeInventory();
            }
        };
    }
}
