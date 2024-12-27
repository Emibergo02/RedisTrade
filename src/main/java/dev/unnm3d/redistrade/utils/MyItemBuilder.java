package dev.unnm3d.redistrade.utils;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;

import java.util.*;
import java.util.function.Function;

@Getter
@SuppressWarnings("unused")
public class MyItemBuilder implements ItemProvider {
    protected ItemStack base;
    protected Material material;
    protected int amount = 1;
    protected int damage;
    protected int customModelData;
    protected Boolean unbreakable;
    protected Component displayName;
    protected Component itemName;
    protected List<Component> lore;
    protected List<ItemFlag> itemFlags;
    protected HashMap<Enchantment, Map.Entry<Integer, Boolean>> enchantments;
    protected List<Function<ItemStack, ItemStack>> modifiers;


    public MyItemBuilder(@NotNull Material material) {
        this.material = material;
    }

    public MyItemBuilder(@NotNull Material material, int amount) {
        this.material = material;
        this.amount = amount;
    }

    public MyItemBuilder(@NotNull ItemStack base) {
        this.base = base.clone();
        this.amount = base.getAmount();
    }

    @Override
    public @NotNull ItemStack get(@NotNull Locale locale) {
        return get();
    }

    @Contract(
            value = " -> new",
            pure = true
    )
    public @NotNull ItemStack get() {
        ItemStack itemStack;
        if (this.base != null) {
            itemStack = this.base;
            itemStack.setAmount(this.amount);
        } else {
            itemStack = new ItemStack(this.material, this.amount);
        }

        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            if (this.displayName != null) {
                itemMeta.displayName(this.displayName);
            }

            if (this.itemName != null) {
                itemMeta.itemName(this.itemName);
            }

            if (this.lore != null) {
                itemMeta.lore(this.lore);
            }

            if (itemMeta instanceof Damageable) {
                ((Damageable) itemMeta).setDamage(this.damage);
            }

            if (this.customModelData != 0) {
                itemMeta.setCustomModelData(this.customModelData);
            }

            if (this.unbreakable != null) {
                itemMeta.setUnbreakable(this.unbreakable);
            }

            if (this.enchantments != null) {
                if (this.base != null) {
                    itemMeta.getEnchants().forEach((enchantment, level) ->
                            itemMeta.removeEnchant(enchantment));
                }

                this.enchantments.forEach((enchantment, pair) ->
                        itemMeta.addEnchant(enchantment, pair.getKey(), pair.getValue()));
            }

            if (this.itemFlags != null) {
                if (this.base != null) {
                    itemMeta.removeItemFlags(itemMeta.getItemFlags().toArray(new ItemFlag[0]));
                }

                itemMeta.addItemFlags(this.itemFlags.toArray(new ItemFlag[0]));
            }

            itemStack.setItemMeta(itemMeta);
        }

        Function<ItemStack, ItemStack> modifier;
        if (this.modifiers != null) {
            for (Iterator<Function<ItemStack, ItemStack>> var6 = this.modifiers.iterator(); var6.hasNext(); itemStack = modifier.apply(itemStack)) {
                modifier = var6.next();
            }
        }

