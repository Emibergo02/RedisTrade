package dev.unnm3d.redistrade;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.unnm3d.redistrade.objects.Trader;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;

import java.util.List;

@AllArgsConstructor
public class Commands {
    private final RedisTrade plugin;


    public CommandAPICommand getTradeCommand() {
        return new CommandAPICommand("trade")
                .withArguments(new DoubleArgument("price"))
                .withArguments(new PlayerArgument("player"))
                .executesPlayer((player, args) -> {
                    final double price = (double) args.get(0);
                    final Player target = (Player) args.get(1);
                    System.out.println("Trade command executed");
                    if (target == null) return;

                    plugin.getDataCache().createTrader(new Trader(player.getUniqueId(), player.getName(), player.getInventory().getItemInMainHand()))
                            .thenAccept(trader -> {
                                plugin.getLogger().info("Trader created: " + trader);
                            }).exceptionally(throwable -> {
                                plugin.getLogger().severe("Error creating trader: " + throwable);
                                return null;
                            });
                    plugin.getDataCache().createOrder(player, target.getName(), List.of(target.getInventory().getItemInMainHand()), price)
                            .thenAccept(order -> {
                                plugin.getLogger().info("Order created: " + order);
                            }).exceptionally(throwable -> {
                                plugin.getLogger().severe("Error creating order: " + throwable);
                                return null;
                            });
                });
    }

    public CommandAPICommand getTradeListCommand() {
        return new CommandAPICommand("tradelist")
                .executesPlayer((player, args) -> {

                    plugin.getDataCache().getOrders(new Trader(player.getUniqueId(), player.getName(), null))
                            .thenAccept(orders -> {
                                player.sendMessage("Your orders: " + orders);
                            });
                });
    }
}
