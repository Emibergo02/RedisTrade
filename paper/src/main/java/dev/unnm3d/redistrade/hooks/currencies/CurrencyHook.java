package dev.unnm3d.redistrade.hooks.currencies;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
public abstract class CurrencyHook {
    protected final String name;

    public CurrencyHook(String currencyName) {
        this.name = currencyName;
    }

    public abstract boolean depositPlayer(@NotNull UUID playerUUID, double amount, @Nullable String reason);

    public abstract double getBalance(@NotNull UUID playerUUID);

    public abstract boolean withdrawPlayer(@NotNull UUID playerUUID, double amount, @Nullable String reason);

    @NotNull
    public abstract String getCurrencySymbol();
}
