package dev.unnm3d.redistrade.hooks;

import java.util.List;
import java.util.UUID;

public interface EconomyHook {
    boolean depositPlayer(UUID playerUUID, double amount, String currencyName, String reason);

    boolean depositPlayer(UUID playerUUID, double amount, String reason);

    double getBalance(UUID playerUUID, String currencyName);

    double getBalance(UUID playerUUID);

    boolean withdrawPlayer(UUID playerUUID, double amount, String currencyName, String reason);

    boolean withdrawPlayer(UUID playerUUID, double amount, String reason);

    String getDefaultCurrencyName();

    List<String> getCurrencyNames();

    String getCurrencySymbol(String currencyName);
}
