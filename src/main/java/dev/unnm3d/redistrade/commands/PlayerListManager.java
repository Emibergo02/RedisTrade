package dev.unnm3d.redistrade.commands;

import com.github.Anon8281.universalScheduler.UniversalRunnable;
import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import dev.unnm3d.redistrade.RedisTrade;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerListManager implements Listener {
    private final RedisTrade plugin;
    private final MyScheduledTask task;
    private final ConcurrentHashMap<String, Long> onlinePlayerList;
    private final ConcurrentHashMap<String, UUID> nameUUIDs;


    public PlayerListManager(RedisTrade plugin) {
        this.plugin = plugin;
        this.onlinePlayerList = new ConcurrentHashMap<>();
        this.nameUUIDs = new ConcurrentHashMap<>();
        this.task = new UniversalRunnable() {
            @Override
            public void run() {
                onlinePlayerList.entrySet().removeIf(stringLongEntry -> System.currentTimeMillis() - stringLongEntry.getValue() > 1000 * 4);

                final List<String> tempList = plugin.getServer().getOnlinePlayers().stream()
                        .map(HumanEntity::getName)
                        .filter(n -> !n.isEmpty())
                        .toList();
                if (!tempList.isEmpty())
                    plugin.getDataCache().publishPlayerList(tempList);
                tempList.forEach(s -> onlinePlayerList.put(s, System.currentTimeMillis()));
            }
        }.runTaskTimerAsynchronously(plugin, 0, 60);//3 seconds

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        plugin.getDataStorage().loadNameUUIDs()
                .thenAccept(map -> {
                    nameUUIDs.clear();
                    nameUUIDs.putAll(map);
                    plugin.getLogger().info("Loaded " + map.size() + " nameUUIDs from database");
                });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getDataCache().updateCachePlayerList(event.getPlayer().getName(), event.getPlayer().getUniqueId());
        plugin.getDataStorage().updateStoragePlayerList(event.getPlayer().getName(), event.getPlayer().getUniqueId());

        setPlayerNameUUID(event.getPlayer().getName(), event.getPlayer().getUniqueId());
    }

    public void updatePlayerList(List<String> inPlayerList) {
        long currentTimeMillis = System.currentTimeMillis();
        inPlayerList.forEach(s -> {
            if (s != null && !s.isEmpty())
                onlinePlayerList.put(s, currentTimeMillis);
        });
    }


    public Set<String> getPlayerList(@Nullable CommandSender sender) {
        final Set<String> keySet = new HashSet<>(onlinePlayerList.keySet());
        //Vanish integration
        return keySet;
    }

    public Optional<UUID> getPlayerUUID(String name) {
        return Optional.ofNullable(nameUUIDs.get(name));
    }

    public void setPlayerNameUUID(String name, UUID uuid) {
        nameUUIDs.put(name, uuid);
    }

    public Optional<String> getPlayerName(UUID uuid) {
        for (Map.Entry<String, UUID> stringUUIDEntry : nameUUIDs.entrySet()) {
            if (stringUUIDEntry.getValue().equals(uuid)) {
                return Optional.of(stringUUIDEntry.getKey());
            }
        }
        return Optional.empty();
    }

    public void stop() {
        HandlerList.unregisterAll(this);
        task.cancel();
    }

    public void clear() {
        onlinePlayerList.clear();
    }

    public record Target(String playerName) {
    }
}
