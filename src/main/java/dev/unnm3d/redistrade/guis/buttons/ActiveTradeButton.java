package dev.unnm3d.redistrade.guis.buttons;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.core.TradeSide;
import dev.unnm3d.redistrade.utils.MyItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.AbstractItem;


public class ActiveTradeButton extends AbstractItem {
    private final NewTrade trade;

    public ActiveTradeButton(@NonNull NewTrade trade) {
        this.trade = trade;
    }

    @Override
    public ItemProvider getItemProvider() {
        return super.getItemProvider();
    }

    @Override
    public ItemProvider getItemProvider(Player viewer) {
        final TradeSide oppositeSide = trade.getTradeSide(trade.getActor(viewer).opposite());
        final ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        final SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setPlayerProfile(
          Bukkit.getOfflinePlayer(oppositeSide.getTraderUUID()).getPlayerProfile());
        head.setItemMeta(meta);

        MyItemBuilder builder = new MyItemBuilder(head);
        builder.setMiniMessageDisplayName("<aqua>" + oppositeSide.getTraderName());
        return builder;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        RedisTrade.getInstance().getTradeManager().openWindow(trade, player);
    }
}
