package dev.unnm3d.redistrade.core;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.restriction.KnownRestriction;
import lombok.AllArgsConstructor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;

@AllArgsConstructor
public class PlayerListener implements Listener {
    private final RedisTrade plugin;

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (!Settings.instance().rightClickToOpen) return;
        if (!event.getPlayer().isSneaking()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player targetPlayer)) return;
        plugin.getTradeManager().getActiveTrade(event.getPlayer().getUniqueId())
                .ifPresentOrElse(trade -> {
                            if (!trade.getCustomerSide().getTraderName().equals(targetPlayer.getName()))
                                event.getPlayer().sendRichMessage(Messages.instance().alreadyInTrade
                                        .replace("%player%", trade.getTraderSide().getTraderName()));
                        },
                        () -> plugin.getTradeManager().startTrade(event.getPlayer(), targetPlayer.getName()));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().distanceSquared(event.getTo()) == 0) return;
        plugin.getRestrictionService().addPlayerRestriction(event.getPlayer(), KnownRestriction.MOVED.toString());
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
        plugin.getRestrictionService().addPlayerRestriction((Player) event.getEntity(), KnownRestriction.DAMAGED.toString());
    }
}
