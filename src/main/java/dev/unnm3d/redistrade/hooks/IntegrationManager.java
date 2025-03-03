package dev.unnm3d.redistrade.hooks;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.Settings;

import java.util.HashMap;
import java.util.Map;

public class IntegrationManager {
    private RedisTrade plugin;
    private HashMap<String, EconomyHook> hooks;
    /**
     * Currency names for each hook
     * Key is the currency name, value is the hook name
     */
    private HashMap<String, String> currencyNames;

    public IntegrationManager(RedisTrade redisTrade) {
        this.plugin = redisTrade;
        if (this.plugin.getServer().getPluginManager().getPlugin("RedisEconomy") != null) {
            registerHook("RedisEconomy", new RedisEconomyHook(this.plugin));
        } else if (this.plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
            registerHook("Vault", new VaultEconomyHook(this.plugin));
        }
        if (this.plugin.getServer().getPluginManager().getPlugin("PlayerPoints") != null) {
            registerHook("PlayerPoints", new PlayerPointsHook(this.plugin));
        }
        for (Map.Entry<String, Settings.CurrencyInfo> stringCurrency : Settings.instance().allowedCurrencies.entrySet()) {
            registerCurrencyName(stringCurrency.getKey(), stringCurrency.getValue().integrationName());
        }
    }

    public void registerHook(String name, EconomyHook hook) {
        hooks.put(name, hook);
    }

    public void registerCurrencyName(String currencyName, String hookName) {
        currencyNames.put(currencyName, hookName);
    }

}
