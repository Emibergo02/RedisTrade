package dev.unnm3d.redistrade.core;

import dev.unnm3d.redistrade.core.enums.Status;
import dev.unnm3d.redistrade.utils.Utils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.inventory.ItemStack;
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
    @Setter
    private Integer rating;
    private final VirtualInventory virtualInventory;
    private final HashMap<String, Double> prices;


    public OrderInfo(int orderSize) {
        this(new VirtualInventory(orderSize), Status.REFUSED, 0, new HashMap<>());
    }

    public OrderInfo(VirtualInventory virtualInventory, Status status, Integer rating, HashMap<String, Double> prices) {
        this.virtualInventory = virtualInventory;
        this.status = status;
        this.rating = rating;
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
        final ByteBuffer buffer = ByteBuffer.allocate(1 + 1 + 2 + (prices.size() * (16 + 8)) + serializedInventory.length);

        buffer.put((byte) status.getStatus());//1 byte

        buffer.put(rating.byteValue());//1 byte

        buffer.putShort((short) prices.size());//2 bytes

        prices.forEach((currency, price) -> {
            //Keep the string size to 16 bytes
            buffer.put(Arrays.copyOf(currency.getBytes(StandardCharsets.ISO_8859_1), 16));//16 bytes
            buffer.putDouble(price);//8 bytes
        });

        buffer.put(serializedInventory);

        return buffer.array();
    }

    public static OrderInfo deserialize(byte[] data) {
        final ByteBuffer buffer = ByteBuffer.wrap(data);
        final Status status = Status.valueOf((char) buffer.get());
        final Integer rating = Byte.toUnsignedInt(buffer.get());
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
        return new OrderInfo(VirtualInventory.deserialize(serializedInventory), status, rating, prices);
    }

    private static byte[] trim(byte[] bytes) {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0) {
            --i;
        }
        return Arrays.copyOf(bytes, i + 1);
    }
}
