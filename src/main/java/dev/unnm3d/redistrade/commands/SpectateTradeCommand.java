package dev.unnm3d.redistrade.commands;

import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.core.NewTrade;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

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
              final Map<UUID, NewTrade> activeTrades = plugin.getTradeManager()
                .getPlayerActiveTrades(targetUUID);
              if (activeTrades.isEmpty()) {
                  player.sendRichMessage(Messages.instance().noPendingTradesOther.replace("%player%", playerName));
                  return;
              }
              activeTrades.values().forEach(newTrade ->
                plugin.getServer().getScheduler().runTask(plugin, () ->
                  //Open the window on the main thread
                  plugin.getTradeManager().openWindow(newTrade, player)));

          }, () -> player.sendRichMessage(Messages.instance().playerNotFound.replace("%player%", playerName)));
    }
}
