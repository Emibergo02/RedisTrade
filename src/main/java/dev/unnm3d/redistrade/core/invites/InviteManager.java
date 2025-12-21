package dev.unnm3d.redistrade.core.invites;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.Messages;
import org.bukkit.Bukkit;

import java.time.Duration;
import java.util.Map;

public class InviteManager {
    // A Cache can act as a Set if we just look at the KeySet
    private final Cache<String, String> inviteCache;

    public InviteManager() {
        this.inviteCache = CacheBuilder.newBuilder()
          .expireAfterWrite(Duration.ofMinutes(1))
          .removalListener(removalListener())
          .build();
    }

    private RemovalListener<String, String> removalListener() {
        return removed -> Bukkit.getServer().getOnlinePlayers().stream()
          .filter(p -> p.getName().equals(removed.getKey()))
          .findFirst()
          .ifPresent(player -> {
              assert removed.getValue() != null;
              if (removed.getCause() == RemovalCause.EXPIRED) {
                  player.sendRichMessage(Messages.instance().inviteExpired
                    .replace("%player%", removed.getValue()));
                  return;
              }
              if (removed.getCause() == RemovalCause.EXPLICIT) {
                  player.sendRichMessage(Messages.instance().inviteRefused
                    .replace("%player%", removed.getValue()));
                  return;
              }
              player.sendRichMessage(Messages.instance().inviteCancelled
                .replace("%player%", removed.getValue())
                .replace("%reason%", removed.getCause().name())
              );


          });
    }

    public void invite(String traderName, String customerName) {
        inviteCache.put(traderName, customerName);
        if (RedisTrade.getInstance().getTradeManager().isIgnoring(customerName, traderName)) return;
        RedisTrade.getInstance().getServer().getOnlinePlayers().stream()
          .filter(p -> p.getName().equals(customerName))
          .findFirst()
          .ifPresent(player ->
            player.sendRichMessage(Messages.instance().tradeReceived
              .replace("%player%", traderName)));
    }

    /**
     * Get who the trader has invited
     *
     * @param traderUUID the trader UUID
     * @return the invited customer UUID, or null if none
     */
    public String getInvitee(String traderName) {
        return inviteCache.getIfPresent(traderName);
    }

    public void acceptInvitationOf(String traderName) {
        inviteCache.invalidate(traderName);
    }

    public Map<String, String> getAllActiveInvites() {
        // This view stays in sync with the cache expiration
        return inviteCache.asMap();
    }
}
