package dev.unnm3d.redistrade.commands;

import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.guis.browsers.ActiveTradesBrowserGUI;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;

import java.util.List;

@AllArgsConstructor
public class SpectateTradeCommand {
    private final RedisTrade plugin;

    @Command(name = "", desc = "Spectate a trade")
    @Require("redistrade.spectate")
    public void spectateTrade(@Sender Player player, PlayerListManager.Target targetName) {
        if (targetName.playerName() == null) {
            player.sendRichMessage(Messages.instance().noPendingTrades);
            return;
        }
        String playerName = targetName.playerName();
        plugin.getPlayerListManager().getPlayerUUID(playerName)
          //Find player's UUID
          .ifPresentOrElse(targetUUID -> {
              final List<NewTrade> actives = plugin.getTradeManager().getPlayerActiveTrades(targetUUID);
              if (actives.isEmpty()) {
                  player.sendRichMessage(Messages.instance().noPendingTradesOther.replace("%player%", playerName));
                  return;
              }
              if (actives.size() == 1) {
                  plugin.getTradeManager().openWindow(actives.getFirst(), player, true);
                  return;
              }
              ActiveTradesBrowserGUI.openBrowser(player, targetUUID, actives);

          }, () -> player.sendRichMessage(Messages.instance().playerNotFound.replace("%player%", playerName)));
    }
}
