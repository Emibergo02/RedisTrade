package dev.unnm3d.redistrade;


import de.exlll.configlib.Configuration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

@Configuration
public class Settings {

    public String databaseType = "sqlite";
    public MySQL mysql = new MySQL("localhost", 3306, "org.mariadb.jdbc.Driver",
            "redistrade", "root", "password",
            10, 10, 1800000, 0, 5000);

    public Map<ButtonType, ItemStack> buttons = Map.of(
            ButtonType.CLOSE, new ItemStack(Material.BARRIER),
            ButtonType.NEXT_PAGE, new ItemStack(Material.ARROW),
            ButtonType.PREVIOUS_PAGE, new ItemStack(Material.ARROW),
            ButtonType.SCROLL_NEXT, new ItemStack(Material.ARROW),
            ButtonType.SCROLL_PREVIOUS, new ItemStack(Material.ARROW),
            ButtonType.BACK, new ItemStack(Material.BARRIER),
            ButtonType.BORDER, new ItemStack(Material.BLACK_STAINED_GLASS_PANE),
            ButtonType.ORDERS_BUTTON, new ItemStack(Material.DIAMOND),
            ButtonType.COMPLETED_ORDERS_BUTTON, new ItemStack(Material.DIAMOND)
    );
    public ItemStack ordersButton = new ItemStack(Material.DIAMOND);
    public ItemStack createOrder = new ItemStack(Material.GOLD_BLOCK);

    public record MySQL(String databaseHost, int databasePort, String driverClass,
                        String databaseName, String databaseUsername, String databasePassword,
                        int maximumPoolSize, int minimumIdle, int maxLifetime,
                        int keepAliveTime, int connectionTimeout) {
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
        COMPLETED_ORDERS_BUTTON
    }

}
