package dev.unnm3d.redistrade.api.enums;

import lombok.Getter;

@Getter
public enum Status {
    REFUSED('R'),
    CONFIRMED('V'),
    COMPLETED('c'),
    RETRIEVED('x');

    private final char status;

    Status(char status) {
        this.status = status;
    }

    public static Status valueOf(char status) {
        return switch (status) {
            case 'R' -> REFUSED;
            case 'V' -> CONFIRMED;
            case 'c' -> COMPLETED;
            case 'x' -> RETRIEVED;
            default -> null;
        };
    }
}
