package dev.unnm3d.redistrade.restriction;

import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.NewTrade;
import org.bukkit.entity.Player;

public class WorldRestriction implements RestrictionHook {

    @Override
    public String getName() {
        return KnownRestriction.WORLD_CHANGE.toString();
    }

    @Override
    public boolean restrict(Player player, NewTrade trade) {
        return Settings.instance().worldBlacklist.contains(player.getLocation().getWorld().getName());
    }
}
