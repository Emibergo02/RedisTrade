package dev.unnm3d.redistrade.commands;

import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import dev.unnm3d.redistrade.RedisTrade;
import org.bukkit.command.CommandSender;

public class TradeReloadCommand {

    @Command(name = "", desc = "Reload RedisTrade")
    @Require("redistrade.admin")
    public void createTrade(@Sender CommandSender sender) {
        RedisTrade.getInstance().loadYML();
        sender.sendMessage("§2RedisTrade reloaded");
    }
}
