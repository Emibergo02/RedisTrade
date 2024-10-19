package dev.unnm3d.redistrade.commands;

import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import dev.unnm3d.redistrade.RedisTrade;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class TradeCommand {

    private final RedisTrade plugin;

    @Command(name = "", desc = "Open the emporium")
    @Require("redistrade.trade")
    public void createTrade(@Sender Player player, @Nullable PlayerListManager.Target targetName) {
        if (targetName == null) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getTradeManager().startTrade(player, targetName.playerName()));
    }

}
