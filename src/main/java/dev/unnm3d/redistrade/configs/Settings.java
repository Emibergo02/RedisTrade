package dev.unnm3d.redistrade.configs;


import de.exlll.configlib.Comment;
import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.Configuration;
import de.exlll.configlib.YamlConfigurations;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NBTType;
import de.tr7zw.changeme.nbtapi.iface.ReadableItemNBT;
import dev.unnm3d.redistrade.utils.MyItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Configuration
public class Settings {
    private static Settings SETTINGS;

    public static Settings instance() {
        return SETTINGS;
    }

    public static Settings initSettings(Path configFile) {
        SETTINGS = YamlConfigurations.update(
                configFile,
                Settings.class,
                ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder()
                        .header("RedisChat config")
                        .footer("Authors: Unnm3d")
                        .charset(StandardCharsets.UTF_8)
                        .build()
        );
        return SETTINGS;
    }

    @Comment({"Storage type for the plugin",
            "MYSQL - MySQL storage",
            "SQLITE - SQLite storage"})
    public StorageType storageType = StorageType.SQLITE;

    @Comment({"Cache type for the plugin",
            "REDIS - Redis cache",
            "PLUGIN_MESSAGE - Plugin message cache (not implemented yet)",
            "MEMORY - Memory cache (RAM) (does not enable cross-server features)"})
    public CacheType cacheType = CacheType.MEMORY;

    public MySQL mysql = new MySQL("localhost", 3306, "com.mysql.cj.jdbc.Driver",
            "redistrade", "root", "password",
            10, 10, 1800000, 0, 5000);

    @Comment("Leave password or user empty if you don't have a password or user")
    public Redis redis = new Redis("localhost",
            6379,
            "",
            "",
            0,
            1000,
            "RedisTrade",
            3);

    @Comment("Timezone for the trade receipt")
    public String timeZone = "GMT+1";
    @Comment("Date format for trade timestamps")
    public String dateFormat = "yyyy-MM-dd@HH:mm";
    @Comment("Decimal format for the trade receipt")
    public String decimalFormat = "#.##";

    @Comment("Allowed currencies for trades")
    public Map<String, CurrencyItemSerializable> allowedCurrencies = Map.of("default",
            new CurrencyItemSerializable("GOLD_INGOT", 0, "<gold>Money"),
            "emerald",
            new CurrencyItemSerializable("EMERALD", 0, "<green>Emeralds"));

    public List<BlacklistedItem> blacklistedItems = List.of(
            new BlacklistedItem("BARRIER", 0, List.of())
    );

    public boolean debug = false;

    public record CurrencyItemSerializable(String material, int customModelData, String displayName) {
        public MyItemBuilder toItemBuilder() {
            return new MyItemBuilder(Material.valueOf(material))
                    .setCustomModelData(customModelData)
                    .setMiniMessageItemName(displayName);
        }
    }

    public record MySQL(String databaseHost, int databasePort, String driverClass,
                        String databaseName, String databaseUsername, String databasePassword,
                        int maximumPoolSize, int minimumIdle, int maxLifetime,
                        int keepAliveTime, int connectionTimeout) {
    }

    public record Redis(String host, int port, String user, String password,
                        int database, int timeout,
                        String clientName, int poolSize) {
    }

    public record Tuple<T, U>(T first, U second) {
    }

    public record BlacklistedItem(String material, int customModelData, List<Tuple<String, String>> nbtBlacklist) {
        private boolean hasAllBlackListedNBTs(ReadableItemNBT NBTs) {
            for (Tuple<String, String> tuple : nbtBlacklist) {
                final String key = tuple.first;
                final String value = tuple.second;
                final NBTType type = NBTs.getType(value);
                //If the NBT is not present or the value is different return false
                if ((type == NBTType.NBTTagString && !NBTs.getString(key).equals(value)) ||
                        (type == NBTType.NBTTagInt && NBTs.getInteger(key) != Integer.parseInt(value)) ||
                        (type == NBTType.NBTTagByte && NBTs.getByte(key) != Byte.parseByte(value)) ||
                        (type == NBTType.NBTTagDouble && NBTs.getDouble(key) != Double.parseDouble(value)) ||
                        (type == NBTType.NBTTagFloat && NBTs.getFloat(key) != Float.parseFloat(value)) ||
                        (type == NBTType.NBTTagLong && NBTs.getLong(key) != Long.parseLong(value))) {
                    return false;
                }
            }
            return true;
        }

        public boolean isSimilar(ItemStack item) {
            return item.getType() == Material.valueOf(material) &&
                    item.getItemMeta().getCustomModelData() == customModelData &&
                    NBT.get(item, this::hasAllBlackListedNBTs);
        }
    }

    public enum ButtonType {
        /**
         * GENERAL BUTTONS
         */
        CLOSE,
        NEXT_PAGE,
        PREVIOUS_PAGE,
        SCROLL_NEXT,
        SCROLL_PREVIOUS,
        BACK,
        BORDER,
        /**
         * TRADE MENU BUTTONS
         */
        SEPARATOR,
        MONEY_BUTTON,
        CONFIRM_BUTTON,
        REFUTE_BUTTON,
        COMPLETED_BUTTON,
        RETRIEVED_BUTTON,
        CANCEL_TRADE_BUTTON,
        /**
         * MONEY EDITOR
         */
        MONEY_DISPLAY,
        MONEY_CONFIRM_BUTTON

    }

    public enum CacheType {
        REDIS,
        PLUGIN_MESSAGE,
        MEMORY,
    }

    public enum StorageType {
        MYSQL,
        SQLITE,
    }

}
