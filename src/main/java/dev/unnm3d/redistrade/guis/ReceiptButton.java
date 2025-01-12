package dev.unnm3d.redistrade.guis;

import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.utils.ReceiptBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.ItemWrapper;
import xyz.xenondevs.invui.item.impl.AbstractItem;

public class ReceiptButton extends AbstractItem {
    private final ItemStack receipt;
    private int retrieveTimes = 0;

    public ReceiptButton(NewTrade trade) {
        this.receipt = ReceiptBuilder.buildReceipt(trade, System.currentTimeMillis()).getItemProvider().get();
    }

    @Override
    public @NotNull ItemProvider getItemProvider() {
        return new ItemWrapper(receipt);
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (retrieveTimes++ >= Settings.instance().receiptDelivered) return;
        // Return the cursor item to the player's inventory or drop it if full
        player.getInventory().addItem(player.getItemOnCursor()).values().forEach(itemStack ->
                player.getWorld().dropItem(player.getLocation(), itemStack)
        );
        player.setItemOnCursor(receipt);
    }
}
