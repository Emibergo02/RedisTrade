package dev.unnm3d.redistrade.guis;

import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.AbstractGui;
import xyz.xenondevs.invui.gui.SlotElement;
import xyz.xenondevs.invui.inventory.Inventory;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.SimpleItem;

import java.util.*;

public class MutableGui extends AbstractGui {

    private final HashMap<Character, LinkedHashSet<ItemPosition>> itemPositionsMap;

    public MutableGui(@NotNull String... structureData) {
        super(sanitize(structureData[0]).length(), structureData.length);
        this.itemPositionsMap = new HashMap<>();

        for (int i = 0; i < structureData.length; i++) {
            final String row = sanitize(structureData[i]);
            for (int j = 0; j < row.length(); j++) {
                char c = row.charAt(j);
                final ItemPosition itemPosition = new ItemPosition(j, i);
                this.itemPositionsMap.compute(c, (key, value) -> {
                    if (value == null) {
                        return new LinkedHashSet<>(List.of(itemPosition));
                    } else {
                        value.add(itemPosition);
                        return value;
                    }
                });
            }
        }
    }

    public MutableGui setIngredient(char c, Item item) {
        LinkedHashSet<ItemPosition> itemPositions = itemPositionsMap.get(c);
        if (itemPositions == null) return this;

        itemPositions.forEach(itemPosition ->
                setSlotElement(itemPosition.x, itemPosition.y,
                        new SlotElement.ItemSlotElement(item)));
        return this;
    }

    public MutableGui setIngredient(char c, ItemProvider itemProvider) {
        return setIngredient(c, new SimpleItem(itemProvider));
    }

    public MutableGui setIngredient(char c, Inventory inventory) {
        LinkedHashSet<ItemPosition> itemPositions = itemPositionsMap.get(c);
        if (itemPositions == null) return this;

        int i = 0;
        for (ItemPosition itemPosition : itemPositions) {
            setSlotElement(itemPosition.x, itemPosition.y,
                    new SlotElement.InventorySlotElement(inventory, i++));
        }
        return this;
    }

    public MutableGui setIngredients(String chars, ItemProvider itemProvider) {
        return setIngredients(chars, new SimpleItem(itemProvider));
    }

    public MutableGui setIngredients(String chars, Item item) {
        chars = sanitize(chars);
        for (char c : chars.toCharArray()) {
            LinkedHashSet<ItemPosition> itemPositions = itemPositionsMap.get(c);
            if (itemPositions == null) {
                continue;
            }
            itemPositions.forEach(itemPosition -> setSlotElement(itemPosition.x, itemPosition.y,
                    new SlotElement.ItemSlotElement(item)));
        }
        return this;
    }

    public void notifyItem(char c) {
        Optional.ofNullable(itemPositionsMap.get(c))
                .ifPresent(itemPositions -> itemPositions.stream()
                        .map(itemPosition -> getItem(itemPosition.x, itemPosition.y))
                        .filter(Objects::nonNull)
                        .forEach(Item::notifyWindows));
    }


    private static String sanitize(String s) {
        return s.replace(" ", "").replace("\n", "");
    }

    private record ItemPosition(int x, int y) {
    }
}
