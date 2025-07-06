package dev.unnm3d.redistrade.commands;

import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.GuiSettings;
import dev.unnm3d.redistrade.configs.Messages;
import lombok.AllArgsConstructor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.window.AnvilWindow;

import java.lang.reflect.Field;

@AllArgsConstructor
@SuppressWarnings("unused")
public class TradeAdminCommand {
    private static BukkitTask task;
    private final RedisTrade plugin;

    @Command(name = "reload", desc = "Reload RedisTrade")
    @Require("redistrade.reload")
    public void reload(@Sender CommandSender sender) {
        plugin.loadYML();
        sender.sendMessage("§2RedisTrade reloaded");
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

    @Command(name = "stresser", desc = "Stress test")
    @Require("redistrade.admin")
    public void toggleStress(@Sender CommandSender sender) {
        if (task != null) {
            task.cancel();
            task = null;
            sender.sendMessage("§2Stress test stopped");
            return;
        }
        sender.sendMessage("§2Stress test started");
        task = RedisTrade.getInstance().getServer().getScheduler().runTaskTimer(RedisTrade.getInstance(), () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, 0, 20);
    }

    @Command(name = "test", desc = "Stress test")
    public void test(@Sender Player sender) {
        ;
        AnvilWindow.single().setGui(Gui.normal().setStructure("abc")
                .addIngredient('a', new ItemStack(Material.DIAMOND))
                .addIngredient('b', new ItemStack(Material.DIAMOND))
                .addIngredient('c', new ItemStack(Material.DIAMOND)).build())
                .open(sender);
    }

}
