package dev.unnm3d.redistrade.guis.maingui.tradeslot;

import dev.unnm3d.redistrade.guis.maingui.ItemTrade;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.builder.ItemBuilder;

@Getter
public abstract class AbstractItemSlot extends ItemTrade {

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        event.setCancelled(false);
        final ItemStack cursorItem = event.getCursor();
        System.out.println(event.getSlot() + " " + event.getRawSlot());
        if (clickType.isRightClick()) {
            if (!cursorItem.isEmpty()) {
                if (itemBuilder.getAmount() == 0) {
                    itemBuilder = new ItemBuilder(cursorItem);
                    itemBuilder.setAmount(1);
                } else if (cursorItem.isSimilar(event.getCurrentItem())) {
                    itemBuilder.setAmount(itemBuilder.getAmount() + 1);
                }

            } else if (itemBuilder.getAmount() != 0) {
                itemBuilder.setAmount(itemBuilder.getAmount() / 2);
            }
        } else {
            itemBuilder = new ItemBuilder(cursorItem);
        }
        updateItem(event.getRawSlot(), itemBuilder.get());
    }

    public abstract void updateItem(int slot, ItemStack item);
}
