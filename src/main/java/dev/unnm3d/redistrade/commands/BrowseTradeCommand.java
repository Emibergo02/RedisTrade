package dev.unnm3d.redistrade.commands;

import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.OptArg;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import dev.unnm3d.redistrade.Messages;
import dev.unnm3d.redistrade.RedisTrade;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;

import java.util.Date;
import java.util.UUID;

@AllArgsConstructor
public class BrowseTradeCommand {
    private final RedisTrade plugin;

    @Command(name = "", desc = "Browse past trades")
    @Require("redistrade.browse")
    public void browseTrade(@Sender Player player, @OptArg("start") @StartDate Date start, @OptArg("end") Date end, @OptArg("target") PlayerListManager.Target target) {
        UUID targetUUID = target.playerName() == null ? player.getUniqueId() :
                plugin.getPlayerListManager().getPlayerUUID(target.playerName())
                        .orElse(null);
        if (targetUUID == null) {
            player.sendRichMessage(Messages.instance().playerNotFound.replace("%player%", target.playerName()));
            return;
        }
        if (start == null) {
            start = new Date(0L);
        }
        if (end == null) {
            end = new Date();
        }

        plugin.getTradeManager().openBrowser(player, targetUUID, start, end);
    }
}
