package dev.unnm3d.redistrade.api;

import java.util.UUID;

public interface ITradeSide {

    UUID getTraderUUID();

    String getTraderName();

    IOrderInfo getOrder();

    boolean isOpened();

    void setOpened(boolean opened);

}
