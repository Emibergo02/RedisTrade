package dev.unnm3d.redistrade.guis;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.core.OrderInfo;
import dev.unnm3d.redistrade.utils.Utils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.AbstractItem;

public class MoneyEditorButton extends AbstractItem {
    private final NewTrade trade;
    private final boolean isTrader;
    private final String currencyName;

    public MoneyEditorButton(NewTrade trade, boolean isTrader, String currencyName) {
        this.trade = trade;
        this.isTrader = isTrader;
        this.currencyName = currencyName;
    }

    @Override
    public ItemProvider getItemProvider() {
        double amount = trade.getOrderInfo(isTrader).getPrices().getOrDefault(currencyName, 0.0);

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
        if (trade.getOrderInfo(isTrader).getStatus() != OrderInfo.Status.REFUSED) return;
        if (!player.hasPermission("redistrade.usecurrency." + currencyName)) {
            player.sendRichMessage(Messages.instance().noPermission);
            return;
        }
        new MoneySelectorGUI(trade, player, currencyName);

    }
}
