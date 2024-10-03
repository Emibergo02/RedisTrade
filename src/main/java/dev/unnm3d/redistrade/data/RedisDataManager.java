package dev.unnm3d.redistrade.data;

import com.google.gson.Gson;
import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.Utils;
import dev.unnm3d.redistrade.objects.trade.Trade;
import dev.unnm3d.redistrade.redistools.RedisAbstract;
import io.lettuce.core.RedisClient;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class RedisDataManager extends RedisAbstract {
    private static final Gson gson = new Gson();
    private final RedisTrade plugin;

    public RedisDataManager(RedisTrade plugin,RedisClient client, int poolSize) {
        super(client, poolSize);
        this.plugin = plugin;
        registerSub(DataKeys.UPDATE_TRADE.toString());
    }

    @Override
    public void receiveMessage(String channel, String message) {
        if (channel.equals(DataKeys.UPDATE_TRADE.toString())) {
            Trade trade = gson.fromJson(message, Trade.class);
            System.out.println(trade);
            plugin.getTradeManager().tradeUpdate(trade);

        } else if (channel.equals(DataKeys.UPDATE_CONFIRM.toString())) {
            String[] split = message.split(":");
            UUID tradeUUID = UUID.fromString(split[0]);
            boolean isTrader = Boolean.parseBoolean(split[1]);
            boolean confirm = Boolean.parseBoolean(split[2]);
            plugin.getTradeManager().getTrade(tradeUUID).ifPresent(trade -> {
                if (isTrader) {
                    trade.setTraderConfirm(confirm);
                } else {
                    trade.setTargetConfirm(confirm);
                }
            });

        } else if (channel.equals(DataKeys.UPDATE_MONEY.toString())) {
            String[] split = message.split(":");
            UUID tradeUUID = UUID.fromString(split[0]);
            boolean isTrader = Boolean.parseBoolean(split[1]);
            double money = Double.parseDouble(split[2]);
            plugin.getTradeManager().getTrade(tradeUUID).ifPresent(trade -> {
                if (isTrader) {
                    trade.setTraderPrice(money);
                } else {
                    trade.setTargetPrice(money);
                }
            });
        } else if (channel.equals(DataKeys.UPDATE_ITEM.toString())) {
            String[] split = message.split("§;");
            UUID tradeUUID = UUID.fromString(split[0]);
            boolean isTrader = Boolean.parseBoolean(split[1]);
            int rawX = Integer.parseInt(split[2]);
            int rawY = Integer.parseInt(split[3]);
            plugin.getTradeManager().getTrade(tradeUUID).ifPresent(trade -> {
                final ItemStack item = Utils.deserialize(split[4])[0];
                if (isTrader) {
                    trade.updateTraderItem(rawX, rawY, item);
                } else {
                    trade.updateTargetItem(rawX, rawY, item);
                }
            });

        }
    }

    public void updateTrade(Trade trade) {
        String serialized = gson.toJson(trade, Trade.class);
        System.out.println(serialized);
        getConnectionAsync(asyncCommands ->
                asyncCommands.publish(DataKeys.UPDATE_TRADE.toString(), serialized));
    }


}
