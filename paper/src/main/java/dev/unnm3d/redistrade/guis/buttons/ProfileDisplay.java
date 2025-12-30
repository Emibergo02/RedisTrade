package dev.unnm3d.redistrade.guis.buttons;

import com.destroystokyo.paper.profile.PlayerProfile;
import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.GuiSettings;
import dev.unnm3d.redistrade.core.TradeSide;
import dev.unnm3d.redistrade.utils.MyItemBuilder;
import dev.unnm3d.redistrade.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.AbstractItem;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;


public class ProfileDisplay extends AbstractItem {
    private static final ConcurrentHashMap<UUID, PlayerTextures> playerTextures = new ConcurrentHashMap<>();
    private final TradeSide tradeSide;
    private ItemProvider playerHead;

    public ProfileDisplay(TradeSide tradeSide) {
        this.tradeSide = tradeSide;
        CompletableFuture.supplyAsync(() -> {
            try {
                return new MyItemBuilder(getHeadItemStack());
            } catch (Exception e) {
                RedisTrade.debug("Error while getting player head: " + e.getMessage());
                return new MyItemBuilder(Material.PLAYER_HEAD);
            }
        }).thenCombineAsync(RedisTrade.getInstance().getDataStorage().getMeanRating(tradeSide.getTraderUUID()), (builder, meanRating) -> {
            builder.setMiniMessageDisplayName(GuiSettings.instance().playerProfile.itemName()
              .replace("%player_name%", tradeSide.getTraderName()));
            builder.addMiniMessageLoreLines(GuiSettings.instance().playerProfile.lore().stream()
              .map(loreLine -> loreLine.replace("%player_name%", tradeSide.getTraderName())
                .replace("%stars%", Utils.starsOf(meanRating.mean()))
                .replace("%rating%", String.valueOf(meanRating.mean()))
                .replace("%trade_count%", String.valueOf(meanRating.countedTrades())))
              .toArray(String[]::new));
            return builder;
        }).thenAccept(item -> {
            playerHead = item;
            notifyWindows();
        });
    }

    @Override
    public ItemProvider getItemProvider() {
        return this.playerHead == null ? new MyItemBuilder(Material.PLAYER_HEAD) : this.playerHead;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
    }

    private ItemStack getHeadItemStack() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        final PlayerProfile profile = Bukkit.getOfflinePlayer(tradeSide.getTraderUUID()).getPlayerProfile();

        if (profile.getTextures().getSkin() == null) {
            Optional.ofNullable(playerTextures.get(tradeSide.getTraderUUID()))
              .ifPresentOrElse(profile::setTextures, () -> {
                  PlayerTextures textures = profile.update().join().getTextures();
                  profile.setTextures(textures);
                  playerTextures.put(tradeSide.getTraderUUID(), textures);
              });
        }
        if (item.getItemMeta() instanceof SkullMeta sm) {
            sm.setPlayerProfile(profile);
            item.setItemMeta(sm);
        }
        return item;
    }

}
