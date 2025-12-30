package dev.unnm3d.redistrade.guis.browsers;

import dev.unnm3d.redistrade.core.ArchivedTrade;
import dev.unnm3d.redistrade.utils.ReceiptBuilder;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;

import java.util.List;


public class ArchivedTradesBrowserGUI extends AbstractBrowserGUI {

    public ArchivedTradesBrowserGUI(List<ArchivedTrade> archivedTrades) {
        super(PagedGui.items()
          .setStructure(
            "# # # # # # # # #",
            "# x x x x x x x #",
            "# x x x x x x x #",
            "# # # < # > # # #")
          .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL) // where paged items should be pu
          .addIngredient('<', backItem())
          .addIngredient('>', forwardItem())
          .setContent(archivedTrades.stream()
            .map(entry -> ReceiptBuilder.buildReceipt(entry.getTrade(), entry.getArchivedAt()))
            .toList())
          .build(), "Archived Trade Browser");
    }

    public static void openBrowser(Player player, List<ArchivedTrade> archivedTrades) {
        new ArchivedTradesBrowserGUI(archivedTrades).openWindow(player);
    }
}
