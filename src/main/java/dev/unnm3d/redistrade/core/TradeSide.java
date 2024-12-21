package dev.unnm3d.redistrade.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

@AllArgsConstructor
@Getter
public class TradeSide {
    private final UUID traderUUID;
    private final String traderName;
    private final OrderInfo order;

    public byte[] serialize() {
        byte[] orderBytes = order.serialize();
        //Allocate bytes for TraderUUID, TraderName, TraderData size, TraderData
        ByteBuffer bb = ByteBuffer.allocate(16 + 16 + 4 + orderBytes.length);
        bb.putLong(traderUUID.getMostSignificantBits());
        bb.putLong(traderUUID.getLeastSignificantBits());

        bb.put(Arrays.copyOf(traderName.getBytes(StandardCharsets.ISO_8859_1), 16));

        bb.putInt(orderBytes.length);

        bb.put(orderBytes);
        return bb.array();
    }

    public static TradeSide deserialize(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        UUID traderUUID = new UUID(bb.getLong(), bb.getLong());

        byte[] traderNameBytes = new byte[16];
        bb.get(traderNameBytes);
        String traderName = new String(traderNameBytes, StandardCharsets.ISO_8859_1).trim();

        int orderSize = bb.getInt();
        byte[] orderBytes = new byte[orderSize];
        bb.get(orderBytes);
        return new TradeSide(traderUUID, traderName, OrderInfo.deserialize(orderBytes));
    }
}
