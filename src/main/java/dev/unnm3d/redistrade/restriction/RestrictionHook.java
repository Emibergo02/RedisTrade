package dev.unnm3d.redistrade.restriction;

import dev.unnm3d.redistrade.core.NewTrade;
import org.bukkit.entity.Player;

public interface RestrictionHook {

    String getName();

    boolean restrict(Player player, NewTrade trade);
}
