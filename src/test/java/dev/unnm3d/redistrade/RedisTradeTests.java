package dev.unnm3d.redistrade;

import dev.unnm3d.redistrade.core.OrderInfo;
import dev.unnm3d.redistrade.core.TradeSide;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

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
            server.addSimpleWorld("world");
            //MockBukkit.load(RedisEconomyPlugin.class);
            // Load your plugin
            plugin = MockBukkit.load(RedisTrade.class);

            this.trader1 = server.addPlayer("Trader1");
            this.trader2 = server.addPlayer("Trader2");
            this.trader3 = server.addPlayer("Trader3");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    public void tearDown() {
        // Stop the mock server
        MockBukkit.unmock();
    }


    @Test
    public void tradeSideSerialization() {
        OrderInfo order = new OrderInfo(20);
        order.setPrice("emerald", 10);
        order.setStatus(OrderInfo.Status.CONFIRMED);
        order.getVirtualInventory().setItemSilently(5, new ItemStack(Material.BARRIER));
        TradeSide side = new TradeSide(UUID.randomUUID(), "Trader1", order);
        byte[] bytes = side.serialize();
        TradeSide side1 = TradeSide.deserialize(bytes);
        assertEquals(side, side1);
        assertEquals(side.getOrder().getPrice("emerald"), side1.getOrder().getPrice("emerald"));
        assertEquals(side.getOrder().getStatus(), side1.getOrder().getStatus());
        assertEquals(side.getOrder().getVirtualInventory().getItem(5), side1.getOrder().getVirtualInventory().getItem(5));
        assertEquals(side.getTraderName(), side1.getTraderName());
    }


}
