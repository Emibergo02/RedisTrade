package dev.unnm3d.redistrade.hooks;

import java.util.List;
import java.util.UUID;

public class EmptyEconomyHook implements EconomyHook{
    @Override
    public boolean depositPlayer(UUID playerUUID, double amount, String currencyName, String reason) {
        return true;
    }

    @Override
    public boolean depositPlayer(UUID playerUUID, double amount, String reason) {
        return true;
    }

    @Override
    public double getBalance(UUID playerUUID, String currencyName) {
        return 0;
    }

    @Override
    public double getBalance(UUID playerUUID) {
        return 0;
    }

    @Override
    public boolean withdrawPlayer(UUID playerUUID, double amount, String currencyName, String reason) {
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
    public String getCurrencySymbol(String currencyName) {
        return "€";
    }
}
