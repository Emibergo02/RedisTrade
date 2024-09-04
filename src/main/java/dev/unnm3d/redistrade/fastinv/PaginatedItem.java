package dev.unnm3d.redistrade.fastinv;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public record PaginatedItem(ItemStack itemStack, Consumer<InventoryClickEvent> eventConsumer) {
}