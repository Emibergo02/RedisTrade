package dev.unnm3d.redistrade.hooks;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public interface EconomyHook {
    boolean depositPlayer(UUID playerUUID, double amount, @NotNull String currencyName, String reason);

    boolean depositPlayer(UUID playerUUID, double amount, String reason);

    double getBalance(UUID playerUUID, @NotNull String currencyName);

    double getBalance(UUID playerUUID);

    boolean withdrawPlayer(UUID playerUUID, double amount, @NotNull String currencyName, String reason);

    boolean withdrawPlayer(UUID playerUUID, double amount, String reason);

    String getDefaultCurrencyName();

    List<String> getCurrencyNames();

    String getCurrencySymbol(@NotNull String currencyName);
}
