package dev.unnm3d.redistrade.guis.buttons;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.GuiSettings;
import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.core.enums.Actor;
import dev.unnm3d.redistrade.core.enums.Status;
import dev.unnm3d.redistrade.core.enums.StatusActor;
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
        return switch (trade.getTradeSide(actorSide).getOrder().getStatus()) {
            case REFUSED -> GuiSettings.instance().cancelTradeButton.toItemBuilder();
            case CONFIRMED -> GuiSettings.instance().cancelConfirmedButton.toItemBuilder();
            default -> GuiSettings.instance().getAllItems.toItemBuilder();
        };
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (getSideStatus() == Status.CONFIRMED) {
            trade.changeAndSendStatus(StatusActor.valueOf(actorSide, Status.REFUSED),
                    getSideStatus(),
                    actorSide);
            return;
        }
        RedisTrade.getInstance().getTradeManager().collectItemsAndClose(player, trade.getUuid());
    }

    private Status getSideStatus() {
        return trade.getTradeSide(actorSide).getOrder().getStatus();
    }
}
