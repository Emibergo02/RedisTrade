package dev.unnm3d.redistrade.objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@AllArgsConstructor
@Getter
@ToString
public class Order {

    private final int id;
    private final long timestamp;
    private final Trader buyer;
    private final Trader seller;
    private final List<ItemStack> items;
    private final boolean collected;
    private final boolean completed;
    private final short review;
    private double offer;

    public void modOffer(double mod) {
        this.offer += mod;
    }


}
