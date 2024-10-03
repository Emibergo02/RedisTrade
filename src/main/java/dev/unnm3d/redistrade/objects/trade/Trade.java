package dev.unnm3d.redistrade.objects.trade;

import dev.unnm3d.redistrade.guis.OrderInfo;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

@Getter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
public class Trade {

    private final UUID uuid;
    protected final OrderInfo traderSideInfo;
    protected final OrderInfo targetSideInfo;


    public Trade() {
        this.uuid = UUID.randomUUID();
        this.traderSideInfo = new OrderInfo(20);
        this.targetSideInfo = new OrderInfo(20);
    }

    public void setTraderPrice(double price) {
        traderSideInfo.setProposed(price);
    }

    public double getTraderPrice() {
        return traderSideInfo.getProposed();
    }

    public void setTargetPrice(double price) {
        targetSideInfo.setProposed(price);
    }

    public double getTargetPrice() {
        return targetSideInfo.getProposed();
    }

    public void setTraderConfirm(boolean confirm) {
        traderSideInfo.setConfirmed(confirm);
    }

    public boolean isTraderConfirmed() {
        return traderSideInfo.isConfirmed();
    }

    public void setTargetConfirm(boolean confirm) {
        targetSideInfo.setConfirmed(confirm);
    }

    public boolean isTargetConfirmed() {
        return targetSideInfo.isConfirmed();
    }

    public ItemStack getTraderItem(int relativeIndex) {
        return traderSideInfo.getItem(relativeIndex);
    }

    public void updateTraderItem(int rawX, int rawY, ItemStack item) {
        int relativeIndex = (rawY - 1) * 4 + rawX; // 4 columns, 5 rows grid
        traderSideInfo.setItem(relativeIndex, item);
    }

    public ItemStack getTargetItem(int relativeIndex) {
        return targetSideInfo.getItem(relativeIndex);
    }

    public void updateTargetItem(int rawX, int rawY, ItemStack item) {
        int relativeIndex = (rawY - 1) * 4 + rawX; // 4 columns, 5 rows grid
        targetSideInfo.setItem(relativeIndex, item);
    }



}
