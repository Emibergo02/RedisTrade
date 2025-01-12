package dev.unnm3d.redistrade.configs;


import de.exlll.configlib.Comment;
import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.Configuration;
import de.exlll.configlib.YamlConfigurations;
import dev.unnm3d.redistrade.utils.MyItemBuilder;
import org.apache.commons.lang3.LocaleUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;

@Configuration
public class Settings {
    private static Settings SETTINGS;
    private static DecimalFormat DECIMAL_FORMAT;

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
        DECIMAL_FORMAT = (DecimalFormat) NumberFormat.getInstance(LocaleUtils.toLocale(SETTINGS.locale));
        DECIMAL_FORMAT.applyLocalizedPattern(SETTINGS.decimalFormat);
        return SETTINGS;
    }

    public static DecimalFormat getDecimalFormat() {
        return DECIMAL_FORMAT;
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
            false,
            "RedisTrade",
            3);

    @Comment("Show receipt button at the end of the trade")
    public boolean deliverReceipt = true;
    @Comment("Maximum number of receipt to be delivered to a single player")
    public int receiptDelivered = 3;
    @Comment("Timezone for the trade receipt")
    public String timeZone = "GMT+1";
    @Comment("Date format for trade timestamps")
    public String dateFormat = "yyyy-MM-dd@HH:mm";
    @Comment("Decimal format for the trade receipt")
    public String decimalFormat = "#.##";
    @Comment("Locale used for decimal format")
    public String locale = "en_US";

    @Comment("Allowed currencies for trades")
    public Map<String, CurrencyItemSerializable> allowedCurrencies = Map.of("default",
            new CurrencyItemSerializable("GOLD_INGOT", 0, "<gold>Money"));

    @Comment("Component blacklist will come in the future")
    public List<BlacklistedItem> blacklistedItems = List.of(
            new BlacklistedItem("FIREWORK_ROCKET", 0, Map.of("flight_duration", "3"))
    );

    public boolean debug = true;
    public boolean debugStrace = false;

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
                        int database, int timeout, boolean ssl,
                        String clientName, int poolSize) {
    }

    public record BlacklistedItem(String material, int customModelData, Map<String, String> componentBlacklist) {
        public boolean isSimilar(ItemStack item) {
            boolean modelData = item.getItemMeta().hasCustomModelData() ?
                    item.getItemMeta().getCustomModelData() == customModelData : true;
            return item.getType() == Material.valueOf(material) &&
                    modelData;
        }
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
