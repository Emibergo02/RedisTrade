package dev.unnm3d.redistrade;


import de.exlll.configlib.Comment;
import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.Configuration;
import de.exlll.configlib.YamlConfigurations;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

@Configuration
public class Settings {
    private static Settings SETTINGS;

    public static Settings instance(){
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


    public String databaseType = "sqlite";
    public MySQL mysql = new MySQL("localhost", 3306, "org.mariadb.jdbc.Driver",
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

    public Map<ButtonType, ItemStack> buttons = Map.ofEntries(
            Map.entry(ButtonType.CLOSE, new ItemStack(Material.BARRIER)),
            Map.entry(ButtonType.NEXT_PAGE, new ItemStack(Material.ARROW)),
            Map.entry(ButtonType.PREVIOUS_PAGE, new ItemStack(Material.ARROW)),
            Map.entry(ButtonType.SCROLL_NEXT, new ItemStack(Material.ARROW)),
            Map.entry(ButtonType.SCROLL_PREVIOUS, new ItemStack(Material.ARROW)),
            Map.entry(ButtonType.BACK, new ItemStack(Material.BARRIER)),
            Map.entry(ButtonType.BORDER, new ItemStack(Material.BLACK_STAINED_GLASS_PANE)),
            Map.entry(ButtonType.ORDERS_BUTTON, new ItemStack(Material.DIAMOND)),
            Map.entry(ButtonType.COMPLETED_ORDERS_BUTTON, new ItemStack(Material.DIAMOND)),
            Map.entry(ButtonType.MONEY_BUTTON, new ItemStack(Material.GOLD_NUGGET)),
            Map.entry(ButtonType.REFUTE_BUTTON, new ItemStack(Material.RED_WOOL)),
            Map.entry(ButtonType.CONFIRM_BUTTON, new ItemStack(Material.GREEN_WOOL))
    );

    public ItemStack ordersButton = new ItemStack(Material.DIAMOND);
    public ItemStack createOrder = new ItemStack(Material.GOLD_BLOCK);

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
         * MAIN MENU BUTTONS
         */
        ORDERS_BUTTON,
        COMPLETED_ORDERS_BUTTON,
        /**
         * TRADE MENU BUTTONS
         */
        MONEY_BUTTON,
        CONFIRM_BUTTON,
        REFUTE_BUTTON,
        ITEM_SLOT_BUTTON,


    }

}
