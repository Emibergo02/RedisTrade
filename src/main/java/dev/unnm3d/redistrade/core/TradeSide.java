package dev.unnm3d.redistrade.core;

import lombok.*;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
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
    private transient Gui sidePerspective;

    public TradeSide(@NotNull UUID traderUUID, @NotNull String traderName, @NotNull OrderInfo order, boolean opened) {
        this(traderUUID, traderName, order, opened, null);
    }

    public void notifyOppositeStatus() {
        notifyButton(8);
    }

    public void notifyOppositePrice() {
        for (int j = 5; j < 8; j++) {
            notifyButton(j);
        }
    }

    private void notifyButton(int index) {
        Optional.ofNullable(sidePerspective.getItem(index)).ifPresent(Item::notifyWindows);
    }

    public void setPrice(String currency, double price) {
        order.setPrice(currency, price);
        for (int i = 1; i < 4; i++) {
            notifyButton(i);
        }
    }

    public void setStatus(OrderInfo.Status status) {
        order.setStatus(status);
        notifyButton(0);
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
