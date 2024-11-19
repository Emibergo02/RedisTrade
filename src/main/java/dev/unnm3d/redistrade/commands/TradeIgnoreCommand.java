package dev.unnm3d.redistrade.commands;

import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.guis.TradeManager;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;

@AllArgsConstructor
public class TradeIgnoreCommand {
    private TradeManager tradeManager;

    @Command(name = "toggle", desc = "Ignores trades from a player")
    @Require("redistrade.ignore")
    public void ignoreTrade(@Sender Player player, PlayerListManager.Target targetName) {
        if (targetName == null) return;
        if (tradeManager.isIgnoring(player.getName(), targetName.playerName())) {
            tradeManager.ignorePlayerCloud(player.getName(), targetName.playerName(), false);
            player.sendRichMessage(Messages.instance().tradeUnignored.replace("%player%", targetName.playerName()));
        } else {
            tradeManager.ignorePlayerCloud(player.getName(), targetName.playerName(), true);
            player.sendRichMessage(Messages.instance().tradeIgnored.replace("%player%", targetName.playerName()));
        }
    }

    @Command(name = "list", desc = "Ignores trades from a player")
    @Require("redistrade.ignore")
    public void ignoreList(@Sender Player player) {
        player.sendRichMessage(Messages.instance().tradeIgnoreList.replace("%list%",
                String.join(", ", tradeManager.getIgnoredPlayers(player.getName()))));
    }

}
