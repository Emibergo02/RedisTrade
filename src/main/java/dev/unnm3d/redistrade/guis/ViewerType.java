package dev.unnm3d.redistrade.guis;

import lombok.Getter;

@Getter
public enum ViewerType {
    TRADER('T'), CUSTOMER('C'), SPECTATOR('S');

    private final char id;

    ViewerType(char id) {
        this.id = id;
    }

    public ViewerType opposite() {
        return switch (this) {
            case TRADER -> CUSTOMER;
            case CUSTOMER -> TRADER;
            case SPECTATOR -> SPECTATOR;
        };
    }

    public static ViewerType fromChar(char id) {
        for (ViewerType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return null;
    }

}
