package dev.unnm3d.redistrade.commands;

import com.jonahseguin.drink.annotation.Command;
import com.jonahseguin.drink.annotation.Sender;
import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.guis.CreateOrderGUI;
import dev.unnm3d.redistrade.objects.Order;
import dev.unnm3d.redistrade.objects.Trader;
import org.bukkit.entity.Player;

import java.util.List;

public class TradeCommand {

    public TradeCommand(RedisTrade redisTrade) {
    }

    @Command(name = "", desc = "Open the emporium")
    public void openTradeGUI(@Sender Player player) {
        Order order = new Order(-1, System.currentTimeMillis(), null, new Trader(player.getUniqueId(), player.getName(), null),
                List.of(player.getInventory().getItemInMainHand()), false, false, (short) 0, 0);
        new CreateOrderGUI(order).open(player);
    }

}
