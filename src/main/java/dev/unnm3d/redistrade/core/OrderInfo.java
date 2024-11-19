package dev.unnm3d.redistrade.core;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import xyz.xenondevs.invui.inventory.VirtualInventory;

import java.nio.ByteBuffer;


@Getter
@ToString
@EqualsAndHashCode
public class OrderInfo {
    @Setter
    private Status status;
    private final VirtualInventory virtualInventory;
    @Setter
    private double proposed;

    public OrderInfo(int orderSize) {
        this(new VirtualInventory(orderSize), Status.REFUTED, 0);
    }

    private OrderInfo(VirtualInventory virtualInventory, Status status, double proposed) {
        this.virtualInventory = virtualInventory;
        this.status = status;
        this.proposed = proposed;
    }


    public byte[] serialize() {
        byte[] serializedInventory = virtualInventory.serialize();
        byte[] finalData = new byte[1 + serializedInventory.length + 8];
        finalData[0] = status.getStatusByte();
        System.arraycopy(serializedInventory, 0, finalData,
                1, serializedInventory.length);
        System.arraycopy(ByteBuffer.allocate(8).putDouble(proposed).array(), 0,
                finalData, 1 + serializedInventory.length, 8);

        return finalData;
    }

    public static OrderInfo deserialize(byte[] data) {

        Status status = Status.fromByte(data[0]);
        byte[] serializedInventory = new byte[data.length - 9];
        System.arraycopy(data, 1, serializedInventory, 0, serializedInventory.length);
        double proposed = ByteBuffer.wrap(data, data.length - 8, 8).getDouble();

        return new OrderInfo(VirtualInventory.deserialize(serializedInventory), status, proposed);
    }

    public enum Status {
        REFUTED((byte) 0),
        CONFIRMED((byte) 1),
        COMPLETED((byte) 2),
        RETRIEVED((byte) 3);

        private final byte status;

        Status(byte status) {
            this.status = status;
        }

        public byte getStatusByte() {
            return status;
        }

        public static Status fromByte(byte status) {
            return switch (status) {
                case 0 -> REFUTED;
                case 1 -> CONFIRMED;
                case 2 -> COMPLETED;
                case 3 -> RETRIEVED;
                default -> throw new IllegalArgumentException("Invalid status byte");
            };
        }
    }
}
