package dev.unnm3d.redistrade.commands;

import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.core.enums.Actor;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;

@AllArgsConstructor
public class SpectateTradeCommand {
    private final RedisTrade plugin;

    @Command(name = "", desc = "Spectate a trade")
    @Require("redistrade.spectate")
    public void browseTrade(@Sender Player player, PlayerListManager.Target targetName) {
        if (targetName.playerName() == null) {
            player.sendRichMessage(Messages.instance().noPendingTrades);
            return;
        }
        String playerName = targetName.playerName();
        plugin.getPlayerListManager().getPlayerUUID(playerName)
          //Find player's UUID
          .ifPresentOrElse(targetUUID -> plugin.getTradeManager().getActiveTrade(targetUUID)
              //Open trade window if player is in a trade
              .ifPresentOrElse(trade -> {
                    Actor targetActor = trade.isTrader(targetUUID) ? Actor.TRADER : Actor.CUSTOMER;
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                      //Open the window on the main thread
                      trade.openWindow(player, targetActor));
                },
                () -> player.sendRichMessage(Messages.instance().noPendingTradesOther
                  .replace("%player%", playerName))),
            () -> player.sendRichMessage(Messages.instance().playerNotFound.replace("%player%", playerName)));
    }
}
