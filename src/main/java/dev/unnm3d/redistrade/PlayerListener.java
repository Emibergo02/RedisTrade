package dev.unnm3d.redistrade;

import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@RequiredArgsConstructor
public class PlayerListener implements Listener {
    private RedisTrade plugin;

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getTradeManager().loadIgnoredPlayers(event.getPlayer().getName());
    }
}
