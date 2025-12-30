package dev.unnm3d.redistrade.configs;


import de.exlll.configlib.Comment;
import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.Configuration;
import de.exlll.configlib.YamlConfigurations;
import dev.unnm3d.redistrade.restriction.KnownRestriction;
import dev.unnm3d.redistrade.utils.MyItemBuilder;
import org.apache.commons.lang3.LocaleUtils;
import org.bukkit.Material;

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
            .header("RedisTrade config")
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
      "MYSQL - MySQL or MariaDB storage",
      "POSTGRESQL - POSTGRESQL storage",
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

    @Comment("Skip invite requirements and instantly open the trade")
    public boolean skipInviteRequirements = false;
    @Comment("Instantly open the trade if target has accepted the invite")
    public boolean openTradeOnAccept = true;
    @Comment({"Instantly cancels the trade if a player closes the trade window",
      "EXPERIMENTAL: do not use this in production yet"})
    public boolean cancelOnClose = true;
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

    @Comment({"Currencies used in trades",
      "vault:default - Default currency from Vault",
      "rediseconomy:lyra - Custom currency from RedisEconomy",
      "playerpoints:default - Custom currency from PlayerPoints",
      "Leave this empty [] if you don't want to use any currency",
      "YOU MUST CHOOSE A DIFFERENT NAME FOR EACH CURRENCY",})
    public List<CurrencyItemSerializable> allowedCurrencies = List.of(
      new CurrencyItemSerializable("vault:default", "GOLD_INGOT", 0, "<gold>Money"),
      new CurrencyItemSerializable("minecraft:xp", "EXPERIENCE_BOTTLE", 0, "<green>Exp")
    );

    @Comment({"Component blacklist via regex, the trade will be closed if one of these regexes match the item data string",
      "if containsOnly is true, the string will not be treated as a regex match but as a simple contains check",
      "To inspect the item to see the data string, use /redistrade inspect with the item in your main hand",
      "Performance tips: avoid using too many regexes, avoid using complex regexes, prefer containsOnly when possible"})
    public List<BlacklistedItemRegex> blacklistedItemRegexes = List.of(
      BlacklistedItemRegex.regex(".*minecraft:flight_duration=\\d.*"),
      BlacklistedItemRegex.containsOnly("MMOITEMS_"));


    @Comment({"Action blacklist, the trade will be closed if one of these actions is detected",
      "Cooldown time is measured in milliseconds",
      "Remove an action to disable the restrict",
      "MOUNT and DISMOUNT are handled with MOUNT restriction"})
    public Map<String, Integer> actionCooldowns = Map.of(
      KnownRestriction.DAMAGED.toString(), 1000,
      KnownRestriction.COMBAT.toString(), 5000,
      KnownRestriction.MOVED.toString(), 400,
      KnownRestriction.MOUNT.toString(), 1000,
      KnownRestriction.WORLD_CHANGE.toString(), 1000,
      "WORLD_GUARD", 1000);

    @Comment({"Trade distance",
      "-1 for cross-server trades",
      "0 to trade only on the same world",
      ">0 to set the max distance allowed to trade"})
    public int tradeDistance = -1;

    @Comment({"Trade rating time in seconds after the trade is closed until the player can rate the trade",
      "After this time, the trade is considered expired and cannot be rated anymore",
      "-1 to disable the expiring time"})
    public int tradeReviewTime = 86400;

    @Comment("World blacklist, the trade will be closed if one of these worlds is detected")
    public List<String> worldBlacklist = List.of("world_nether", "world_the_end");

    @Comment("Command cooldown in milliseconds")
    public int commandCooldown = 1000;

    @Comment("If shift-clicking a player should open a trade")
    public boolean rightClickToOpen = true;

    public Map<String, List<String>> commandAliases = Map.of("trade", List.of("trade", "t"),
      "trade-ignore", List.of("trade-ignore", "tignore"),
      "trade-browse", List.of("trade-browse", "tbrowse"),
      "trade-spectate", List.of("trade-spectate", "tspec"),
      "trade-rate", List.of("trade-rate", "trate"));

    public boolean debug = true;
    public boolean debugStrace = false;

    public record CurrencyItemSerializable(String name, String material, int customModelData, String displayName) {
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

    public record BlacklistedItemRegex(String regex, boolean containsOnly) {
        public static BlacklistedItemRegex containsOnly(String checkString) {
            return new BlacklistedItemRegex(checkString, true);
        }

        public static BlacklistedItemRegex regex(String regex) {
            return new BlacklistedItemRegex(regex, false);
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
        POSTGRESQL,
    }

}
