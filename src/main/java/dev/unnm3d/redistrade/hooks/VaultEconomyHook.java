package dev.unnm3d.redistrade.hooks;

import dev.unnm3d.redistrade.RedisTrade;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class VaultEconomyHook implements EconomyHook{
    private final Economy economy;

    public VaultEconomyHook(RedisTrade plugin) {
        final RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            throw new IllegalStateException("Economy not found");
        }
        economy = rsp.getProvider();
    }


    public boolean depositPlayer(UUID playerUUID, double amount, @NotNull String currencyName, String reason) {
        return depositPlayer(playerUUID, amount, reason);
    }

    public boolean depositPlayer(UUID playerUUID, double amount, String reason) {
        return economy.depositPlayer(Bukkit.getOfflinePlayer(playerUUID), amount).transactionSuccess();
    }

    public double getBalance(UUID playerUUID, @NotNull String currencyName) {
        return getBalance(playerUUID);
    }

    public double getBalance(UUID playerUUID) {
        return economy.getBalance(Bukkit.getOfflinePlayer(playerUUID));
    }

    public boolean withdrawPlayer(UUID playerUUID, double amount, @NotNull String currencyName, String reason) {
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

    public String getCurrencySymbol(@NotNull String currencyName) {
        return economy.currencyNamePlural();
    }

}
