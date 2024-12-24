package dev.unnm3d.redistrade.commands;

import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.GuiSettings;
import dev.unnm3d.redistrade.configs.Messages;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

@SuppressWarnings("unused")
@AllArgsConstructor
public class TradeGuiCommand {
    private RedisTrade plugin;


    @Command(name = "", desc = "Set the item")
    @Require("redistrade.setitem")
    public void defaultItem(@Sender Player player) {
    }

    @Command(name = "setitem", desc = "Set the item")
    @Require("redistrade.setitem")
    public void setItem(@Sender Player player, Field itemField) {
        itemField.setAccessible(true);
        try {
            itemField.set(GuiSettings.instance(), GuiSettings.SimpleSerializableItem
                    .fromItemStack(player.getInventory().getItemInMainHand()));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        plugin.saveYML();
        player.sendRichMessage(Messages.instance().setItemField.replace("%field%", itemField.getName()));
    }

    @Command(name = "getitem", desc = "Get a gui item to your hand")
    @Require("redistrade.getitem")
    public void getItem(@Sender Player player, Field itemField) {
        try {
            GuiSettings.SimpleSerializableItem item = (GuiSettings.SimpleSerializableItem) itemField.get(GuiSettings.instance());
            player.getInventory().addItem(item.toItemBuilder().get());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        player.sendRichMessage(Messages.instance().getItemField.replace("%field%", itemField.getName()));
    }

}
