package dev.unnm3d.redistrade.hooks.currencies;

import dev.unnm3d.redistrade.RedisTrade;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class VaultCurrencyHook extends CurrencyHook {
    private final Economy economy;

    public VaultCurrencyHook(RedisTrade plugin, String currencyName) {
        super(currencyName);
        final RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            throw new IllegalStateException("Economy not found");
        }
        economy = rsp.getProvider();
    }

    @Override
    public boolean depositPlayer(@NotNull UUID playerUUID, double amount, String reason) {
        return economy.depositPlayer(Bukkit.getOfflinePlayer(playerUUID), amount).transactionSuccess();
    }

    @Override
    public double getBalance(@NotNull UUID playerUUID) {
        return economy.getBalance(Bukkit.getOfflinePlayer(playerUUID));
    }

    @Override
    public boolean withdrawPlayer(@NotNull UUID playerUUID, double amount, String reason) {
        return economy.withdrawPlayer(Bukkit.getOfflinePlayer(playerUUID), amount).transactionSuccess();
    }

    @Override
    public @NotNull String getCurrencySymbol() {
        return economy.currencyNamePlural();
    }

}
