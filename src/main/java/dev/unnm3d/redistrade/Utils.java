package dev.unnm3d.redistrade;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

@UtilityClass
public class Utils {



    public Optional<Player> getPlayer(UUID uuid) {
        return Optional.ofNullable(Bukkit.getPlayer(uuid));
    }

    public Optional<Player> getPlayer(String playerName) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getName().equalsIgnoreCase(playerName))
                .map(player -> (Player) player)
                .findFirst();
    }

    public String serialize(ItemStack... items) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {

            dataOutput.writeInt(items.length);

            for (ItemStack item : items)
                dataOutput.writeObject(item);

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());

        } catch (Exception exception) {
            exception.printStackTrace();
            return "";
        }
    }

    public ItemStack[] deserialize(String source) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(source));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {

            final ItemStack[] items = new ItemStack[dataInput.readInt()];

            for (int i = 0; i < items.length; i++)
                items[i] = (ItemStack) dataInput.readObject();

            return items;
        } catch (Exception exception) {
            exception.printStackTrace();
            return new ItemStack[0];
        }
    }
    public ItemStack parseItemPlaceholders(ItemStack itemStack, Map<String, String> placeholders) {
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta.hasLore()) {
            final List<Component> lore = itemStack.getItemMeta().lore();
            if (lore != null) {
                for (int i = 0; i < lore.size(); i++) {
                    Component component = lore.get(i);
                    for (Map.Entry<String, String> stringStringEntry : placeholders.entrySet()) {
                        component = component.replaceText(builder -> builder.matchLiteral("%" + stringStringEntry.getKey() + "%")
                                .replacement(stringStringEntry.getValue()));
                    }
                    lore.set(i, component);
                }
            }
            itemMeta.lore(lore);
        }
        if (itemMeta.hasDisplayName()) {
            Component component = itemMeta.displayName();
            if (component != null) {
                for (Map.Entry<String, String> stringStringEntry : placeholders.entrySet()) {
                    component = component.replaceText(builder -> builder.matchLiteral("%" + stringStringEntry.getKey() + "%").replacement(stringStringEntry.getValue()));
                }
            }
            itemMeta.displayName(component);
        }

        if (itemMeta.hasItemName()) {
            Component component = itemMeta.itemName();
            for (Map.Entry<String, String> stringStringEntry : placeholders.entrySet()) {
                component = component.replaceText(builder -> builder.matchLiteral("%" + stringStringEntry.getKey() + "%").replacement(stringStringEntry.getValue()));
            }
            itemMeta.itemName(component);
        }
        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }
}
