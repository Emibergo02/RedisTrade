package dev.unnm3d.redistrade.objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

@AllArgsConstructor
@Getter
@ToString
public class Trader {

    private final UUID uuid;
    private final String name;
    private final ItemStack receipt;

    public static Trader fromPlayer(Player player) {
        return new Trader(player.getUniqueId(), player.getName(),null);
    }

}
