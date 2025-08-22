package dev.unnm3d.redistrade.guis.buttons;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.GuiSettings;
import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.core.enums.Actor;
import dev.unnm3d.redistrade.core.enums.Status;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.AbstractItem;

@AllArgsConstructor
public class CancelButton extends AbstractItem {

    private final NewTrade trade;
    private final Actor actorSide;

    @Override
    public ItemProvider getItemProvider(Player player) {
        Status currentStatus = trade.getTradeSide(actorSide).getOrder().getStatus();
        if (currentStatus == Status.REFUSED || currentStatus == Status.CONFIRMED) {
            return GuiSettings.instance().cancelTradeButton.toItemBuilder();
        }
        return GuiSettings.instance().getAllItems.toItemBuilder();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        RedisTrade.getInstance().getTradeManager().collectItemsAndClose(player, trade.getUuid());
    }
}
