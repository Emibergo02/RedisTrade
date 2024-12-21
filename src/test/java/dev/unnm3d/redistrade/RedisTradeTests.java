package dev.unnm3d.redistrade;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import dev.unnm3d.redistrade.core.OrderInfo;
import dev.unnm3d.redistrade.core.TradeSide;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("RedisTrade test Spigot 1_21")
public class RedisTradeTests {


    private ServerMock server;
    private RedisTrade plugin;
    private PlayerMock trader1;
    private PlayerMock trader2;
    private PlayerMock trader3;

    @BeforeEach
    public void setUp() {
        try {
            // Start the mock server
            server = MockBukkit.mock();
            // Load your plugin
            plugin = MockBukkit.load(RedisTrade.class);
            server.addSimpleWorld("world");
            this.trader1 = server.addPlayer("Trader1");
            this.trader2 = server.addPlayer("Trader2");
            this.trader3 = server.addPlayer("Trader3");
        }catch (UnsupportedOperationException e){
            e.printStackTrace();
        }
    }

    @AfterEach
    public void tearDown() {
        // Stop the mock server
        MockBukkit.unmock();
    }

    @Test
    public void openTrade() {
        try {
        plugin.getTradeManager().startTrade(this.trader1, "Trader2");
        plugin.getTradeManager().getActiveTrade(this.trader1.getUniqueId()).ifPresent(trade -> {
            plugin.getTradeManager().openWindow(trade, this.trader2.getUniqueId());
            this.trader2.simulateInventoryClick(0);
            this.trader1.simulateInventoryClick(0);

            assertEquals(OrderInfo.Status.CONFIRMED, trade.getTraderSide().getOrder().getStatus());
            assertEquals(OrderInfo.Status.CONFIRMED, trade.getOtherSide().getOrder().getStatus());

        });
        }catch (UnsupportedOperationException e){
            e.printStackTrace();
        }
    }

    @Test
    public void serialization() {
        OrderInfo order = new OrderInfo(20);
        order.setPrice("emerald", 10);
        TradeSide side = new TradeSide(UUID.randomUUID(), "Trader1", order);
    }


}
