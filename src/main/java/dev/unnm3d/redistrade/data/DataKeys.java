package dev.unnm3d.redistrade.data;

public enum DataKeys {

    UPDATE_TRADE("rtrade:update"),
    TRADES("rtrade:trades"),
    NAME_UUIDS("rtrade:name_uuids"),
    TRADE_ARCHIVE("rtrade:trade_archive"),
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
