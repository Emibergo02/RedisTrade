package dev.unnm3d.redistrade.guis;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.objects.NewTrade;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class TradeManager {

    private final RedisTrade plugin;
    private final ConcurrentHashMap<UUID, NewTrade> tradeGuis;
    private final ConcurrentHashMap<String, UUID> playerTradeMap;

    public TradeManager(RedisTrade plugin) {
        this.plugin = plugin;
        this.tradeGuis = new ConcurrentHashMap<>();
        this.playerTradeMap = new ConcurrentHashMap<>();
    }


    public void startTrade(Player traderPlayer, String targetName) {
        //Create Trade and send update
        final NewTrade trade = new NewTrade(plugin.getRedisDataManager());
        tradeGuis.put(trade.getUuid(), trade);
        plugin.getRedisDataManager().updateTrade(trade);

        playerTradeMap.put(traderPlayer.getName(), trade.getUuid());
        playerTradeMap.put(targetName, trade.getUuid());

        trade.openWindow(traderPlayer.getName(),true);
        trade.openRemoteWindow(targetName,false);
    }

    public Optional<NewTrade> getTrade(UUID tradeUUID) {
        return Optional.ofNullable(tradeGuis.get(tradeUUID));
    }

    public Optional<NewTrade> getPlayerTrade(UUID playerUUID) {
        return Optional.ofNullable(playerTradeMap.get(playerUUID))
                .map(tradeGuis::get);
    }

    public void tradeUpdate(NewTrade trade) {
        Optional.ofNullable(tradeGuis.put(trade.getUuid(), trade))
                .ifPresent(oldTrade -> {
                    oldTrade.getTraderGui().closeForAllViewers();
                    oldTrade.getTargetGui().closeForAllViewers();
                });
    }

    public NewTrade editTrade(UUID tradeUUID, Function<NewTrade, NewTrade> editFunction) {
        return tradeGuis.compute(tradeUUID, (uuid, tradeGUI) -> editFunction.apply(tradeGUI));
    }
}
