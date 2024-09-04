package dev.unnm3d.redistrade.fastinv;

import dev.unnm3d.redistrade.fastinv.guielements.GuiElement;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Getter
public class StructuredGui extends FastInv {
    @Setter
    private String[] structureMap;
    private final HashMap<Character, GuiElement> ingredients;

    public StructuredGui(String title, String... structureMap) {
        super(9 * (Math.max(structureMap.length, 3)), title);
        this.structureMap = structureMap;
        if (structureMap.length < 3) {
            this.structureMap = Arrays.copyOf(structureMap, 3);
            for (int i = 0; i < 3; i++) {
                if (this.structureMap[i].length() == 9) continue;
                this.structureMap[i] = "         ";
            }
        }

        this.ingredients = new HashMap<>();
    }

    public StructuredGui setIngredient(char c, ItemStack item, Consumer<InventoryClickEvent> clickHandler) {
        return setIngredient(c, new GuiElement(item, clickHandler));
    }

    public StructuredGui setIngredient(char c, GuiElement guiElement) {
        ingredients.put(c, guiElement);
        for (int i = 0; i < rows(); i++) {
            for (int j = 0; j < 9; j++) {
                if (c != structureMap[i].charAt(j)) continue;
                setItem(i * 9 + j, guiElement.getItem(), guiElement.getClickHandler());
            }
        }
        return this;
    }

    public StructuredGui fillIngredients(char c, List<GuiElement> elements) {
        //ingredients.put(c, elements.get(0));
        int elementsIndex = 0;
        for (int i = 0; i < rows(); i++) {
            for (int j = 0; j < 9; j++) {
                if (c != structureMap[i].charAt(j)) continue;
                setItem(i * 9 + j, elements.get(elementsIndex).getItem(), elements.get(elementsIndex).getClickHandler());
                elementsIndex++;
                if (elementsIndex == elements.size()) {
                    return this;
                }
            }
        }

        return this;
    }

    public StructuredGui setItem(int slot, char c, ItemStack item, Consumer<InventoryClickEvent> clickHandler) {
        structureMap[slot / 9] = structureMap[slot / 9].substring(0, slot % 9) + c + structureMap[slot / 9].substring(slot % 9 + 1);
        return setIngredient(c, item, clickHandler);
    }

    public void setIngredientClickHandler(char c, Consumer<InventoryClickEvent> clickHandler) {
        final ItemStack is = Optional.ofNullable(ingredients.get(c))
                .map(GuiElement::getItem)
                .orElse(new ItemBuilder(Material.BARRIER).name("§cItem not set").build());
        setIngredient(c, is, clickHandler);
    }

    public ItemStack getIngredient(char c) {
        return ingredients.get(c).getItem();
    }

    public int rows() {
        return structureMap.length;
    }


}
