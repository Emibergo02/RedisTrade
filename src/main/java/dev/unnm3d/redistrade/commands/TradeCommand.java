package dev.unnm3d.redistrade.commands;

import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.OptArg;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.RedisTrade;
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
                            plugin.getServer().getScheduler().runTask(plugin, () ->
                                    plugin.getTradeManager().openWindow(trade, player.getUniqueId()
                                    ));
                            if(!trade.getOtherSide().getTraderName().equals(targetName.playerName()))
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
