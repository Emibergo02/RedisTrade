package dev.unnm3d.redistrade.core;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public record TradeInvite(UUID traderId, UUID customerId) {

    public static TradeInvite of(UUID fromPlayer, UUID toPlayer) {
        return new TradeInvite(fromPlayer, toPlayer);
    }

    @Contract(" -> new")
    public @NonNull String serialize() {
        //16 bytes for UUID + 16 bytes for UUID
        final ByteBuffer bb = ByteBuffer.allocate(16 + 16);
        bb.putLong(traderId.getMostSignificantBits());
        bb.putLong(traderId.getLeastSignificantBits());
        bb.putLong(customerId.getMostSignificantBits());
        bb.putLong(customerId.getLeastSignificantBits());
        return new String(bb.array(), StandardCharsets.ISO_8859_1);
    }

    public static @NonNull TradeInvite deserialize(@NonNull String data) {
        ByteBuffer bb = ByteBuffer.wrap(data.getBytes(StandardCharsets.ISO_8859_1));
        UUID traderId = new UUID(bb.getLong(), bb.getLong());
        UUID customerId = new UUID(bb.getLong(), bb.getLong());
        return new TradeInvite(traderId, customerId);
    }
}
