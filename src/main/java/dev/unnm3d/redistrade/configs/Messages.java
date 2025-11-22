package dev.unnm3d.redistrade.configs;


import de.exlll.configlib.Configuration;
import de.exlll.configlib.YamlConfigurations;
import dev.unnm3d.redistrade.restriction.KnownRestriction;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Configuration
public class Messages {
    private static Messages SETTINGS;


    public static Messages instance() {
        return SETTINGS;
    }

    public static void loadMessages(Path configFile) {
        SETTINGS = YamlConfigurations.update(configFile, Messages.class);
    }

    public String cmdCooldown = "<red>Wait some time before using this command again";
    public String noPermission = "<red>You don't have permission to do that";
    public String alreadyInTrade = "<red>You already trading with %player% <click:run_command:'/trade'><dark_aqua>[Click to resume or execute /trade]</dark_aqua></click>";
    public String noPendingTrades = "<red>You don't have any pending trades";
    public String noPendingTradesOther = "<red>%player% doesn't have any pending trades";
    public String tradeNotFound = "<red>Trade not found";
    public String targetAlreadyInTrade = "<red>The player %player% is already trading with someone else";
    public String tradeCreated = "<gradient:green:gray>You started a trade with %player%<br><dark_aqua><click:run_command:'/trade %player%'>[Click to open]</click> <white>or </white>execute /trade %player%</dark_aqua>";
    public String tradeReceived = "<green>%player% wants to trade with you <click:run_command:'/trade %player%'><dark_aqua>[Click to open or execute /trade %player%]</dark_aqua></click>";
    public String setItemField = "<green>You successfully set the item as %field%";
    public String getItemField = "<green>You successfully get the item %field% to your inventory";
    public String completionTimer = "<green>The trade will be finalized in %time% seconds";
    public String playerNotFound = "<red>Player %player% not found";
    public String tradeDistance = "<red>You must be at least %blocks% distant from the other player to trade";
    public String notEnoughMoney = "<red>You don't have enough of this currency";
    public String invalidFormat = "<red>Invalid format";
    public String tradeWithYourself = "<red>You can't trade with yourself";
    public String tradeUnignored = "<green>You unignored %player% trades";
    public String tradeIgnored = "<green>You ignored %player% trades";
    public String tradeIgnoreList = "<green>Ignored players: %list%";
    public String blacklistedItem = "<red>You can't trade this item, it's blacklisted";
    public String notSupported = "<red>This feature is not supported with %feature%";
    public List<String> moneyButtonLore = List.of("<white>Currency: %currency%",
            "Value: <gold>%amount%%symbol%");
    public String confirmMoneyDisplay = "<green>Confirm the price %amount%%symbol%";
    public String tradeRunning = "<color:#39abab>The trade is still opened in background</color><br><color:#bfbfbf><click:run_command:'/trade %player%'>[Click to resume]</click> or <click:suggest_command:'/trade'>[/trade]</click>";
    public String newTradesLock = "<red>Sorry for the inconvenience. There are temporary synchronization issues<br> Please try again in a few seconds";
    public String tradeRated = "<green>You reviewed the trade with %rate% stars";
    public String playerShowRating = "<green>%player%'s rating is %mean% or <yellow>%stars%</yellow> on %count% trades";
    public String tradeShowNoRating = "<green>%trader_name% didn't review the trade yet";
    public String tradeShowRating = "<green>%reviewer% review of %reviewed%: <yellow>%stars%</yellow>";
    public String tradeWindowClosed = "<aqua>Your trade has been put it background<br><color:#bfbfbf><click:run_command:'/trade %player%'>[Click to resume]</click> or <click:suggest_command:'/trade'>[/trade]</click><br><red>You did something you're not allowed to do when trading";
    public String tradeRestricted = "<red>You can't open the trade window because you did something you're not allowed to do: moving, getting damaged, being in combat";
    public Map<String, String> restrictionMessages = Map.of(
            KnownRestriction.MOVED.toString(), "<red>You can't move while trading",
            KnownRestriction.DAMAGED.toString(), "<red>You can't get damaged while trading",
            KnownRestriction.COMBAT.toString(), "<red>You can't be in combat while trading",
            KnownRestriction.WORLD_CHANGE.toString(), "<red>You can't change world while trading",
            "WORLD_GUARD", "<red>You can't trade in this region"
    );
}
