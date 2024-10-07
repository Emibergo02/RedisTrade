package dev.unnm3d.redistrade.commands;

import com.github.Anon8281.universalScheduler.UniversalRunnable;
import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import dev.unnm3d.redistrade.RedisTrade;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerListManager {
    private final MyScheduledTask task;
    private final ConcurrentHashMap<String, Long> playerList;


    public PlayerListManager(RedisTrade plugin) {
        this.playerList = new ConcurrentHashMap<>();
        this.task = new UniversalRunnable() {
            @Override
            public void run() {
                playerList.entrySet().removeIf(stringLongEntry -> System.currentTimeMillis() - stringLongEntry.getValue() > 1000 * 4);

                final List<String> tempList = plugin.getServer().getOnlinePlayers().stream()
                        .map(HumanEntity::getName)
                        .filter(s -> !s.isEmpty())
                        .toList();
                if (!tempList.isEmpty())
                    plugin.getRedisDataManager().publishPlayerList(tempList);
                tempList.forEach(s -> playerList.put(s, System.currentTimeMillis()));
            }
        }.runTaskTimerAsynchronously(plugin, 0, 60);//3 seconds
    }

    public void updatePlayerList(List<String> inPlayerList) {
        long currentTimeMillis = System.currentTimeMillis();
        inPlayerList.forEach(s -> {
            if (s != null && !s.isEmpty())
                playerList.put(s, currentTimeMillis);
        });
    }


    public Set<String> getPlayerList(@Nullable CommandSender sender) {
        final Set<String> keySet = new HashSet<>(playerList.keySet());
        //Vanish integration
        return keySet;
    }

    public void stop() {
        task.cancel();
    }

    public record Target(String playerName){}
}
