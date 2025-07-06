package dev.unnm3d.redistrade.hooks.currencies;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class EmptyCurrencyHook extends CurrencyHook {
    public EmptyCurrencyHook() {
        super("empty");
    }

    @Override
    public boolean depositPlayer(@NotNull UUID playerUUID, double amount, String reason) {
        return true;
    }

    @Override
    public double getBalance(@NotNull UUID playerUUID) {
        return 0D;
    }

    @Override
    public boolean withdrawPlayer(@NotNull UUID playerUUID, double amount, String reason) {
        return true;
    }

    @Override
    public @NotNull String getCurrencySymbol() {
        return "";
    }

}
