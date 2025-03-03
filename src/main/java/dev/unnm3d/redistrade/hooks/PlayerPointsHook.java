package dev.unnm3d.redistrade.hooks;

import dev.unnm3d.redistrade.RedisTrade;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.List;
import java.util.UUID;

public class PlayerPointsHook implements EconomyHook {
    private final PlayerPointsAPI playerPointsAPI;

    public PlayerPointsHook(RedisTrade plugin) {
        this.playerPointsAPI = PlayerPoints.getInstance().getAPI();
        if (this.playerPointsAPI == null) {
            throw new IllegalStateException("Economy not found");
        }
    }


    public boolean depositPlayer(UUID playerUUID, double amount, String currencyName, String reason) {
        return depositPlayer(playerUUID, amount, reason);
    }

    public boolean depositPlayer(UUID playerUUID, double amount, String reason) {
        return playerPointsAPI.give(playerUUID, (int) amount);
    }

    public double getBalance(UUID playerUUID, String currencyName) {
        return getBalance(playerUUID);
    }

    public double getBalance(UUID playerUUID) {
        return playerPointsAPI.look(playerUUID);
    }

    public boolean withdrawPlayer(UUID playerUUID, double amount, String currencyName, String reason) {
        return withdrawPlayer(playerUUID, amount, reason);
    }

    public boolean withdrawPlayer(UUID playerUUID, double amount, String reason) {
        return playerPointsAPI.take(playerUUID, (int) amount);
    }

    public String getDefaultCurrencyName() {
        return "default";
    }

    public List<String> getCurrencyNames() {
        return List.of("default");
    }

    public String getCurrencySymbol(String currencyName) {
        return "P";
    }

}
