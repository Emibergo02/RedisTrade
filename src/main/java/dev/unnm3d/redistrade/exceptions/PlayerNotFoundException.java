package dev.unnm3d.redistrade.exceptions;

import lombok.Getter;

@Getter
public class PlayerNotFoundException extends Exception {
    private final String playerName;

    public PlayerNotFoundException(String playerName) {
        super("Player not found: " + playerName);
        this.playerName = playerName;
    }

}
