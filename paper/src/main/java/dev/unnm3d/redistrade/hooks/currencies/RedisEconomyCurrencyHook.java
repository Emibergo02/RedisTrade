package dev.unnm3d.redistrade.hooks.currencies;

import dev.unnm3d.rediseconomy.api.RedisEconomyAPI;
import dev.unnm3d.rediseconomy.currency.Currency;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class RedisEconomyCurrencyHook extends CurrencyHook {
    private final Currency currency;

    public RedisEconomyCurrencyHook(String currencyName) {
        super(currencyName);
        final RedisEconomyAPI api = RedisEconomyAPI.getAPI();
        if (api == null) {
            throw new IllegalStateException("RedisEconomy not found");
        }
        this.currency = api.getCurrencyByName(currencyName);
        if (this.currency == null) {
            throw new IllegalStateException("RedisEconomy currency " + currencyName + " not found");
        }
    }

    @Override
    public boolean depositPlayer(@NotNull UUID playerUUID, double amount, String reason) {
        return currency.depositPlayer(playerUUID, null, amount, reason).transactionSuccess();
    }

    @Override
    public double getBalance(@NotNull UUID playerUUID) {
        return currency.getBalance(playerUUID);
    }

    @Override
    public boolean withdrawPlayer(@NotNull UUID playerUUID, double amount, String reason) {
        return currency.withdrawPlayer(playerUUID, null, amount, reason).transactionSuccess();
    }

    @Override
    public @NotNull String getCurrencySymbol() {
        return currency.getCurrencyPlural();
    }
}
