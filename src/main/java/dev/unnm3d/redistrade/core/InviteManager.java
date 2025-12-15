package dev.unnm3d.redistrade.core;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class InviteManager {
    private final Set<TradeInvite> playerInvites;
    public InviteManager() {
        this.playerInvites = buildInviteCache();
    }

    private Set<TradeInvite> buildInviteCache() {
        return Collections.newSetFromMap(CacheBuilder.newBuilder()
          .removalListener((RemovalListener<TradeInvite, Boolean>) notification -> {
              if (notification.getCause() == RemovalCause.EXPIRED) {
                  playerInvites.remove(notification.getKey());
              }
          })
          .expireAfterWrite(Duration.ofMinutes(1))
          .build().asMap());
    }
}
