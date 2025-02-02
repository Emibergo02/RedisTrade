package dev.unnm3d.redistrade.guis.buttons;

import dev.unnm3d.redistrade.configs.GuiSettings;
import dev.unnm3d.redistrade.core.TradeSide;
import dev.unnm3d.redistrade.core.enums.Status;
import dev.unnm3d.redistrade.guis.ReviewGUI;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.AbstractItem;

import java.util.UUID;

@AllArgsConstructor
public class ReviewButton extends AbstractItem {

    private final UUID tradeUUID;
    private final TradeSide tradeSide;

    @Override
    public ItemProvider getItemProvider() {
        if (tradeSide.getOrder().getStatus() == Status.RETRIEVED) {
            return GuiSettings.instance().openRatingMenu.toItemBuilder();
        }
        return GuiSettings.instance().separator.toItemBuilder();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (tradeSide.getOrder().getStatus() == Status.RETRIEVED) {
            ReviewGUI.open(tradeUUID, player);
        }
    }
}
