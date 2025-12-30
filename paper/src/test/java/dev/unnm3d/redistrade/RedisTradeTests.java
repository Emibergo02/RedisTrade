package dev.unnm3d.redistrade;

import dev.unnm3d.redistrade.api.IOrderInfo;
import dev.unnm3d.redistrade.api.ITradeSide;
import dev.unnm3d.redistrade.api.enums.Status;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import xyz.xenondevs.invui.InvUI;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
            //MockBukkit.load(RedisEconomyPlugin.class);
            // Load your plugin
            plugin = MockBukkit.load(RedisTrade.class);
            InvUI.getInstance().setPlugin(MockBukkit.createMockPlugin());
            this.trader1 = server.addPlayer("Trader1");
            this.trader2 = server.addPlayer("Trader2");
            this.trader3 = server.addPlayer("Trader3");
            plugin.getPlayerListManager().setPlayerNameUUID(trader1.getName(), trader1.getUniqueId());
            plugin.getPlayerListManager().setPlayerNameUUID(trader2.getName(), trader2.getUniqueId());
            plugin.getPlayerListManager().setPlayerNameUUID(trader3.getName(), trader3.getUniqueId());
            plugin.getPlayerListManager().updatePlayerList(List.of(trader1.getName(), trader2.getName(), trader3.getName()));
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
        IOrderInfo order = new IOrderInfo(20);
        order.setPrice("emerald", 10);
        order.setPrice("gold", 1);
        order.setStatus(Status.CONFIRMED);
        order.getVirtualInventory().setItemSilently(5, new ItemStack(Material.BARRIER));
        ITradeSide side = new ITradeSide(UUID.randomUUID(), "Trader1", order, true);
        byte[] bytes = side.serialize();


        ITradeSide side1 = ITradeSide.deserialize(bytes);
        assertEquals(side.getOrder().getPrice("emerald"), side1.getOrder().getPrice("emerald"));
        assertEquals(side.getOrder().getPrice("gold"), side1.getOrder().getPrice("gold"));
        assertEquals(side.getOrder().getStatus(), side1.getOrder().getStatus());
        assertTrue(side.isOpened());
        assertEquals(side.getOrder().getVirtualInventory().getItem(5), side1.getOrder().getVirtualInventory().getItem(5));
        assertEquals(side.getTraderName(), side1.getTraderName());
    }


}