        return itemStack;
    }

    @Contract("_ -> this")
    public @NotNull MyItemBuilder removeLoreLine(int index) {
        if (this.lore != null) {
            this.lore.remove(index);
        }

        return this;
    }

    @Contract("_, _ -> this")
    public @NotNull MyItemBuilder setLoreLine(int index, Component line) {
        if (this.lore != null) {
            this.lore.set(index, line);
        }

        return this;
    }

    @Contract("-> this")
    public @NotNull MyItemBuilder clearLore() {
        if (this.lore != null) {
            this.lore.clear();
        }

        return this;
    }

    @Contract("_ -> this")
    public @NotNull MyItemBuilder setDisplayName(@NotNull Component displayName) {
        this.displayName = displayName;
        return this;
    }

    @Contract("_ -> this")
    public @NotNull MyItemBuilder setItemName(@NotNull Component itemName) {
        this.itemName = itemName;
        return this;
    }

    @Contract("_ -> this")
    public @NotNull MyItemBuilder setMiniMessageItemName(@NotNull String itemName) {
        this.itemName = MiniMessage.miniMessage().deserialize(itemName);
        return this;
    }

    @Contract("_ -> this")
    public @NotNull MyItemBuilder setLegacyDisplayName(@NotNull String displayName) {
        this.displayName = LegacyComponentSerializer.legacyAmpersand().deserialize(displayName);
        return this;
    }

    @Contract("_ -> this")
    public @NotNull MyItemBuilder setMiniMessageDisplayName(@NotNull String displayName) {
        this.displayName = MiniMessage.miniMessage().deserialize(displayName);
        return this;
    }

    @Contract("_ -> this")
    public @NotNull MyItemBuilder setMaterial(@NotNull Material material) {
        this.material = material;
        return this;
    }

    @Contract("_ -> this")
    public @NotNull MyItemBuilder setAmount(int amount) {
        this.amount = amount;
        return this;
    }

    @Contract("_ -> this")
    public @NotNull MyItemBuilder setDamage(int damage) {
        this.damage = damage;
        return this;
    }

    @Contract("_ -> this")
    public @NotNull MyItemBuilder setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
        return this;
    }

    @Contract("_ -> this")
    public @NotNull MyItemBuilder setUnbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
        return this;
    }


    @Contract("_ -> this")
    public @NotNull MyItemBuilder addMiniMessageLoreLines(String... lines) {
        if (this.lore == null) {
            this.lore = new ArrayList<>();
        }

        for (String line : lines) {
            this.lore.add(MiniMessage.miniMessage().deserialize("<!i><white>" + line));
        }

        return this;
    }

    @Contract("_ -> this")
    public @NotNull MyItemBuilder addLoreLines(Component... lines) {
        if (this.lore == null) {
            this.lore = new ArrayList<>();
        }

        this.lore.addAll(Arrays.asList(lines));

        return this;
    }


    @Contract("_, _ -> this")
    public @NotNull MyItemBuilder addLegacyLoreLines(char prefix, String... lines) {
        if (this.lore == null) {
            this.lore = new ArrayList<>();
        }
        for (String line : lines) {
            this.lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
        }
        return this;
    }

    @Contract("_ -> this")
    public @NotNull MyItemBuilder setItemFlags(@NotNull List<ItemFlag> itemFlags) {
        this.itemFlags = itemFlags;
        return this;
    }

    @Contract("_ -> this")
    public @NotNull MyItemBuilder addItemFlags(ItemFlag... itemFlags) {
        if (this.itemFlags == null) {
            this.itemFlags = new ArrayList<>();
        }

        this.itemFlags.addAll(Arrays.asList(itemFlags));
        return this;
    }

    @Contract("-> this")
    public @NotNull MyItemBuilder addAllItemFlags() {
        this.itemFlags = new ArrayList<>(Arrays.asList(ItemFlag.values()));
        return this;
    }

    @Contract("_ -> this")
    public @NotNull MyItemBuilder removeItemFlags(ItemFlag... itemFlags) {
        if (this.itemFlags != null) {
            this.itemFlags.removeAll(Arrays.asList(itemFlags));
        }

        return this;
    }

    @Contract("-> this")
    public @NotNull MyItemBuilder clearItemFlags() {
        if (this.itemFlags != null) {
            this.itemFlags.clear();
        }

        return this;
    }

    @Contract("_ -> this")
    public @NotNull MyItemBuilder setEnchantments(@NotNull HashMap<Enchantment, Map.Entry<Integer, Boolean>> enchantments) {
        this.enchantments = enchantments;
        return this;
    }

    @Contract("_, _, _ -> this")
    public @NotNull MyItemBuilder addEnchantment(Enchantment enchantment, int level, boolean ignoreLevelRestriction) {
        if (this.enchantments == null) {
            this.enchantments = new HashMap<>();
        }

        this.enchantments.put(enchantment, Map.entry(level, ignoreLevelRestriction));
        return this;
    }

    @Contract("_ -> this")
    public @NotNull MyItemBuilder removeEnchantment(Enchantment enchantment) {
        if (this.enchantments != null) {
            this.enchantments.remove(enchantment);
        }

        return this;
    }

    @Contract("-> this")
    public @NotNull MyItemBuilder clearEnchantments() {
        if (this.enchantments != null) {
            this.enchantments.clear();
        }

        return this;
    }

    @Contract("_ -> this")
    public @NotNull MyItemBuilder addModifier(Function<ItemStack, ItemStack> modifier) {
        if (this.modifiers == null) {
            this.modifiers = new ArrayList<>();
        }

        this.modifiers.add(modifier);
        return this;
    }

    @Contract("-> this")
    public @NotNull MyItemBuilder clearModifiers() {
        if (this.modifiers != null) {
            this.modifiers.clear();
        }

        return this;
    }

    @Contract("_ -> this")
    public @NotNull MyItemBuilder replacePlaceholders(@NotNull Map<String, String> replacements) {

        if (this.displayName != null) {
            this.displayName = this.displayName.replaceText(builder -> {
                for (Map.Entry<String, String> entry : replacements.entrySet()) {
                    builder.matchLiteral(entry.getKey()).replacement(entry.getValue());
                }
            });
        }
        if (this.itemName != null) {
            this.itemName = this.itemName.replaceText(builder -> {
                for (Map.Entry<String, String> entry : replacements.entrySet()) {
                    builder.matchLiteral(entry.getKey()).replacement(entry.getValue());
                }
            });
        }
        if (this.lore != null) {
            final List<Component> newLore = new ArrayList<>();
            for (Component line : this.lore) {
                newLore.add(line.replaceText(builder -> {
                    for (Map.Entry<String, String> entry : replacements.entrySet()) {
                        builder.matchLiteral(entry.getKey()).replacement(entry.getValue());
                    }
                }));
            }
            this.lore = newLore;
        }
        return this;
    }

    @Contract("_ -> this")
    public @NotNull MyItemBuilder replaceComponentPlaceholders(@NotNull Map<String, Component> replacements) {

        if (this.displayName != null) {
            this.displayName = this.displayName.replaceText(builder -> {
                for (Map.Entry<String, Component> entry : replacements.entrySet()) {
                    builder.matchLiteral(entry.getKey()).replacement(entry.getValue());
                }
            });
        }
        if (this.itemName != null) {
            this.itemName = this.itemName.replaceText(builder -> {
                for (Map.Entry<String, Component> entry : replacements.entrySet()) {
                    builder.matchLiteral(entry.getKey()).replacement(entry.getValue());
                }
            });
        }
        if (this.lore != null) {
            final List<Component> newLore = new ArrayList<>();
            for (Component line : this.lore) {
                newLore.add(line.replaceText(builder -> {
                    for (Map.Entry<String, Component> entry : replacements.entrySet()) {
                        builder.matchLiteral(entry.getKey()).replacement(entry.getValue());
                    }
                }));
            }
            this.lore = newLore;
        }
        return this;
    }


}
