package dev.unnm3d.redistrade.guis;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.inventory.VirtualInventory;

import java.nio.ByteBuffer;


@ToString
@EqualsAndHashCode
public class OrderInfo {
    @Setter
    @Getter
    private boolean confirmed;
    @Getter
    private final VirtualInventory virtualInventory;
    @Setter
    @Getter
    private double proposed;

    public OrderInfo(int orderSize) {
        this(new VirtualInventory(orderSize), false, 0);
    }

    private OrderInfo(VirtualInventory virtualInventory, boolean confirmed, double proposed) {
        this.virtualInventory = virtualInventory;
        this.confirmed = confirmed;
        this.proposed = proposed;
    }

    public void setItem(int index, ItemStack item) {
        virtualInventory.setItemSilently(index, item);
    }

    public ItemStack getItem(int index) {
        return virtualInventory.getItem(index);
    }

    public byte[] serialize() {
        byte[] serializedInventory = virtualInventory.serialize();
        byte[] finalData = new byte[1 + serializedInventory.length + 8];
        finalData[0] = confirmed ? (byte) 1 : (byte) 0;
        System.arraycopy(serializedInventory, 0, finalData,
                1, serializedInventory.length);
        System.arraycopy(ByteBuffer.allocate(8).putDouble(proposed).array(), 0,
                finalData, 1 + serializedInventory.length, 8);

        return finalData;
    }

    public static OrderInfo deserialize(byte[] data) {

        boolean confirmed = data[0] == 1;
        byte[] serializedInventory = new byte[data.length - 9];
        System.arraycopy(data, 1, serializedInventory, 0, serializedInventory.length);
        double proposed = ByteBuffer.wrap(data, data.length - 8, 8).getDouble();

        return new OrderInfo(VirtualInventory.deserialize(serializedInventory), confirmed, proposed);
    }
}
