package dev.unnm3d.redistrade.core;

import dev.unnm3d.redistrade.configs.Messages;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;


public class CompletionTimer extends BukkitRunnable {
    private final NewTrade trade;
    private int iteration;
    private UUID traderUUID;
    private UUID targetUUID;

    public CompletionTimer(NewTrade trade) {
        this.trade = trade;
        this.iteration = 1;
    }

    @Override
    public void run() {
        if (this.iteration == 4) {
            this.trade.completePhase();
            return;
        }

        Optional.ofNullable(Bukkit.getPlayer(this.traderUUID))
          .ifPresent(player -> {
              player.sendRichMessage(Messages.instance().completionTimer.replace("%time%", String.valueOf(4 - this.iteration)));
              player.playSound(player, "entity.experience_orb.pickup", 1, 1);
          });

        Optional.ofNullable(Bukkit.getPlayer(this.targetUUID))
          .ifPresent(player -> {
              player.sendRichMessage(Messages.instance().completionTimer.replace("%time%", String.valueOf(4 - this.iteration)));
              player.playSound(player, "entity.experience_orb.pickup", 1, 1);
          });

        this.iteration++;
    }

    public void runTask(Plugin redisTrade, @NotNull UUID traderUUID, @NotNull UUID targetUUID) {
        this.traderUUID = traderUUID;
        this.targetUUID = targetUUID;
        this.runTaskTimer(redisTrade, 20, 20);
    }

    @Override
    public void cancel() {
        super.cancel();
        this.iteration = 0;
    }
}
