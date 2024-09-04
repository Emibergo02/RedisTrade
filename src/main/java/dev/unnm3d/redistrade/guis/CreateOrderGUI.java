package dev.unnm3d.redistrade.guis;

import dev.unnm3d.redistrade.fastinv.ItemBuilder;
import dev.unnm3d.redistrade.fastinv.StructuredGui;
import dev.unnm3d.redistrade.fastinv.guielements.StorageElement;
import dev.unnm3d.redistrade.objects.Order;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;

public class CreateOrderGUI extends StructuredGui {

    /**
     * #########
     * ####I####
     * 1234P5678
     * ##X###V##
     */
    private final Order currentOrder;

    public CreateOrderGUI(Order currentOrder) {
        super("Create Order",
                "#########",
                "####I####",
                "1234P5678",
                "##X###V##");
        this.currentOrder = currentOrder;
        setIngredient('I', new StorageElement());
        drawPriceSelector();
        drawOfferDisplay();

    }

    public void drawOfferDisplay() {
        setIngredient('P', new ItemBuilder(Material.GOLD_INGOT)
                .name("§b" + currentOrder.getOffer() + "$")
                .build(), null);
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        if (!this.getInventory().equals(event.getClickedInventory()) && !event.getClick().isShiftClick()) {
            event.setCancelled(false);
        }
    }


    public void drawPriceSelector() {
        setIngredient('1', new ItemBuilder(Material.GOLD_NUGGET)
                        .name("§c-100$")
                        .build()
                , e -> {
                    currentOrder.modOffer(-1000d);
                    drawOfferDisplay();
                });
        setIngredient('2', new ItemBuilder(Material.GOLD_NUGGET)
                        .name("§c-100$")
                        .build()
                , e -> {
                    currentOrder.modOffer(-1000d);
                    drawOfferDisplay();
                });
        setIngredient('3', new ItemBuilder(Material.GOLD_NUGGET)
                        .name("§c-10$")
                        .build()
                , e -> {
                    currentOrder.modOffer(-100d);
                    drawOfferDisplay();
                });
        setIngredient('4', new ItemBuilder(Material.GOLD_NUGGET)
                        .name("§c-1$")
                        .build()
                , e -> {
                    currentOrder.modOffer(-1d);
                    drawOfferDisplay();
                });

        setIngredient('5', new ItemBuilder(Material.GOLD_NUGGET)
                        .name("§a+1$")
                        .build()
                , e -> {
                    currentOrder.modOffer(+1d);
                    drawOfferDisplay();
                });
        setIngredient('6', new ItemBuilder(Material.GOLD_NUGGET)
                        .name("§a+10$")
                        .build()
                , e -> {
                    currentOrder.modOffer(+100d);
                    drawOfferDisplay();
                });
        setIngredient('7', new ItemBuilder(Material.GOLD_NUGGET)
                        .name("§a+100$")
                        .build()
                , e -> {
                    currentOrder.modOffer(+1000d);
                    drawOfferDisplay();
                });
    }
}
