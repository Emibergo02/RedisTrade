package dev.unnm3d.redistrade.commands;

import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.OptArg;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.NewTrade;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

@RequiredArgsConstructor
public class TradeCommand {

    private final RedisTrade plugin;

    @Command(name = "", desc = "Open the emporium")
    @Require("redistrade.trade")
    /**
     * /trade resume the trade. If no trade is found, send message (Case A)
     * /trade <player> sends invite to player, if player has already invited target, send message (Case B)
     *
     * If target has already invited sender, accept the invite and open the trade window  (Case C)
     *
     * If target is already in a trade with sender, open the trade window (Case E)
     * If the target is already in another trade, send message todo
     * If sender is already in a trade, send message with possibility to open the trade window todo
     *
     * OPTIONAL
     * When target opens the trade, automatically open the window for sender too
     */
    public void tradeCommand(@Sender Player player, @OptArg("target") PlayerListManager.Target targetName) {
        if (targetName == null || targetName.playerName() == null) {//Case A
            plugin.getTradeManager().openActiveTrades(player);
            return;
        }
        if (targetName.playerName().equals(player.getName())) {
            player.sendRichMessage(Messages.instance().tradeWithYourself);
            return;
        }

        plugin.getPlayerListManager().getPlayerUUID(targetName.playerName()).ifPresentOrElse(targetUUID -> {
            if (plugin.getTradeManager().checkInvalidDistance(player, targetUUID)) return;

            //Open the already running trade (Case E)
            for (NewTrade activeTrade : plugin.getTradeManager().getPlayerActiveTrades(player.getUniqueId())) {
                if(activeTrade.isParticipant(targetUUID)){
                    plugin.getTradeManager().openWindow(activeTrade, player,false);
                    return;
                }
            }

            //Start the trade if player has been invited by target (Case C)
            if (Settings.instance().skipInviteRequirements || player.getName().equals(plugin.getTradeManager().getInviteManager().getInvitee(targetName.playerName()))) {
                plugin.getTradeManager().startAcceptInviteAndOpen(player, targetUUID, targetName.playerName(),false);
                return;
            }
            invitePlayer(player, targetName.playerName());

        }, () ->
          player.sendRichMessage(Messages.instance().playerNotFound.replace("%player%", targetName.playerName())));
    }

    private void invitePlayer(@NonNull Player inviter, String targetName) {
        //If player has already invited target, send message (Case B)
        if (targetName.equals(plugin.getTradeManager().getInviteManager().getInvitee(inviter.getName()))) {
            inviter.sendRichMessage(Messages.instance().inviteAlreadySent.replace("%player%", targetName));
            return;
        }
        //Case B
        plugin.getTradeManager().getInviteManager().invite(inviter.getName(), targetName);
        plugin.getDataCache().sendInvite(inviter.getName(), targetName);
        inviter.sendRichMessage(Messages.instance().inviteNew.replace("%player%", targetName));
    }

}
