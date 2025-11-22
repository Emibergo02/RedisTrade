package dev.unnm3d.redistrade.hooks.restrictions;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.restriction.RestrictionHook;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class WorldGuardHook implements RestrictionHook {

    private StateFlag restrictFlag;

    public WorldGuardHook() {
        restrictFlag = (StateFlag) WorldGuard.getInstance().getFlagRegistry().get("restrict-trades");
        if (restrictFlag != null) return;
        restrictFlag = new StateFlag("restrict-trades", true);
        WorldGuard.getInstance().getFlagRegistry().register(restrictFlag);
    }

    @Override
    public String getName() {
        return "WORLD_GUARD";
    }

    @Override
    public boolean restrict(Player player, NewTrade trade) {
        return Optional.ofNullable(WorldGuard.getInstance().getPlatform().getRegionContainer()
                        .get(BukkitAdapter.adapt(player.getLocation().getWorld())))
                .map(manager -> getOverlappingRegions(manager, player.getLocation()).getRegions().stream()
                        .anyMatch(overlapped -> overlapped.getFlag(restrictFlag) == StateFlag.State.DENY))
                .orElse(false);
    }

    @NotNull
    private static ApplicableRegionSet getOverlappingRegions(@NotNull RegionManager manager,
                                                             @NotNull Location bukkitLocation) {
        return manager.getApplicableRegions(BlockVector3.at(bukkitLocation.x(), bukkitLocation.y(), bukkitLocation.z()));
    }
}
