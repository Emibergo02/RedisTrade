package dev.unnm3d.redistrade.guis;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.AbstractGui;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.structure.Structure;

public final class TradeGuiImpl extends AbstractGui {

    public TradeGuiImpl(@NotNull Structure structure) {
        super(structure.getWidth(), structure.getHeight());
        applyStructure(structure);
    }

    @Override
    public void handleClick(int slotNumber, Player player, ClickType clickType, InventoryClickEvent event) {
        int x = slotNumber % 9;
        int y = slotNumber / 9;
        if (x > 3 && y > 0) {
            event.setCancelled(true);
            return;
        }
        super.handleClick(slotNumber, player, clickType, event);
    }

    public static class Builder extends AbstractBuilder<Gui, Gui.Builder.Normal> implements Gui.Builder.Normal {

        @Override
        public @NotNull Gui build() {
            if (structure == null)
                throw new IllegalStateException("Structure is not defined.");

            var gui = new TradeGuiImpl(structure);
            applyModifiers(gui);
            return gui;
        }
    }

}