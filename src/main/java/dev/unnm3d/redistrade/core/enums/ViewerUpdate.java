package dev.unnm3d.redistrade.core.enums;

import lombok.Getter;

@Getter
public enum ViewerUpdate {
    TRADE_CREATE('0', Actor.SPECTATOR, UpdateType.CREATE),

    TRADER_OPEN('1', Actor.TRADER, UpdateType.OPEN),
    TRADER_CLOSE('2', Actor.TRADER, UpdateType.CLOSE),
    TRADER_PRICE('3', Actor.TRADER, UpdateType.PRICE),
    TRADER_ITEM('4', Actor.TRADER, UpdateType.ITEM),
    TRADER_STATUS('5', Actor.TRADER, UpdateType.STATUS),

    CUSTOMER_OPEN('6', Actor.CUSTOMER, UpdateType.OPEN),
    CUSTOMER_CLOSE('7', Actor.CUSTOMER, UpdateType.CLOSE),
    CUSTOMER_PRICE('8', Actor.CUSTOMER, UpdateType.PRICE),
    CUSTOMER_ITEM('9', Actor.CUSTOMER, UpdateType.ITEM),
    CUSTOMER_STATUS('a', Actor.CUSTOMER, UpdateType.STATUS),

    SPECTATOR_OPEN('b', Actor.SPECTATOR, UpdateType.OPEN),
    SPECTATOR_CLOSE('c', Actor.SPECTATOR, UpdateType.CLOSE);

    private final char id;
    private final Actor actorSide;
    private final UpdateType updateType;

    ViewerUpdate(char id, Actor actorSide, UpdateType updateType) {
        this.id = id;
        this.actorSide = actorSide;
        this.updateType = updateType;
    }

    @Override
    public String toString() {
        return String.valueOf(this.id);
    }

    public static ViewerUpdate valueOf(char id) {
        return switch (id) {
            case '0' -> TRADE_CREATE;
            case '1' -> TRADER_OPEN;
            case '2' -> TRADER_CLOSE;
            case '3' -> TRADER_PRICE;
            case '4' -> TRADER_ITEM;
            case '5' -> TRADER_STATUS;
            case '6' -> CUSTOMER_OPEN;
            case '7' -> CUSTOMER_CLOSE;
            case '8' -> CUSTOMER_PRICE;
            case '9' -> CUSTOMER_ITEM;
            case 'a' -> CUSTOMER_STATUS;
            case 'b' -> SPECTATOR_OPEN;
            case 'c' -> SPECTATOR_CLOSE;
            default -> throw new IllegalArgumentException("Invalid trade update type char");
        };
    }

    public static ViewerUpdate valueOf(Actor actor, UpdateType updateType) {
        return switch (actor) {
            case TRADER -> switch (updateType) {
                case CREATE -> TRADE_CREATE;
                case OPEN -> TRADER_OPEN;
                case CLOSE -> TRADER_CLOSE;
                case PRICE -> TRADER_PRICE;
                case ITEM -> TRADER_ITEM;
                case STATUS -> TRADER_STATUS;
            };
            case CUSTOMER -> switch (updateType) {
                case CREATE -> TRADE_CREATE;
                case OPEN -> CUSTOMER_OPEN;
                case CLOSE -> CUSTOMER_CLOSE;
                case PRICE -> CUSTOMER_PRICE;
                case ITEM -> CUSTOMER_ITEM;
                case STATUS -> CUSTOMER_STATUS;
            };
            case SPECTATOR -> switch (updateType) {
                case CREATE -> TRADE_CREATE;
                case OPEN -> SPECTATOR_OPEN;
                case CLOSE -> SPECTATOR_CLOSE;
                default -> throw new IllegalStateException("Unexpected value: " + updateType);
            };
        };
    }
}