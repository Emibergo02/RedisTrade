package dev.unnm3d.redistrade.hooks;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class EmptyEconomyHook implements EconomyHook{
    @Override
    public boolean depositPlayer(UUID playerUUID, double amount, @NotNull String currencyName, String reason) {
        return true;
    }

    @Override
    public boolean depositPlayer(UUID playerUUID, double amount, String reason) {
        return true;
    }

    @Override
    public double getBalance(UUID playerUUID, @NotNull String currencyName) {
        return 0;
    }

    @Override
    public double getBalance(UUID playerUUID) {
        return 0;
    }

    @Override
    public boolean withdrawPlayer(UUID playerUUID, double amount, @NotNull String currencyName, String reason) {
        return true;
    }

    @Override
    public boolean withdrawPlayer(UUID playerUUID, double amount, String reason) {
        return true;
    }

    @Override
    public String getDefaultCurrencyName() {
        return "default";
    }

    @Override
    public List<String> getCurrencyNames() {
        return List.of("default");
    }

    @Override
    public String getCurrencySymbol(@NotNull String currencyName) {
        return "€";
    }
}
