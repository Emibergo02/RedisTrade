package dev.unnm3d.redistrade.guis.buttons;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.core.enums.Status;
import dev.unnm3d.redistrade.core.enums.Actor;
import dev.unnm3d.redistrade.guis.MoneySelectorGUI;
import dev.unnm3d.redistrade.utils.Permissions;
import dev.unnm3d.redistrade.utils.Utils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.AbstractItem;

public class MoneyEditorButton extends AbstractItem {
    private final NewTrade trade;
    private final Actor actorSide;
    private final String currencyName;

    public MoneyEditorButton(NewTrade trade, Actor actorSide, String currencyName) {
        this.trade = trade;
        this.actorSide = actorSide;
        this.currencyName = currencyName;
    }

    @Override
    public @NotNull ItemProvider getItemProvider() {
        double amount = trade.getTradeSide(actorSide).getOrder().getPrices().getOrDefault(currencyName, 0.0);

        return Settings.instance().allowedCurrencies.get(currencyName).toItemBuilder()
                .addMiniMessageLoreLines(Messages.instance().moneyButtonLore.stream()
                        .map(s -> s.replace("%currency%", currencyName)
                                .replace("%currency_display%", Settings.instance().allowedCurrencies.get(currencyName).displayName())
                                .replace("%amount%", Utils.parseDoubleFormat(amount))
                                .replace("%symbol%", RedisTrade.getInstance().getEconomyHook().getCurrencySymbol(currencyName)))
                        .toArray(String[]::new));
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        //Player can only edit money in the first phase
        if (trade.getTradeSide(actorSide).getOrder().getStatus() != Status.REFUSED) return;
        //Be sure that the player isn't modifying the other side
        if (!actorSide.isSideOf(trade.getActor(player))) return;
        if (!player.hasPermission(Permissions.URE_CURRENCY_PREFIX.getPermission() + currencyName)) {
            player.sendRichMessage(Messages.instance().noPermission);
            return;
        }
        MoneySelectorGUI.open(trade, actorSide, player, currencyName);
    }
}
