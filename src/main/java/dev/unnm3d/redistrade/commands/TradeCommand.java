package dev.unnm3d.redistrade.commands;

import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.OptArg;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.core.NewTrade;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.Optional;
import java.util.UUID;

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
     */
    public void createTrade(@Sender Player player, @OptArg("target") PlayerListManager.Target targetName) {
        if (targetName == null || targetName.playerName() == null) {//Case A
            plugin.getTradeManager().getLatestTrade(player.getUniqueId()).ifPresentOrElse(trade -> {
                //Open the latest trade (Case A1)
                plugin.getTradeManager().openWindow(trade, player);
            }, () -> {
                //No pending trades (Case A2)
                player.sendRichMessage(Messages.instance().noPendingTrades);
            });
            return;
        }

        plugin.getPlayerListManager().getPlayerUUID(targetName.playerName()).ifPresentOrElse(targetUUID -> {
            if (!plugin.getTradeManager().checkInvalidDistance(player, targetUUID)) {
                Optional<NewTrade> alreadyRunning = plugin.getTradeManager()
                  .getTradeFromParticipants(player.getUniqueId(), targetUUID);
                if (alreadyRunning.isPresent()) {
                    //Open the already running trade (Case E)
                    plugin.getTradeManager().openWindow(alreadyRunning.get(), player);
                    return;
                }

                //Start the trade if player has been invited by target (Case C)

                if (player.getName().equals(plugin.getTradeManager().getInviteManager().getInvitee(targetName.playerName()))) {
                    startAndAccept(player, targetUUID, targetName.playerName());
                    return;
                }
            }

            invitePlayer(player, targetName.playerName());
        }, () -> {
            player.sendRichMessage(Messages.instance().playerNotFound.replace("%player%", targetName.playerName()));
        });
    }

    private void startAndAccept(Player player, UUID targetUUID, @NonNull String targetName) {
        plugin.getTradeManager().startTrade(player, targetUUID, targetName)
          .thenAccept(trade -> trade.ifPresentOrElse(t -> {
              //Open the trade window for the accepting player
              plugin.getTradeManager().getInviteManager().acceptInvitationOf(targetName);
              plugin.getServer().getScheduler().runTask(plugin, () ->
                plugin.getTradeManager().openWindow(t, player));

          }, () -> player.sendRichMessage(Messages.instance().newTradesLock)));
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
