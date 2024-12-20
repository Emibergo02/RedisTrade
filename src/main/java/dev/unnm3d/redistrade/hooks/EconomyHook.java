package dev.unnm3d.redistrade.hooks;

import dev.unnm3d.redistrade.RedisTrade;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.List;
import java.util.UUID;

public class EconomyHook {
    private final Economy economy;

    public EconomyHook(RedisTrade plugin) {
        final RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            throw new IllegalStateException("Economy not found");
        }
        economy = rsp.getProvider();
    }


    public boolean depositPlayer(UUID playerUUID, double amount, String currencyName, String reason) {
        return depositPlayer(playerUUID, amount, reason);
    }

    public boolean depositPlayer(UUID playerUUID, double amount, String reason) {
        return economy.depositPlayer(Bukkit.getOfflinePlayer(playerUUID), amount).transactionSuccess();
    }

    public double getBalance(UUID playerUUID, String currencyName) {
        return getBalance(playerUUID);
    }

    public double getBalance(UUID playerUUID) {
        return economy.getBalance(Bukkit.getOfflinePlayer(playerUUID));
    }

    public boolean withdrawPlayer(UUID playerUUID, double amount, String currencyName, String reason) {
        return withdrawPlayer(playerUUID, amount, reason);
    }

    public boolean withdrawPlayer(UUID playerUUID, double amount, String reason) {
        return economy.withdrawPlayer(Bukkit.getOfflinePlayer(playerUUID), amount).transactionSuccess();
    }

    public String getDefaultCurrencyName() {
        return "default";
    }

    public List<String> getCurrencyNames() {
        return List.of("default");
    }

    public String getCurrencySymbol(String currencyName) {
        return economy.currencyNamePlural();
    }

}
