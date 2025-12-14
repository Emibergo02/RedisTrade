package dev.unnm3d.redistrade.commands;

import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.OptArg;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.core.enums.Actor;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class TradeCommand {

    private final RedisTrade plugin;

    @Command(name = "", desc = "Open the emporium")
    @Require("redistrade.trade")
    /**
     * /trade resume the trade. If no trade is found, send message
     * /trade <player> sends invite to player
     *
     * If target has already invited sender, accept the invite and
     * open the trade window only if target is not already in a trade
     *
     * If target is already in a trade with sender, open the trade window
     * If the target is already in another trade, send message
     * If sender is already in a trade, send message with possibility to open the trade window
     */
    public void createTrade(@Sender Player player, @OptArg("target") PlayerListManager.Target targetName) {
        plugin.getTradeManager().getActiveTrade(player.getUniqueId())
          .ifPresentOrElse(trade -> {
                // If the player is already in a trade, open the trade window
                boolean isTargetTradingAlready = trade.getActor(player) != Actor.SPECTATOR;
                if (targetName == null || targetName.playerName() == null || isTargetTradingAlready) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                      plugin.getTradeManager().openWindow(trade, player));
                    return;
                }
                //If the targetName is not in the active trade, tell the player that he is already in a trade
                player.sendRichMessage(Messages.instance().alreadyInTrade
                  .replace("%player%", trade.getTraderSide().getTraderName()));
            },
            () -> {
                if (targetName == null || targetName.playerName() == null) {
                    player.sendRichMessage(Messages.instance().noPendingTrades);
                    return;
                }
                plugin.getTradeManager().startTrade(player, targetName.playerName());
            });
    }

}
