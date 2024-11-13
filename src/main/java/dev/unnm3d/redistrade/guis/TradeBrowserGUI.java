package dev.unnm3d.redistrade.guis;

import dev.unnm3d.redistrade.Settings;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.ItemWrapper;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;
import xyz.xenondevs.invui.window.Window;

import java.util.List;


public class TradeBrowserGUI {
    private final PagedGui<Item> gui;

    public TradeBrowserGUI(List<Item> receiptItems) {
        this.gui = PagedGui.items()
                .setStructure(
                        "# # # # # # # # #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# # # < # > # # #")
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL) // where paged items should be pu
                .addIngredient('<', backItem())
                .addIngredient('>', forwardItem())
                .setContent(receiptItems)
                .build();
        System.out.println("TradeBrowserGUI created");
    }

    public void openWindow(Player player) {
        Window.single()
                .setGui(gui)
                .setTitle("Trade Browser")
                .open(player);
    }

    public PageItem forwardItem(){
        return new PageItem(true) {
            @Override
            public ItemProvider getItemProvider(PagedGui<?> gui) {
                return new ItemWrapper(Settings.instance().getButton(Settings.ButtonType.NEXT_PAGE));
            }
        };
    }
    public PageItem backItem(){
        return new PageItem(false) {
            @Override
            public ItemProvider getItemProvider(PagedGui<?> gui) {
                return new ItemWrapper(Settings.instance().getButton(Settings.ButtonType.PREVIOUS_PAGE));
            }
        };
    }
}