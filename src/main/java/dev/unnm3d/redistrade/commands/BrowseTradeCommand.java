package dev.unnm3d.redistrade.commands;

import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.OptArg;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.Messages;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
public class BrowseTradeCommand {
    private final RedisTrade plugin;

    @Command(name = "", desc = "Browse past trades")
    @Require("redistrade.browse")
    public void browseTrade(@Sender Player player, @OptArg("start") @StartDate LocalDateTime start, @OptArg("end") LocalDateTime end, @OptArg("target") PlayerListManager.Target target) {
        UUID targetUUID = target.playerName() == null ? player.getUniqueId() :
          plugin.getPlayerListManager().getPlayerUUID(target.playerName())
            .orElse(null);
        if (targetUUID == null) {
            player.sendRichMessage(Messages.instance().playerNotFound.replace("%player%", target.playerName()));
            return;
        }
        if (start == null) {
            start = LocalDateTime.MIN;
        }
        if (end == null) {
            end = LocalDateTime.MAX;
        }

        plugin.getTradeManager().openBrowser(player, targetUUID, start, end);
    }
}
