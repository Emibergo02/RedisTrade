package dev.unnm3d.redistrade.commands;

import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.OptArg;
import com.jonahseguin.drink.annotation.Require;
import com.jonahseguin.drink.annotation.Sender;
import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.TradeSide;
import dev.unnm3d.redistrade.core.enums.Actor;
import dev.unnm3d.redistrade.data.IStorageData;
import dev.unnm3d.redistrade.guis.ReviewGUI;
import dev.unnm3d.redistrade.utils.Utils;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public class RateCommand {

    private final RedisTrade plugin;
    private final ConcurrentHashMap<UUID, Long> ratingCooldown = new ConcurrentHashMap<>();

    @Command(name = "set", desc = "Rate a trade", usage = "<tradeUUID> [rating]")
    @Require("redistrade.rate")
    public void setRating(@Sender Player player, UUID tradeUUID, @OptArg("0") int rating) {
        if (cooldown(player.getUniqueId())) {
            player.sendRichMessage(Messages.instance().cmdCooldown);
            return;
        }
        if (tradeUUID == null) {
            player.sendRichMessage(Messages.instance().tradeNotFound);
            return;
        }

        plugin.getDataStorage().getArchivedTrade(tradeUUID)
          .thenAccept(trade -> trade.ifPresentOrElse(t -> {
              Actor oppositeActor = t.getTrade().getActor(player).opposite();
              //Check if is the right actor
              if (!oppositeActor.isParticipant()) {
                  player.sendRichMessage(Messages.instance().noPermission);
                  return;
              }
              //Check expiration
              if (t.getArchivedAt().toInstant().isBefore(
                Instant.now().minusSeconds(Settings.instance().tradeReviewTime))) {
                  player.sendRichMessage(Messages.instance().tradeExpired);
                  return;
              }
              //Open GUI if rating is 0
              if (rating == 0) {
                  ReviewGUI.open(t.getTrade().getUuid(), player);
                  return;
              }
              RedisTrade.getInstance().getDataStorage().rateTrade(t.getTrade(), oppositeActor, rating);
              player.sendRichMessage(Messages.instance().tradeRated
                .replace("%rate%", String.valueOf(rating))
                .replace("%stars%", Utils.starsOf(rating))
              );
          }, () -> player.sendRichMessage(Messages.instance().tradeNotFound)));
    }

    @Command(name = "show-trade", desc = "Show trade ratings", usage = "<tradeUUID>")
    @Require("redistrade.rate.showtrade")
    public void showTradeRating(@Sender Player player, UUID tradeUUID) {
        if (cooldown(player.getUniqueId())) {
            player.sendRichMessage(Messages.instance().cmdCooldown);
            return;
        }

        plugin.getDataStorage().getArchivedTrade(tradeUUID)
          .thenAccept(optTrade -> optTrade.ifPresentOrElse(archivedTrade -> {
              sendTradeRating(player, archivedTrade.getTrade().getTraderSide(), archivedTrade.getTrade().getCustomerSide().getTraderName());
              sendTradeRating(player, archivedTrade.getTrade().getCustomerSide(), archivedTrade.getTrade().getTraderSide().getTraderName());
          }, () -> player.sendRichMessage(Messages.instance().tradeNotFound)));
    }

    @Command(name = "show-player", desc = "Show player rating stats", usage = "[playerNameOrUUID]")
    @Require("redistrade.rate.showplayer")
    public void showPlayerRating(@Sender Player player, @OptArg String playerNameUUID) {
        if (cooldown(player.getUniqueId())) {
            player.sendRichMessage(Messages.instance().cmdCooldown);
            return;
        }

        if (playerNameUUID == null) {
            plugin.getDataStorage().getMeanRating(player.getUniqueId())
              .thenAccept(meanRating -> sendMeanRating(player, meanRating));
            return;
        }
        if (playerNameUUID.length() > 16) {//If it's a UUID
            plugin.getDataStorage().getMeanRating(UUID.fromString(playerNameUUID))
              .thenAccept(meanRating -> sendMeanRating(player, meanRating));
            return;
        }
        plugin.getPlayerListManager().getPlayerUUID(playerNameUUID)
          .ifPresent(uuid -> plugin.getDataStorage().getMeanRating(uuid)
            .thenAccept(meanRating -> sendMeanRating(player, meanRating)));
    }

    private void sendMeanRating(Player player, IStorageData.MeanRating meanRating) {
        if (meanRating.playerName() == null) {
            player.sendRichMessage(Messages.instance().playerNotFound);
            return;
        }
        player.sendRichMessage(Messages.instance().playerShowRating
          .replace("%player%", meanRating.playerName())
          .replace("%mean%", String.valueOf(meanRating.mean()))
          .replace("%count%", String.valueOf(meanRating.countedTrades()))
          .replace("%stars%", Utils.starsOf(meanRating.mean()))
        );
    }

    private void sendTradeRating(Player player, TradeSide tradeSide, String otherTraderName) {
        int rating = tradeSide.getOrder().getRating();
        if (rating != 0) {
            player.sendRichMessage(Messages.instance().tradeShowRating
              .replace("%reviewer%", otherTraderName)
              .replace("%reviewed%", tradeSide.getTraderName())
              .replace("%rate%", String.valueOf(rating))
              .replace("%stars%", Utils.starsOf(rating))
            );
        } else {
            player.sendRichMessage(Messages.instance().tradeShowNoRating
              .replace("%trader_name%", tradeSide.getTraderName())
            );
        }
    }

    private boolean cooldown(UUID playerUUID) {
        if (ratingCooldown.containsKey(playerUUID)) {
            if (ratingCooldown.get(playerUUID) > System.currentTimeMillis()) {
                return true;
            }
            ratingCooldown.values().removeIf(aLong -> aLong < System.currentTimeMillis());
        }
        ratingCooldown.put(playerUUID, System.currentTimeMillis() + Settings.instance().commandCooldown);
        return false;
    }
}
