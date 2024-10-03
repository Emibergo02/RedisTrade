package dev.unnm3d.redistrade.objects.trade;

import dev.unnm3d.redistrade.guis.maingui.TargetGui;
import dev.unnm3d.redistrade.guis.maingui.TraderGui;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Getter
public class TradeGUI extends Trade {
    private final TraderGui traderGui;
    private final TargetGui targetGui;

    public TradeGUI() {
        super();
        this.traderGui = new TraderGui(this);
        this.targetGui = new TargetGui(this);
    }

    public TradeGUI(Trade trade) {
        super(trade.getUuid(), trade.getTraderSideInfo(), trade.getTargetSideInfo());
        this.traderGui = new TraderGui(this);
        this.targetGui = new TargetGui(this);
    }

    @Override
    public void setTraderPrice(double price) {
        super.setTraderPrice(price);
        targetGui.drawOrNotifyMoney();
        traderGui.drawOrNotifyMoney();
    }

    @Override
    public void setTargetPrice(double price) {
        super.setTargetPrice(price);
        targetGui.drawOrNotifyMoney();
        traderGui.drawOrNotifyMoney();
    }

    @Override
    public void setTraderConfirm(boolean confirm) {
        super.setTraderConfirm(confirm);
        traderGui.drawOrNotifyConfirm();
        targetGui.drawOrNotifyConfirm();
    }

    @Override
    public void setTargetConfirm(boolean confirm) {
        super.setTargetConfirm(confirm);
        targetGui.drawOrNotifyConfirm();
        traderGui.drawOrNotifyConfirm();
    }

    @Override
    public void updateTraderItem(int rawX, int rawY, ItemStack item) {
        int relativeIndex = (rawY - 1) * 4 + rawX; // 4 columns, 5 rows grid
        traderSideInfo.setItem(relativeIndex, item);
        traderGui.notifyViewerItem(rawX, rawY);
        targetGui.notifyViewerItem(Math.abs(rawX - 8), rawY);
    }

    @Override
    public void updateTargetItem(int x, int y, ItemStack item) {
        int relativeIndex = (y - 1) * 4 + x; // 4 columns, 5 rows grid
        targetSideInfo.setItem(relativeIndex, item);
        targetGui.notifyViewerItem(x, y);
        traderGui.notifyViewerItem(Math.abs(x - 8), y);
    }



    public void openTraderWindow(Player trader) {
        traderGui.openWindow(trader);
    }

    public void openTargetWindow(Player target) {
        targetGui.openWindow(target);
    }
}
