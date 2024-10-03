package dev.unnm3d.redistrade.objects.trade;

import dev.unnm3d.redistrade.data.DataKeys;
import dev.unnm3d.redistrade.data.RedisDataManager;
import org.bukkit.inventory.ItemStack;

public class RemoteTrade extends TradeGUI {
    private final RedisDataManager dataManager;

    public RemoteTrade(RedisDataManager dataManager) {
        super();
        this.dataManager = dataManager;
    }

    public RemoteTrade(Trade trade, RedisDataManager dataManager) {
        super(trade);
        this.dataManager = dataManager;
    }

    public void setAndSendTraderPrice(double price) {
        super.setTraderPrice(price);
        dataManager.getConnectionAsync(connection->
                connection.publish(DataKeys.UPDATE_MONEY.toString(), getUuid() + ":true:" + price));
    }

    public void setAndSendTargetPrice(double price) {
        super.setTargetPrice(price);
        dataManager.getConnectionAsync(connection->
                connection.publish(DataKeys.UPDATE_MONEY.toString(), getUuid() + ":false:" + price));
    }

    public void setAndSendTraderConfirm(boolean confirm) {
        super.setTraderConfirm(confirm);
        dataManager.getConnectionAsync(connection->
                connection.publish(DataKeys.UPDATE_CONFIRM.toString(), getUuid() + ":true:" + confirm));
    }

    public void setAndSendTargetConfirm(boolean confirm) {
        super.setTargetConfirm(confirm);
        dataManager.getConnectionAsync(connection->
                connection.publish(DataKeys.UPDATE_CONFIRM.toString(), getUuid() + ":false:" + confirm));
    }

    public void updateAndSendTraderItem(int rawX, int rawY, ItemStack item) {
        super.updateTraderItem(rawX, rawY, item);
        dataManager.getConnectionAsync(connection->
                connection.publish(DataKeys.UPDATE_ITEM.toString(), getUuid() + "§;true§;" + rawX + "§;" + rawY + "§;" + item));
    }

    public void updateAndSendTargetItem(int rawX, int rawY, ItemStack item) {
        super.updateTargetItem(rawX, rawY, item);
        dataManager.getConnectionAsync(connection->
                connection.publish(DataKeys.UPDATE_ITEM.toString(), getUuid() + "§;false§;" + rawX + "§;" + rawY + "§;" + item));
    }

}
