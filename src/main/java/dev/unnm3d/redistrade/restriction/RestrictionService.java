package dev.unnm3d.redistrade.restriction;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.core.TradeSide;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class RestrictionService {
    private final ConcurrentHashMap<Player, Restriction> restrictionCooldown = new ConcurrentHashMap<>();
    private final List<RestrictionHook> restrictionHooks = new ArrayList<>();
    private RedisTrade plugin;

    public RestrictionService(RedisTrade plugin) {
        this.plugin = plugin;
    }

    public @Nullable Restriction getRestriction(Player player, NewTrade trade) {
        for (RestrictionHook restrictionHook : restrictionHooks) {
            boolean isRestricted = restrictionHook.restrict(player, trade);
            if (!isRestricted) continue;
            addPlayerRestriction(player, restrictionHook.getName());
        }
        final Restriction restriction = restrictionCooldown.get(player);
        if (restriction == null) return null;
        if (restriction.isExpired()) {
            restrictionCooldown.remove(player);
            return null;
        }
        RedisTrade.debug("Player " + player.getName() + " is restricted by " + restriction.restrictionName());
        return restriction;
    }

    public void addRestrictionHook(RestrictionHook hook) {
        restrictionHooks.add(hook);
    }

    public void addPlayerRestriction(Player player, String restrictionName) {
        final Restriction previousRestriction = restrictionCooldown.get(player);

        int restrictionDuration = Settings.instance().actionCooldowns.getOrDefault(restrictionName, 0);
        if (restrictionDuration <= 0) return;

        // If the restrict is null or the remaining is less than the future restrict timestamp
        if (previousRestriction == null || previousRestriction.endRestrictionTimestamp() < System.currentTimeMillis() + restrictionDuration) {
            restrictionCooldown.put(player, Restriction.from(restrictionName, restrictionDuration));

            //Close trade GUI for the player if open
            plugin.getTradeManager().getActiveTrade(player.getUniqueId()).ifPresent(trade -> {
                final TradeSide playerSide = trade.getTradeSide(trade.getActor(player));
                if (playerSide.getSidePerspective().findAllCurrentViewers().contains(player)) {
                    player.closeInventory();
                    player.sendRichMessage(Messages.instance().tradeWindowClosed);
                }
            });
        }
    }


    public record Restriction(String restrictionName, long endRestrictionTimestamp) {
        public static Restriction from(String restrictionName, long duration) {
            return new Restriction(restrictionName, System.currentTimeMillis() + duration);
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > endRestrictionTimestamp;
        }
    }

}
