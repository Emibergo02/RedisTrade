package dev.unnm3d.redistrade.data;

public enum DataKeys {

    FULL_TRADE("rtrade:f_trade_up"),
    TRADES("rtrade:trades"),
    TRADE_QUERY("rtrade:query"),
    NAME_UUIDS("rtrade:name_uuids"),
    TRADE_ARCHIVE("rtrade:trades"),
    TRADE_ARCHIVE_PLAYER_PREFIX("rtrade:p_trades_"),
    PLAYER_TRADES("rtrade:p_trades"),
    IGNORE_PLAYER_PREFIX("rtrade:ignore_"),
    IGNORE_PLAYER_UPDATE("rtrade:ignore_up"),
    FIELD_UPDATE_TRADE("rtrade:f_update"),
    PLAYERLIST("rtrade:playerlist"),
    OPEN_WINDOW("rtrade:open_window"),
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
