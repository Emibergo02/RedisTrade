package dev.unnm3d.redistrade.core;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.restriction.KnownRestriction;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;

public record PlayerListener(RedisTrade plugin) implements Listener {
    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (!Settings.instance().rightClickToOpen) return;
        if (!event.getPlayer().isSneaking()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player targetPlayer)) return;
        plugin.getTradeManager().startTrade(event.getPlayer(), targetPlayer.getUniqueId(), targetPlayer.getName());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getTradeManager().loadIgnoredPlayers(event.getPlayer().getName());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().distanceSquared(event.getTo()) < 0.1) return;
        plugin.getRestrictionService().addPlayerRestriction(event.getPlayer(), KnownRestriction.MOVED.toString());
    }

    @EventHandler
    public void onPlayerMount(EntityMountEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        plugin.getRestrictionService().addPlayerRestriction(p, KnownRestriction.MOUNT.toString());
    }

    @EventHandler
    public void onPlayerDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        plugin.getRestrictionService().addPlayerRestriction(p, KnownRestriction.MOUNT.toString());
    }

    @EventHandler
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) return;
        plugin.getRestrictionService().addPlayerRestriction((Player) event.getEntity(), KnownRestriction.DAMAGED.toString());
    }

    @EventHandler
    public void onPlayerCombat(EntityDamageByEntityEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) return;
        if (event.getDamager().getType() != EntityType.PLAYER) return;
        plugin.getRestrictionService().addPlayerRestriction((Player) event.getDamager(), KnownRestriction.COMBAT.toString());
    }
}
