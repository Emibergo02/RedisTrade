package dev.unnm3d.redistrade.core;

import dev.unnm3d.redistrade.core.enums.Status;
import dev.unnm3d.redistrade.guis.MutableGui;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class TradeSide {

    private final UUID traderUUID;
    private final String traderName;
    private final OrderInfo order;
    @Setter
    private boolean opened;
    @Setter
    private transient MutableGui sidePerspective;

    public TradeSide(@NotNull UUID traderUUID, @NotNull String traderName, @NotNull OrderInfo order, boolean opened) {
        this(traderUUID, traderName, order, opened, null);
    }

    public void notifyOppositeStatus() {
        sidePerspective.notifyItem('c');
    }

    public void setStatus(Status status) {
        order.setStatus(status);
        sidePerspective.notifyItem('C');
        sidePerspective.notifyItem('D');
    }

    public void notifyOppositePrice() {
        sidePerspective.notifyItem('m');
        sidePerspective.notifyItem('n');
        sidePerspective.notifyItem('o');
    }

    public void setPrice(String currency, double price) {
        order.setPrice(currency, price);
        sidePerspective.notifyItem('M');
        sidePerspective.notifyItem('N');
        sidePerspective.notifyItem('O');
    }

    public byte[] serialize() {
        byte[] orderBytes = order.serialize();
        //Allocate bytes for TraderUUID, TraderName, IsOpened, TraderData size, TraderData
        ByteBuffer bb = ByteBuffer.allocate(16 + 16 + 1 + 4 + orderBytes.length);
        bb.putLong(traderUUID.getMostSignificantBits());
        bb.putLong(traderUUID.getLeastSignificantBits());

        bb.put(Arrays.copyOf(traderName.getBytes(StandardCharsets.ISO_8859_1), 16));
        bb.put((byte) (opened ? 1 : 0));
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
        boolean isOpened = bb.get() == 1;
        int orderSize = bb.getInt();
        byte[] orderBytes = new byte[orderSize];
        bb.get(orderBytes);
        return new TradeSide(traderUUID, traderName, OrderInfo.deserialize(orderBytes), isOpened);
    }
}
