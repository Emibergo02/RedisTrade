package dev.unnm3d.redistrade.commands;

import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import dev.unnm3d.redistrade.Messages;
import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.Settings;
import dev.unnm3d.redistrade.guis.TradeManager;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;

@AllArgsConstructor
public class TradeGuiCommand {
    private RedisTrade plugin;

    @Command(name = "setitem", desc = "Set the item")
    @Require("redistrade.setitem")
    public void setItem(@Sender Player player, Settings.ButtonType buttonType) {
        Settings.instance().buttons.put(buttonType, player.getInventory().getItemInMainHand());
        plugin.saveYML();
        player.sendRichMessage(Messages.instance().setItemField.replace("%field%", buttonType.name()));
    }

    @Command(name = "getitem", desc = "Get a gui item to your hand")
    @Require("redistrade.getitem")
    public void getItem(@Sender Player player, Settings.ButtonType buttonType) {
        player.getInventory().addItem(Settings.instance().getButton(buttonType));
        player.sendRichMessage(Messages.instance().getItemField.replace("%field%", buttonType.name()));
    }

}
