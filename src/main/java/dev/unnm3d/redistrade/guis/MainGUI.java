package dev.unnm3d.redistrade.guis;

import dev.unnm3d.redistrade.fastinv.FastInv;
import dev.unnm3d.redistrade.fastinv.ItemBuilder;
import org.bukkit.Material;

public class MainGUI extends FastInv {

    public MainGUI() {
        super(27);
        setItem(11, new ItemBuilder(Material.DIAMOND)
                .name("§aCreate an order")
                .lore("§7Click here to create a new order").build(), e -> {

        });
        setItem(15, new ItemBuilder(Material.DIAMOND)
                .name("§aYour orders")
                .lore("§7Click here to see your orders").build(), e -> {

        });
        setItem(17, new ItemBuilder(Material.DIAMOND)
                .name("§aYour items")
                .lore("§7Click here to see your completed orders").build(), e -> {

        });
        setItem(19, new ItemBuilder(Material.DIAMOND)
                .name("§aYour reviews")
                .lore("§7Click here to see your completed orders").build(), e -> {

        });
    }

}
