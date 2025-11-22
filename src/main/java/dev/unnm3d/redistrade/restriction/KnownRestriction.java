package dev.unnm3d.redistrade.restriction;

public enum KnownRestriction {
    MOVED("PLAYER_MOVED"),
    DAMAGED("PLAYER_DAMAGED"),
    COMBAT("PLAYER_COMBAT"),
    WORLD_CHANGE("WORLD_CHANGE");
    private final String name;

    KnownRestriction(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}