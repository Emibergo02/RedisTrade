package dev.unnm3d.redistrade.core.enums;

import lombok.Getter;

@Getter
public enum Actor {
    TRADER, CUSTOMER, SPECTATOR, ADMIN;

    public Actor opposite() {
        return switch (this) {
            case TRADER -> CUSTOMER;
            case CUSTOMER -> TRADER;
            case SPECTATOR -> SPECTATOR;
            case ADMIN -> ADMIN;
        };
    }

    /**
     * Check another actor is on this actor intended as side of the trade
     *
     * @param otherActor the other actor to check
     * @return true if the other actor is on this actor side or is ADMIN
     */
    public boolean isSideOf(Actor otherActor) {
        return this == otherActor || otherActor == ADMIN;
    }

    public boolean isParticipant() {
        return this == TRADER || this == CUSTOMER;
    }
}
