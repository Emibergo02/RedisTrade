package dev.unnm3d.redistrade.guis.maingui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;

public class ItemTrade extends AbstractItem {
    protected ItemBuilder itemBuilder;

    public ItemTrade(ItemStack itemStack) {
        this.itemBuilder = new ItemBuilder(itemStack);
    }
    public ItemTrade() {
        this.itemBuilder = new ItemBuilder(Material.AIR).setAmount(0);
    }

    @Override
    public ItemProvider getItemProvider() {
        return itemBuilder;
    }

    public void notifyWindows(ItemStack itemStack) {
        itemBuilder = new ItemBuilder(itemStack);
        notifyWindows();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {

    }
}
