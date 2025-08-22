package dev.unnm3d.redistrade.hooks.currencies;

import dev.unnm3d.redistrade.RedisTrade;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;


public class MinecraftValueCurrencyHook extends CurrencyHook {
    public MinecraftValueCurrencyHook(String currencyName) {
        super(currencyName);
    }

    @Override
    public boolean depositPlayer(@NotNull UUID playerUUID, double amount, String reason) {
        Player player = RedisTrade.getInstance().getServer().getPlayer(playerUUID);
        if (player == null) return false;
        return switch (name) {
            case "xp" -> {
                player.setExperienceLevelAndProgress(player.calculateTotalExperiencePoints() + (int) amount);
                yield true;
            }
            case "xplevel" -> {
                player.setLevel(player.getLevel() + (int) amount);
                yield true;
            }
            case "health" -> {
                player.setHealth(Math.min(player.getHealth() + amount, player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
                yield true;
            }
            default -> false;
        };
    }

    @Override
    public double getBalance(@NotNull UUID playerUUID) {
        Player player = RedisTrade.getInstance().getServer().getPlayer(playerUUID);
        if (player == null) return 0D;
        return switch (name) {
            case "xp" -> player.calculateTotalExperiencePoints();
            case "xplevel" -> player.getLevel();
            case "health" -> player.getHealth();
            default -> 0D;
        };
    }

    @Override
    public boolean withdrawPlayer(@NotNull UUID playerUUID, double amount, String reason) {
        Player player = RedisTrade.getInstance().getServer().getPlayer(playerUUID);
        if (player == null) return false;
        switch (name) {
            case "xp":
                if (getBalance(playerUUID) < (int) amount) return false;
                player.setExperienceLevelAndProgress(player.calculateTotalExperiencePoints() - (int) amount);
                return true;
            case "xplevel":
                if (getBalance(playerUUID) < (int) amount) return false;
                player.setLevel(player.getLevel() - (int) amount);
                return true;
            case "health":
                if (getBalance(playerUUID) < amount) return false;
                player.setHealth(player.getHealth() - amount);
                return true;
        }
        return false;
    }

    @Override
    public @NotNull String getCurrencySymbol() {
        return switch (name) {
            case "xp" -> "XP";
            case "xplevel" -> "XPLVL";
            case "health" -> "❤";
            default -> "";
        };
    }
}
