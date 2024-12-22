package dev.unnm3d.redistrade.commands;

import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import dev.unnm3d.redistrade.RedisTrade;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitTask;

public class StresserCommand {
    private static BukkitTask task;

    @Command(name = "", desc = "Stress test")
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
}
