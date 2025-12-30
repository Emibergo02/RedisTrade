package dev.unnm3d.redistrade.guis;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.core.enums.Actor;
import org.bukkit.entity.Player;

public abstract class MoneySelector {

    protected final NewTrade trade;
    protected final Actor playerSide;
    protected final Player player;
    protected final String currencyName;
    protected final double previousPrice;
    protected String changingPriceString;

    public MoneySelector(NewTrade trade, Actor playerSide, Player player, String currencyName) {
        this.trade = trade;
        this.playerSide = playerSide;
        this.player = player;
        this.currencyName = currencyName;
        this.previousPrice = trade.getTradeSide(playerSide).getOrder().getPrice(currencyName);
        this.changingPriceString = Settings.getDecimalFormat().format(previousPrice);
    }

    protected boolean processTransaction(double priceDifference) {
        if (priceDifference < 0) {
            return RedisTrade.getInstance().getIntegrationManager()
              .getCurrencyHook(currencyName)
              .withdrawPlayer(player.getUniqueId(), Math.abs(priceDifference), "Trade price");
        } else if (priceDifference > 0) {
            return RedisTrade.getInstance().getIntegrationManager()
              .getCurrencyHook(currencyName)
              .depositPlayer(player.getUniqueId(), priceDifference, "Trade price");
        }
        return true;
    }

    protected void notifyInsufficientFunds(double nextPrice, double balance) {
        player.sendRichMessage(Messages.instance().notEnoughMoney
          .replace("%amount%", Settings.getDecimalFormat().format(nextPrice)));
        changingPriceString = Settings.getDecimalFormat().format(balance);
    }

    public abstract void openWindow();

}
