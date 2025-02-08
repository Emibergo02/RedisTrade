package dev.unnm3d.redistrade.restriction;

import dev.unnm3d.redistrade.configs.Settings;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldRestriction implements RestrictionHook {

    @Override
    public String getName() {
        return KnownRestriction.WORLD_CHANGE.toString();
    }

    @Override
    public boolean restrict(Player player, Location playerLocation) {
        return Settings.instance().worldBlacklist.contains(playerLocation.getWorld().getName());
    }
}
