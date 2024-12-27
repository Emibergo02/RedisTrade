package dev.unnm3d.redistrade.data;

import dev.unnm3d.redistrade.guis.ViewerType;
import lombok.Getter;

@Getter
public enum TradeUpdateType {
    TRADE_CREATE(ViewerType.SPECTATOR, UpdateType.CREATE),

    TRADER_OPEN(ViewerType.TRADER, UpdateType.OPEN),
    TRADER_PRICE(ViewerType.TRADER, UpdateType.PRICE),
    TRADER_ITEM(ViewerType.TRADER, UpdateType.ITEM),
    TRADER_STATUS(ViewerType.TRADER, UpdateType.STATUS),

    CUSTOMER_OPEN(ViewerType.CUSTOMER, UpdateType.OPEN),
    CUSTOMER_PRICE(ViewerType.CUSTOMER, UpdateType.PRICE),
    CUSTOMER_ITEM(ViewerType.CUSTOMER, UpdateType.ITEM),
    CUSTOMER_STATUS(ViewerType.CUSTOMER, UpdateType.STATUS);

    private final ViewerType viewerType;
    private final UpdateType updateType;

    TradeUpdateType(ViewerType viewerType, UpdateType updateType) {
        this.viewerType = viewerType;
        this.updateType = updateType;
    }

    @Override
    public String toString() {
        return String.valueOf(viewerType == ViewerType.TRADER ?
                Character.toUpperCase(updateType.code) :
                updateType.code);
    }

    public static TradeUpdateType valueOf(char code) {
        return switch (code) {
            case 'C' -> TRADE_CREATE;
            case 'O' -> TRADER_OPEN;
            case 'P' -> TRADER_PRICE;
            case 'I' -> TRADER_ITEM;
            case 'S' -> TRADER_STATUS;
            case 'o' -> CUSTOMER_OPEN;
            case 'p' -> CUSTOMER_PRICE;
            case 'i' -> CUSTOMER_ITEM;
            case 's' -> CUSTOMER_STATUS;
            default -> null;
        };
    }

    public static TradeUpdateType valueOf(ViewerType viewerType, UpdateType updateType) {
        if (updateType == UpdateType.CREATE) return TRADE_CREATE;
        return switch (viewerType) {
            case TRADER -> switch (updateType) {
                case OPEN -> TRADER_OPEN;
                case PRICE -> TRADER_PRICE;
                case ITEM -> TRADER_ITEM;
                case STATUS -> TRADER_STATUS;
                default -> throw new IllegalStateException("Unexpected value: " + updateType);
            };
            case CUSTOMER -> switch (updateType) {
                case OPEN -> CUSTOMER_OPEN;
                case PRICE -> CUSTOMER_PRICE;
                case ITEM -> CUSTOMER_ITEM;
                case STATUS -> CUSTOMER_STATUS;
                default -> throw new IllegalStateException("Unexpected value: " + updateType);
            };
            case SPECTATOR -> null;
        };
    }

    @Getter
    public enum UpdateType {
        CREATE('C'),
        OPEN('o'),
        PRICE('p'),
        ITEM('i'),
        STATUS('s');

        private final char code;

        UpdateType(char code) {
            this.code = code;
        }
    }
}
