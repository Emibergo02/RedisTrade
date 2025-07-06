package dev.unnm3d.redistrade.hooks.currencies;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PlayerPointsCurrencyHook extends CurrencyHook {
    private final PlayerPointsAPI playerPointsAPI;

    public PlayerPointsCurrencyHook(String currencyName) {
        super(currencyName);
        this.playerPointsAPI = PlayerPoints.getInstance().getAPI();
        if (this.playerPointsAPI == null) {
            throw new IllegalStateException("PlayerPoints API not found");
        }
    }

    public boolean depositPlayer(@NotNull UUID playerUUID, double amount, String reason) {
        return playerPointsAPI.give(playerUUID, (int) amount);
    }

    public double getBalance(@NotNull UUID playerUUID) {
        return playerPointsAPI.look(playerUUID);
    }

    public boolean withdrawPlayer(@NotNull UUID playerUUID, double amount, String reason) {
        return playerPointsAPI.take(playerUUID, (int) amount);
    }

    @Override
    public @NotNull String getCurrencySymbol() {
        return "P";
    }

}
