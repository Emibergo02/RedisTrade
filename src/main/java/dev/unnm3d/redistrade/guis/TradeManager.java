package dev.unnm3d.redistrade.guis;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.objects.trade.RemoteTrade;
import dev.unnm3d.redistrade.objects.trade.Trade;
import dev.unnm3d.redistrade.objects.trade.TradeGUI;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class TradeManager {

    private final RedisTrade plugin;
    private final ConcurrentHashMap<UUID, RemoteTrade> tradeGuis;
    private final ConcurrentHashMap<UUID, UUID> playerTradeMap;

    public TradeManager(RedisTrade plugin) {
        this.plugin = plugin;
        this.tradeGuis = new ConcurrentHashMap<>();
        this.playerTradeMap = new ConcurrentHashMap<>();
    }


    public void startTrade(Player player, Player target) {
        final RemoteTrade trade = new RemoteTrade(plugin.getRedisDataManager());
        tradeGuis.put(trade.getUuid(), trade);
        playerTradeMap.put(player.getUniqueId(), trade.getUuid());
        playerTradeMap.put(target.getUniqueId(), trade.getUuid());
        trade.openTraderWindow(player);
        trade.openTargetWindow(target);
    }

    public Optional<TradeGUI> getTrade(UUID tradeUUID) {
        return Optional.ofNullable(tradeGuis.get(tradeUUID));
    }

    public Optional<TradeGUI> getPlayerTrade(UUID playerUUID) {
        return Optional.ofNullable(playerTradeMap.get(playerUUID))
                .map(tradeGuis::get);
    }

    public void tradeUpdate(Trade trade) {
        editTrade(trade.getUuid(), tradeGUI -> {
            if (tradeGUI == null) {
                return new RemoteTrade(trade, plugin.getRedisDataManager());
            }
            tradeGUI.setTraderPrice(trade.getTraderSideInfo().getProposed());
            tradeGUI.setTargetPrice(trade.getTargetSideInfo().getProposed());
            tradeGUI.setTraderConfirm(trade.getTraderSideInfo().isConfirmed());
            tradeGUI.setTargetConfirm(trade.getTargetSideInfo().isConfirmed());
            return tradeGUI;
        });
    }

    public RemoteTrade editTrade(UUID tradeUUID, Function<RemoteTrade, RemoteTrade> editFunction) {
        return tradeGuis.compute(tradeUUID, (uuid, tradeGUI) -> editFunction.apply(tradeGUI));
    }
}
