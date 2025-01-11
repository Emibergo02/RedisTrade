package dev.unnm3d.redistrade.core.enums;

import lombok.Getter;

@Getter
public enum Status {
    REFUSED('r'),
    CONFIRMED('v'),
    COMPLETED('c'),
    RETRIEVED('x');

    private final char status;

    Status(char status) {
        this.status = status;
    }

    public static Status valueOf(char status) {
        return switch (status) {
            case 'r' -> REFUSED;
            case 'v' -> CONFIRMED;
            case 'c' -> COMPLETED;
            case 'x' -> RETRIEVED;
            default -> null;
        };
    }
}
