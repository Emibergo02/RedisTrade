package dev.unnm3d.redistrade.configs;


import de.exlll.configlib.Configuration;
import de.exlll.configlib.YamlConfigurations;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

@Configuration
public class Messages {
    private static Messages SETTINGS;
    public @NotNull String alreadyIgnored;


    public static Messages instance() {
        return SETTINGS;
    }

    public static Messages initSettings(Path configFile) {
        SETTINGS = YamlConfigurations.update(configFile, Messages.class);
        return SETTINGS;
    }

    public String alreadyInTrade = "<red>You already trading with %player% <click:run_command:'/trade'><dark_aqua>[Click to resume or execute /trade]</dark_aqua></click>";
    public String noPendingTrades = "<red>You don't have any pending trades";
    public String targetAlreadyInTrade = "<red>The player %player% is already trading with someone else";
    public String tradeCreated = "<red>You started a trade with %player% <click:run_command:'/trade %player%'><dark_aqua>[Click to open or execute /trade %player%]</dark_aqua></click>";
    public String tradeReceived = "<green>%player% wants to trade with you <click:run_command:'/trade %player%'><dark_aqua>[Click to open or execute /trade %player%]</dark_aqua></click>";
    public String setItemField = "<green>You successfully set the item for the field %field%";
    public String getItemField = "<green>You successfully get the item %field% to your inventory";
    public String completionTimer = "<green>The trade will be finalized in %time% seconds";
    public String playerNotFound = "<red>Player %player% not found";
    public String notEnoughMoney = "<red>You don't have enough money to trade with %amount%";
    public String tradeWithYourself = "<red>You can't trade with yourself";
    public String tradeUnignored = "<green>You unignored %player% trades";
    public String tradeIgnored = "<green>You ignored %player% trades";
    public String tradeIgnoreList = "<green>Ignored players: %list%";
}
