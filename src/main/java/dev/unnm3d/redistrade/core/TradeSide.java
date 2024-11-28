package dev.unnm3d.redistrade.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@AllArgsConstructor
@Getter
public class TradeSide {
    private final UUID traderUUID;
    private final String traderName;
    private final OrderInfo order;

    public byte[] serialize() {
        byte[] traderData = order.serialize();
        //Allocate bytes for TraderUUID, TraderName, TraderData size, TraderData
        ByteBuffer bb = ByteBuffer.allocate(16 + 16 + 4 + traderData.length);

        bb.putLong(traderUUID.getMostSignificantBits());
        bb.putLong(traderUUID.getLeastSignificantBits());

        byte[] paddedTraderName = new byte[16];
        System.arraycopy(traderName.getBytes(StandardCharsets.ISO_8859_1), 0, paddedTraderName, 0, traderName.length());
        bb.put(paddedTraderName);

        bb.putInt(traderData.length);

        bb.put(traderData);
        return bb.array();
    }
    public static TradeSide deserialize(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        UUID traderUUID = new UUID(bb.getLong(), bb.getLong());

        byte[] traderNameBytes = new byte[16];
        bb.get(traderNameBytes);
        String traderName = new String(traderNameBytes, StandardCharsets.ISO_8859_1).trim();

        int traderSize = bb.getInt();
        byte[] traderData = new byte[traderSize];
        bb.get(traderData);
        return new TradeSide(traderUUID, traderName, OrderInfo.deserialize(traderData));
    }
}
