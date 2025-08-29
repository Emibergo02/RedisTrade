package dev.unnm3d.redistrade.hooks;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.core.enums.Actor;
import dev.unnm3d.redistrade.guis.BedrockMoneySelectorGUI;
import dev.unnm3d.redistrade.guis.MoneySelectorGUI;
import dev.unnm3d.redistrade.hooks.currencies.*;
import dev.unnm3d.redistrade.utils.MyItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class IntegrationManager {
    private final RedisTrade plugin;
    /**
     * Currency names for each hook
     * Key is the currency name, value is the hook name
     */
    private final ConcurrentSkipListMap<String, CurrencyHook> currencies;
    private final ConcurrentHashMap<String, MyItemBuilder> displayItems;

    public IntegrationManager(RedisTrade redisTrade) {
        this.plugin = redisTrade;
        this.currencies = new ConcurrentSkipListMap<>();
        this.displayItems = new ConcurrentHashMap<>();

        for (Settings.CurrencyItemSerializable currencyConfig : Settings.instance().allowedCurrencies) {
            final String[] split = currencyConfig.name().split(":"); // <plugin>:<currency>
            if (split.length != 2) {
                plugin.getLogger().warning("Invalid currency name: " + currencyConfig.name() + ". Must be in the format <plugin>:<currency>");
                continue;
            }
            try {
                final CurrencyHook currencyHook = createCurrencyHook(split[0], split[1]);
                addCurrencyHook(split[1], currencyHook, currencyConfig.toItemBuilder());
            } catch (Exception e) {
                plugin.getLogger().warning("Error creating currency hook for " + currencyConfig.name() + ": " + e.getMessage());
            }
        }
    }

    private CurrencyHook createCurrencyHook(String hookName, String currencyName) {
        return switch (hookName.toLowerCase()) {
            case "vault" -> new VaultCurrencyHook(plugin, currencyName);
            case "playerpoints" -> new PlayerPointsCurrencyHook(currencyName);
            case "rediseconomy" -> new RedisEconomyCurrencyHook(currencyName);
            case "minecraft" -> new MinecraftValueCurrencyHook(currencyName);
            default -> throw new IllegalStateException("Unexpected currency plugin: " + hookName.toLowerCase());
        };
    }

    public CurrencyHook getCurrencyHook(String currencyName) {
        return currencies.getOrDefault(currencyName, new EmptyCurrencyHook());
    }

    public MyItemBuilder getDisplayItem(String currencyName) {
        return displayItems.getOrDefault(currencyName, new MyItemBuilder(Material.BARRIER));
    }

    /**
     * Add a currency hook to the manager
     * If the currency hook already exists, it will be ignored
     *
     * @param currencyName The name of the currency
     * @param currencyHook The currency hook
     * @param displayItem The display item for the currency
     */
    public void addCurrencyHook(String currencyName, CurrencyHook currencyHook, MyItemBuilder displayItem) {
        if (currencies.containsKey(currencyName)) {
            plugin.getLogger().warning("Currency hook for " + currencyName + " already exists. Ignoring.");
            return;
        }
        currencies.put(currencyName, currencyHook);
        displayItems.put(currencyName, displayItem);
    }

    public void openMoneySelectorGUI(NewTrade trade, Actor playerSide, Player player, String currencyName) {
        boolean floodgateEnabled = plugin.getServer().getPluginManager().isPluginEnabled("floodgate");
        boolean isBedrockPlayer = floodgateEnabled && BedrockMoneySelectorGUI.isBedrockPlayer(player.getUniqueId());
        if (isBedrockPlayer) {
            new BedrockMoneySelectorGUI(trade, playerSide, player, currencyName).openWindow();
        } else {
            new MoneySelectorGUI(trade, playerSide, player, currencyName).openWindow();
        }
    }

    public Set<String> getCurrencyNames() {
        return currencies.keySet();
    }

    public List<CurrencyHook> getCurrencyHooks() {
        return new ArrayList<>(currencies.values());
    }

}
