package dev.unnm3d.redistrade.hooks;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import dev.unnm3d.redistrade.restriction.RestrictionHook;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class WorldGuardHook implements RestrictionHook {

    public final static StateFlag RESTRICT_TRADES = new StateFlag("restrict-trades", false);

    public WorldGuardHook() {
        if (WorldGuard.getInstance().getFlagRegistry().get("restrict-trades") != null) return;
        WorldGuard.getInstance().getFlagRegistry().register(RESTRICT_TRADES);
    }

    @Override
    public String getName() {
        return "WorldGuard";
    }

    @Override
    public boolean restriction(Player player, Location playerLocation) {
        return Optional.ofNullable(WorldGuard.getInstance().getPlatform().getRegionContainer()
                        .get(BukkitAdapter.adapt(playerLocation.getWorld())))
                .map(manager ->
                        getOverlappingRegions(manager, playerLocation).getRegions().stream()
                                .anyMatch(overlapped ->
                                        overlapped.getFlag(RESTRICT_TRADES) == StateFlag.State.DENY))
                .orElse(false);
    }

    @NotNull
    private static ApplicableRegionSet getOverlappingRegions(@NotNull RegionManager manager,
                                                             @NotNull org.bukkit.Location bukkitLocation) {
        return manager.getApplicableRegions(BlockVector3.at(bukkitLocation.x(), bukkitLocation.y(), bukkitLocation.z()));
    }
}
