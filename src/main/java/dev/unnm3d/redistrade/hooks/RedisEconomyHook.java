package dev.unnm3d.redistrade.hooks;

import dev.unnm3d.rediseconomy.api.RedisEconomyAPI;
import dev.unnm3d.rediseconomy.currency.Currency;
import dev.unnm3d.redistrade.RedisTrade;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class RedisEconomyHook extends EconomyHook implements MultipleCurrency {

    private final RedisEconomyAPI api;

    public RedisEconomyHook(RedisTrade plugin) {
        super(plugin);
        this.api = RedisEconomyAPI.getAPI();
        if (api == null) {
            throw new IllegalStateException("RedisEconomy not found");
        }
    }

    public boolean depositPlayer(UUID playerUUID, double amount, @NotNull String currencyName, String reason) {
        Currency currency = api.getCurrencyByName(currencyName);
        if (currency == null) return false;
        return currency.depositPlayer(playerUUID, null, amount, reason).transactionSuccess();
    }

    public double getBalance(UUID playerUUID, String currencyName) {
        Currency currency = api.getCurrencyByName(currencyName);
        if (currency == null) return 0D;
        return currency.getBalance(playerUUID);
    }

    public boolean withdrawPlayer(UUID playerUUID, double amount, String currencyName, String reason) {
        Currency currency = api.getCurrencyByName(currencyName);
        if (currency == null) return false;
        return currency.withdrawPlayer(playerUUID, null, amount, reason).transactionSuccess();
    }

    @Override
    public List<String> getCurrencyNames() {
        return api.getCurrencies().stream().map(Currency::getName).toList();
    }
}
