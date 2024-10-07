package dev.unnm3d.redistrade.guis;

import dev.unnm3d.redistrade.Settings;
import dev.unnm3d.redistrade.objects.NewTrade;
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
    private NewTrade trade;
    private boolean isTrader;

    public MoneyButton(NewTrade trade, boolean isTrader) {
        super(Settings.instance().buttons.get(Settings.ButtonType.MONEY_BUTTON));
        this.trade = trade;
        this.isTrader = isTrader;
    }

    @Override
    public ItemProvider getItemProvider() {
        final OrderInfo orderInfo = isTrader ? trade.getTraderSideInfo() : trade.getTargetSideInfo();

        return new ItemBuilder(Settings.instance().buttons.get(Settings.ButtonType.MONEY_BUTTON))
                .setDisplayName("Current price: " + orderInfo.getProposed());
    }


    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
        new MoneySelectorGUI(trade, isTrader, 0, player);
    }
}
