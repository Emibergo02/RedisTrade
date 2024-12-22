package dev.unnm3d.redistrade.configs;


import de.exlll.configlib.Configuration;
import de.exlll.configlib.YamlConfigurations;

import java.nio.file.Path;
import java.util.List;

@Configuration
public class Messages {
    private static Messages SETTINGS;

    public static Messages instance() {
        return SETTINGS;
    }

    public static void loadMessages(Path configFile) {
        SETTINGS = YamlConfigurations.update(configFile, Messages.class);
    }


    public String noPermission = "<red>You don't have permission to do that";
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
    public String blacklistedItem = "<red>You can't trade this item, it's blacklisted";
    public String notSupported = "<red>This feature is not supported with %feature%";
    public List<String> moneyButtonLore = List.of("<white>Name: %currency%",
            "Value: <gold>%amount%",
            "Symbol: <green>%symbol%");
    public String confirmMoneyDisplay = "<green>Confirm the price %amount%%symbol%";
    public String tradeRunning = "<red>The trade is still opened in background. <click:run_command:'/trade %player%'><dark_aqua>[Click to resume or execute /trade %player%]</dark_aqua></click>";
    public String newTradesLock = "<red>Sorry for the inconvenience. There are temporary synchronization issues<br> Please try again in a few seconds";
}
