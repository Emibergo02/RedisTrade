package dev.unnm3d.redistrade.guis;

import dev.unnm3d.redistrade.Settings;
import dev.unnm3d.redistrade.guis.maingui.AbstractTradeGui;
import dev.unnm3d.redistrade.guis.maingui.TraderGui;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;

@Getter
@Setter
public class MoneyButton extends SimpleItem {
    private AbstractTradeGui gui;

    public MoneyButton(AbstractTradeGui gui) {
        super(Settings.instance().buttons.get(Settings.ButtonType.MONEY_BUTTON));
        this.gui = gui;
    }

    @Override
    public ItemProvider getItemProvider() {
        if (gui instanceof TraderGui) {
            return new ItemBuilder(Settings.instance().buttons.get(Settings.ButtonType.MONEY_BUTTON))
                    .setDisplayName("Current price: " + gui.getTrade().getTraderSideInfo().getProposed());
        }

        return new ItemBuilder(Settings.instance().buttons.get(Settings.ButtonType.MONEY_BUTTON))
                .setDisplayName("Current price: " + gui.getTrade().getTargetSideInfo().getProposed());
    }


    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
        new MoneySelectorGUI(gui, 0, player);
    }
}
