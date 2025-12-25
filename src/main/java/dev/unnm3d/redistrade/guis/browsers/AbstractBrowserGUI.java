package dev.unnm3d.redistrade.guis.browsers;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.GuiSettings;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;
import xyz.xenondevs.invui.window.Window;

@AllArgsConstructor
public abstract class AbstractBrowserGUI {
    public final PagedGui<Item> gui;
    public final String title;

    public void openWindow(Player player) {
        Bukkit.getScheduler().runTask(RedisTrade.getInstance(), () ->
          Window.single()
            .setGui(this.gui)
            .setTitle(this.title)
            .open(player));
    }

    protected static Item forwardItem() {
        return new PageItem(true) {
            @Override
            public ItemProvider getItemProvider(PagedGui<?> gui) {
                return GuiSettings.instance().nextPage.toItemBuilder();
            }
        };
    }

    protected static Item backItem() {
        return new PageItem(false) {
            @Override
            public ItemProvider getItemProvider(PagedGui<?> gui) {
                return GuiSettings.instance().previousPage.toItemBuilder();
            }
        };
    }
}
