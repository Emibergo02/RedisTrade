package dev.unnm3d.redistrade.guis;

import dev.unnm3d.redistrade.configs.GuiSettings;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Markers;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.BoundItem;
import xyz.xenondevs.invui.item.Item;
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
    }

    public void openWindow(Player player) {
        Window.single()
                .setGui(gui)
                .setTitle("Trade Browser")
                .open(player);
    }

    public Item forwardItem() {
        return BoundItem.pagedGui()
                .setItemProvider(player -> GuiSettings.instance().nextPage.toItemBuilder())
                .build();
    }

    public Item backItem() {
        return BoundItem.pagedGui()
                .setItemProvider(player -> GuiSettings.instance().previousPage.toItemBuilder())
                .build();
    }
}
