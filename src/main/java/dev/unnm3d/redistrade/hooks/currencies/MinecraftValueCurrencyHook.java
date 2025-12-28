package dev.unnm3d.redistrade.hooks.currencies;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.GuiSettings;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.inventory.VirtualInventory;

import java.util.UUID;


public class MinecraftValueCurrencyHook extends CurrencyHook {
    public MinecraftValueCurrencyHook(String currencyName) {
        super(currencyName);
    }

    @Override
    public boolean depositPlayer(@NotNull UUID playerUUID, double amount, String reason) {
        // We don't support adding XP directly to the player, only through XP bottles in trades
        // This is to prevent XP to be given twice or only on one server
        return false;
    }

    @Override
    public double getBalance(@NotNull UUID playerUUID) {
        Player player = RedisTrade.getInstance().getServer().getPlayer(playerUUID);
        if (player == null) return 0D;
        return switch (name) {
            case "xp" -> player.calculateTotalExperiencePoints();
            //Keep the switch for new implementations
            default -> 0D;
        };
    }

    @Override
    public boolean withdrawPlayer(@NotNull UUID playerUUID, double amount, String reason) {
        Player player = RedisTrade.getInstance().getServer().getPlayer(playerUUID);
        if (player == null) return false;

        return switch (name) {
            case "xp":
                //Check if the player has enough of the currency
                if (getBalance(playerUUID) < (int) amount) yield false;
                player.setExperienceLevelAndProgress(player.calculateTotalExperiencePoints() - (int) amount);
                //Add xp bottle to the trade
                RedisTrade.getInstance().getTradeManager().getLatestTrade(playerUUID)
                  .ifPresent(trade -> {
                    VirtualInventory traderInventory = trade.getTradeSide(trade.getActor(player)).getOrder().getVirtualInventory();
                    ItemStack itemStack = new ItemStack(Material.EXPERIENCE_BOTTLE);
                    itemStack.editMeta(meta -> {
                        meta.displayName(MiniMessage.miniMessage().deserialize(GuiSettings.instance().xpBottleDisplayName
                          .replace("%amount%", String.valueOf((int) amount))));
                        meta.getPersistentDataContainer().set(new NamespacedKey("redistrade", "xp"), PersistentDataType.INTEGER, (int) amount);
                    });
                    if (traderInventory.addItem(null, itemStack) != 0) {
                        player.setExperienceLevelAndProgress(player.calculateTotalExperiencePoints() + (int) amount);
                    }
                });
                yield true;

                //Keep the switch for new implementations
            default:
                yield false;
        };
    }

    @Override
    public @NotNull String getCurrencySymbol() {
        return switch (name) {
            case "xp" -> "XP";
            //Keep the switch for new implementations
            default -> "";
        };
    }
}
