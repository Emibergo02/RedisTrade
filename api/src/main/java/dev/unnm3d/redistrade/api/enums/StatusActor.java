package dev.unnm3d.redistrade.api.enums;

import lombok.Getter;

@Getter
public enum StatusActor {
    TRADER_REFUSED(Actor.TRADER, Status.REFUSED),
    TRADER_CONFIRMED(Actor.TRADER, Status.CONFIRMED),
    TRADER_COMPLETED(Actor.TRADER, Status.COMPLETED),
    TRADER_RETRIEVED(Actor.TRADER, Status.RETRIEVED),

    CUSTOMER_REFUSED(Actor.CUSTOMER, Status.REFUSED),
    CUSTOMER_CONFIRMED(Actor.CUSTOMER, Status.CONFIRMED),
    CUSTOMER_COMPLETED(Actor.CUSTOMER, Status.COMPLETED),
    CUSTOMER_RETRIEVED(Actor.CUSTOMER, Status.RETRIEVED);

    private final Actor viewerActor;
    private final Status status;

    StatusActor(Actor viewerActor, Status status) {
        this.viewerActor = viewerActor;
        this.status = status;
    }

    public char toChar() {
        return switch (this) {
            case TRADER_REFUSED -> 'R';
            case TRADER_CONFIRMED -> 'V';
            case TRADER_COMPLETED -> 'C';
            case TRADER_RETRIEVED -> 'X';
            case CUSTOMER_REFUSED -> 'r';
            case CUSTOMER_CONFIRMED -> 'v';
            case CUSTOMER_COMPLETED -> 'c';
            case CUSTOMER_RETRIEVED -> 'x';
        };
    }

    public static StatusActor fromChar(char status) {
        return switch (status) {
            case 'R' -> TRADER_REFUSED;
            case 'V' -> TRADER_CONFIRMED;
            case 'C' -> TRADER_COMPLETED;
            case 'X' -> TRADER_RETRIEVED;
            case 'r' -> CUSTOMER_REFUSED;
            case 'v' -> CUSTOMER_CONFIRMED;
            case 'c' -> CUSTOMER_COMPLETED;
            case 'x' -> CUSTOMER_RETRIEVED;
            default -> throw new IllegalArgumentException("Invalid status byte");
        };
    }

    public static StatusActor valueOf(Actor actor, Status status) {
        return switch (actor) {
            case TRADER -> switch (status) {
                case REFUSED -> TRADER_REFUSED;
                case CONFIRMED -> TRADER_CONFIRMED;
                case COMPLETED -> TRADER_COMPLETED;
                case RETRIEVED -> TRADER_RETRIEVED;
            };
            case CUSTOMER -> switch (status) {
                case REFUSED -> CUSTOMER_REFUSED;
                case CONFIRMED -> CUSTOMER_CONFIRMED;
                case COMPLETED -> CUSTOMER_COMPLETED;
                case RETRIEVED -> CUSTOMER_RETRIEVED;
            };
            default -> throw new IllegalArgumentException("Invalid actor type");
        };
    }
}
