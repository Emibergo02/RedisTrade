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

    private final Gui currentGui;
    private final ArchivedTrade archivedTrade;
    private final Player player;

    public ReviewGUI(ArchivedTrade archivedTrade, Player player) {
        this.archivedTrade = archivedTrade;
        this.player = player;
        Structure structure = new Structure(
          "#########",
          "##12345##",
          "#########")
          .addIngredient('1', createStarButton(1))
          .addIngredient('2', createStarButton(2))
          .addIngredient('3', createStarButton(3))
          .addIngredient('4', createStarButton(4))
          .addIngredient('5', createStarButton(5));
        this.currentGui = Gui.of(structure);
    }

    public static void open(UUID tradeUUID, Player player) {
        RedisTrade.getInstance().getDataStorage().getArchivedTrade(tradeUUID)
          .thenAccept(optTrade -> optTrade.ifPresent(trade -> {
              final ReviewGUI reviewGUI = new ReviewGUI(trade, player);
              RedisTrade.getInstance().getServer().getScheduler()
                .runTask(RedisTrade.getInstance(), reviewGUI::openWindow);
          }));

    }

    private void openWindow() {
        final Actor oppositeActor = archivedTrade.getTrade().getActor(player).opposite();
        Window.single().setGui(currentGui)
          .setTitle(GuiSettings.instance().ratingMenuTitle.replace("%player%", archivedTrade.getTrade()
            .getTradeSide(oppositeActor).getTraderName()))
          .addCloseHandler(() -> {
              player.getInventory().addItem(player.getItemOnCursor()).values().forEach(itemStack ->
                player.getWorld().dropItem(player.getLocation(), itemStack)
              );
              player.setItemOnCursor(null);
          })
          .open(player);
    }


    public Item createStarButton(int rating) {
        return new AbstractItem() {

            @Override
            public @NotNull ItemProvider getItemProvider() {
                return GuiSettings.instance().rateItem.toItemBuilder()
                  .replacePlaceholders(Map.of("%stars%", Utils.starsOf(rating), "%rating%", String.valueOf(rating)));
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                final Actor oppositeActor = archivedTrade.getTrade().getActor(player).opposite();
                if (!oppositeActor.isParticipant()) {
                    player.sendRichMessage(Messages.instance().noPermission);
                    return;
                }
                //Check if the review time has expired
                if (archivedTrade.getArchivedAt().toInstant().isBefore(
                  Instant.now().minusSeconds(Settings.instance().tradeReviewTime))) {
                    player.sendRichMessage(Messages.instance().noPermission);
                    return;
                }

                RedisTrade.getInstance().getDataStorage().rateTrade(archivedTrade.getTrade(), oppositeActor, rating);
                player.closeInventory();
            }
        };
    }
}
