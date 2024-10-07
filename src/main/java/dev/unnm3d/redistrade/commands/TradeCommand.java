package dev.unnm3d.redistrade.commands;

import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.Sender;
import dev.unnm3d.redistrade.RedisTrade;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class TradeCommand {

    private final RedisTrade plugin;

    @Command(name = "", desc = "Open the emporium")
    public void openTradeGUI(@Sender Player player, PlayerListManager.Target targetName) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {

            plugin.getTradeManager().startTrade(player, targetName.playerName());
        });
    }

}
