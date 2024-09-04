package dev.unnm3d.redistrade.fastinv.guielements;

public class StorageElement extends GuiElement {
    public StorageElement() {
        super(null, null);
        this.clickHandler = e -> {
            if (!e.getCursor().isEmpty() && e.getCurrentItem() == null) {
                e.setCancelled(false);
                item = e.getCursor();
            }
        };
    }
}
