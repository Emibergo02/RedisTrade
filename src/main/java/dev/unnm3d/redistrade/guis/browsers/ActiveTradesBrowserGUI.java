package dev.unnm3d.redistrade.guis.browsers;

import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.guis.buttons.ActiveTradeButton;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;

import java.util.List;
import java.util.UUID;


public class ActiveTradesBrowserGUI extends AbstractBrowserGUI {

    public ActiveTradesBrowserGUI(UUID tradeOwner, List<NewTrade> activeTrades) {
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
            .map(trade -> (Item) new ActiveTradeButton(trade, trade.getActor(tradeOwner)))
            .toList())
          .build(), "Active Trade Browser");

    }

    public static void openBrowser(Player viewer, UUID tradesOwner, List<NewTrade> activeTrades) {
        new ActiveTradesBrowserGUI(tradesOwner, activeTrades).openWindow(viewer);
    }
}
