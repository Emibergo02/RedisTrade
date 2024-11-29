package dev.unnm3d.redistrade.configs;


import de.exlll.configlib.Comment;
import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.Configuration;
import de.exlll.configlib.YamlConfigurations;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.item.builder.ItemBuilder;

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
            "REDIS - Redis storage (you can't use archived trades with this storage type)",
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

    @Comment({"Remember that a book line contains 20 large characters",
            "(if you use 'i's or 'l's it will be contain more characters)"})
    public List<List<String>> receiptIntestationFormat = List.of(
            List.of(
                    "Trade Receipt",
                    "",
                    "<black>Trader: <blue>%trader%</blue>",
                    "",
                    "<black>Target: <blue>%target%</blue>",
                    "",
                    "Date: ",
                    "<blue>%timestamp%</blue>",
                    "",
                    "Trader price: <gold>%trader_price%</gold>",
                    "Target price: <gold>%target_price%</gold>"
            )
    );

    public String receiptBookDisplayName = "<white>%trader%'s Receipt";

    @Comment({"Remember that a book line contains 20 large characters",
            "(if you use 'i's or 'l's it will be contain more characters)"})
    public List<String> receiptBookLore = List.of(
            "Trader: <blue>%trader%</blue>",
            "Target: <blue>%target%</blue>",
            "Date: ",
            "<blue>%timestamp%</blue>",
            "Trader price: <gold>%trader_price%</gold>",
            "Target price: <gold>%target_price%</gold>",
            "Exchanged items:",
            "%items%"
    );
    public String itemDisplayLoreFormat = "<!i><gray>[x%amount% %item_display%]";

    public String traderItemsIntestation = "<bold>Trader items: </bold>";
    public String targetItemsIntestation = "<bold>Target items: </bold>";
    @Comment("%item_name% - item name, %amount% - item amount, %display_name% - item display name")
    public String itemFormat = "<dark_gray>[x%amount% %item_name%]";

    public String tradeGuiTitle = "Trading with %player%";
    public List<String> tradeGuiStructure = List.of(
            "CM##D##mc",
            "LLLLxRRRR",
            "LLLLxRRRR",
            "LLLLxRRRR",
            "LLLLxRRRR",
            "LLLLxRRRR");

    public Map<ButtonType, ItemStack> buttons = Map.ofEntries(
            Map.entry(ButtonType.CLOSE, getCloseButton()),
            Map.entry(ButtonType.NEXT_PAGE, getNextPageButton()),
            Map.entry(ButtonType.PREVIOUS_PAGE, getPreviousPageButton()),
            Map.entry(ButtonType.BORDER, new ItemStack(Material.BLACK_STAINED_GLASS_PANE)),
            Map.entry(ButtonType.MONEY_BUTTON, getMoneyButton()),
            Map.entry(ButtonType.REFUTE_BUTTON, getRefuteButton()),
            Map.entry(ButtonType.COMPLETED_BUTTON, getCompletedButton()),
            Map.entry(ButtonType.CONFIRM_BUTTON, getConfirmButton()),
            Map.entry(ButtonType.RETRIEVED_BUTTON, getRetrievedButton()),
            Map.entry(ButtonType.CANCEL_TRADE_BUTTON, getCancelTradeButton()),
            Map.entry(ButtonType.MONEY_DISPLAY, getMoneyDisplay()),
            Map.entry(ButtonType.MONEY_CONFIRM_BUTTON, getMoneyConfirmButton()),
            Map.entry(ButtonType.SEPARATOR, getTradeBackground())
    );

    public String defaultCurrency = "default";
    public boolean debug = false;

    public ItemStack getButton(ButtonType buttonType) {
        return buttons.getOrDefault(buttonType, new ItemBuilder(Material.BARRIER).setDisplayName("§cItem is not set").get());
    }

    private ItemStack getTradeBackground() {
        return new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE).setDisplayName("").get();
    }

    public ItemStack getCloseButton() {
        return new ItemBuilder(Material.BARRIER).setDisplayName("§cClose").get();
    }

    public ItemStack getNextPageButton() {
        return new ItemBuilder(Material.ARROW).setDisplayName("§3Next Page").get();
    }

    public ItemStack getPreviousPageButton() {
        return new ItemBuilder(Material.ARROW).setDisplayName("§3Previous Page").get();
    }

    public ItemStack getRefuteButton() {
        return new ItemBuilder(Material.RED_WOOL)
                .setDisplayName("§cRefuted trade")
                .setLegacyLore(List.of("", "§fClick to §2confirm §fthe trade"))
                .get();
    }

    public ItemStack getConfirmButton() {
        return new ItemBuilder(Material.GREEN_WOOL)
                .setDisplayName("§aConfirmed trade")
                .setLegacyLore(List.of("", "§fClick to §crefute §fthe trade"))
                .get();
    }

    public ItemStack getCompletedButton() {
        return new ItemBuilder(Material.BLUE_WOOL)
                .setDisplayName("§bCompleted trade")
                .setLegacyLore(List.of("", "§fThe trade has been completed"))
                .get();
    }

    public ItemStack getRetrievedButton() {
        return new ItemBuilder(Material.GRAY_WOOL)
                .setDisplayName("§aItems retrieved")
                .get();
    }

    public ItemStack getCancelTradeButton() {
        return new ItemBuilder(Material.BARRIER)
                .setDisplayName("§cCancel trade")
                .get();
    }

    public ItemStack getMoneyButton() {
        return new ItemBuilder(Material.GOLD_NUGGET)
                .setDisplayName("§fCurrent price: §6%price%")
                .setLegacyLore(List.of("", "§fClick to §2set §fthe price"))
                .get();
    }

    public ItemStack getMoneyDisplay() {
        return new ItemBuilder(Material.GOLD_NUGGET)
                .setLegacyLore(List.of("§fClick to change currency", "§fSet your price"))
                .get();
    }

    public ItemStack getMoneyConfirmButton() {
        return new ItemBuilder(Material.GREEN_WOOL)
                .setDisplayName("§aConfirm price %price%")
                .setLegacyLore(List.of("", "§fConfirm your trade price"))
                .get();
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
