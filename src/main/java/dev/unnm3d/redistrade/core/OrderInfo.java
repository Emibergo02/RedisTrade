package dev.unnm3d.redistrade.core;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.inventory.VirtualInventory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;


@Getter
@ToString
@EqualsAndHashCode
public class OrderInfo {
    @Setter
    private Status status;
    private final VirtualInventory virtualInventory;
    private final HashMap<String, Double> prices;

    public OrderInfo(int orderSize) {
        this(new VirtualInventory(orderSize), Status.REFUSED, new HashMap<>());
    }

    private OrderInfo(VirtualInventory virtualInventory, Status status, HashMap<String, Double> prices) {
        this.virtualInventory = virtualInventory;
        this.status = status;
        this.prices = prices;
    }

    public void setPrice(String currency, double price) {
        prices.put(currency.trim(), price);
    }

    public double getPrice(@NotNull String currency) {
        return prices.getOrDefault(currency, 0.0d);
    }

    public byte[] serialize() {
        byte[] serializedInventory = virtualInventory.serialize();
        // 1 byte for status, 2 bytes for proposed size
        // 16 bytes for each proposed currency name, 8 bytes for each proposed double
        final ByteBuffer buffer = ByteBuffer.allocate(1 + 2 + (prices.size() * (16 + 8)) + serializedInventory.length);

        buffer.put(status.getStatusByte());

        buffer.putShort((short) prices.size());

        prices.forEach((currency, price) -> {
            //Keep the string size to 16 bytes
            buffer.put(Arrays.copyOf(currency.getBytes(StandardCharsets.ISO_8859_1), 16));
            buffer.putDouble(price);
        });

        buffer.put(serializedInventory);

        return buffer.array();
    }

    public static OrderInfo deserialize(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        Status status = Status.fromByte(buffer.get());
        short pricesSize = buffer.getShort();
        final HashMap<String, Double> prices = new HashMap<>();
        for (int i = 0; i < pricesSize; i++) {
            byte[] currencyName = new byte[16];
            buffer.get(currencyName);
            //Remove trailing 0s
            prices.put(new String(trim(currencyName), StandardCharsets.ISO_8859_1),
                    buffer.getDouble());
        }
        byte[] serializedInventory = new byte[buffer.remaining()];
        buffer.get(serializedInventory);
        return new OrderInfo(VirtualInventory.deserialize(serializedInventory), status, prices);
    }

    private static byte[] trim(byte[] bytes) {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0) {
            --i;
        }
        return Arrays.copyOf(bytes, i + 1);
    }

    public enum Status {
        REFUSED((byte) 0),
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
                case 0 -> REFUSED;
                case 1 -> CONFIRMED;
                case 2 -> COMPLETED;
                case 3 -> RETRIEVED;
                default -> throw new IllegalArgumentException("Invalid status byte");
            };
        }
    }
}
