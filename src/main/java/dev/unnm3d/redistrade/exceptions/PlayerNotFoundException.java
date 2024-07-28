package dev.unnm3d.redistrade.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class PlayerNotFoundException extends Exception{
    private String playerName;

    public PlayerNotFoundException(String playerName){
        super("Player not found: " + playerName);
        this.playerName = playerName;
    }

}
