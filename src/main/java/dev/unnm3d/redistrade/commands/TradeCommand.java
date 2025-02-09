package dev.unnm3d.redistrade.commands;

import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.OptArg;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.Messages;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class TradeCommand {

    private final RedisTrade plugin;

    @Command(name = "", desc = "Open the emporium")
    @Require("redistrade.trade")
    public void createTrade(@Sender Player player, @OptArg("target") PlayerListManager.Target targetName) {

        plugin.getTradeManager().getActiveTrade(player.getUniqueId())
                .ifPresentOrElse(trade -> {
                            // If the player is already in a trade, open the trade window
                            if (targetName == null || targetName.playerName() == null || trade.getCustomerSide().getTraderName().equals(targetName.playerName())) {
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
