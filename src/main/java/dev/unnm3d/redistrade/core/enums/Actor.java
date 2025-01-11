package dev.unnm3d.redistrade.core.enums;

import lombok.Getter;

@Getter
public enum Actor {
    TRADER, CUSTOMER, SPECTATOR;

    public Actor opposite() {
        return switch (this) {
            case TRADER -> CUSTOMER;
            case CUSTOMER -> TRADER;
            case SPECTATOR -> SPECTATOR;
        };
    }
}
