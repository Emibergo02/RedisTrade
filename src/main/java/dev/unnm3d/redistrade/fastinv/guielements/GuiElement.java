package dev.unnm3d.redistrade.fastinv.guielements;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

@AllArgsConstructor
@Getter
public class GuiElement{
    protected ItemStack item;
    protected Consumer<InventoryClickEvent> clickHandler;
}
