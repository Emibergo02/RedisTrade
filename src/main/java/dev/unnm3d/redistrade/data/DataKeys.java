package dev.unnm3d.redistrade.data;

public enum DataKeys {
    UPDATE_TRADE_ITEM("rtrade:up_item"),
    UPDATE_TRADE_MONEY("rtrade:up_money"),
    UPDATE_TRADE_CONFIRM("rtrade:up_confirm"),
    UPDATE_TRADE("rtrade:update"),
    UPDATE_MONEY("rtrade:up_money"),
    UPDATE_CONFIRM("rtrade:up_confirm"),
    UPDATE_ITEM("rtrade:up_item"),
    ORDER_TABLE("rtrade:update"),
    ;


    private final String key;

    DataKeys(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return key;
    }
}
