package dev.unnm3d.redistrade.restriction;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface RestrictionHook {

    String getName();

    boolean restrict(Player player, Location playerLocation);
}
