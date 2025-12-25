package dev.unnm3d.redistrade.guis.browsers;

import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.guis.buttons.ActiveTradeButton;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;

import java.util.List;


public class ActiveTradesBrowserGUI extends AbstractBrowserGUI {

    public ActiveTradesBrowserGUI(List<NewTrade> activeTrades) {
        super(PagedGui.items()
          .setStructure(
            "# # # # # # # # #",
            "# x x x x x x x #",
            "# x x x x x x x #",
            "# # # < # > # # #")
          .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL) // where paged items should be pu
          .addIngredient('<', backItem())
          .addIngredient('>', forwardItem())
          .setContent(activeTrades.stream()
            .map(trade -> (Item) new ActiveTradeButton(trade))
            .toList())
          .build(), "Active Trade Browser");

    }

    public static void openBrowser(Player player, List<NewTrade> activeTrades) {
        new ActiveTradesBrowserGUI(activeTrades).openWindow(player);
    }
}
