package dev.unnm3d.redistrade.data;

import com.google.gson.Gson;
import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.objects.Order;
import dev.unnm3d.redistrade.objects.Trader;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class DataCache {
    protected ConcurrentHashMap<String, Trader> traderCache;
    protected ConcurrentHashMap<Integer, Order> orderCache;
    protected ExecutorService executorService;

    public String serialize(ItemStack... items) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {

            dataOutput.writeInt(items.length);

            for (ItemStack item : items)
                dataOutput.writeObject(item);

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());

        } catch (Exception exception) {
            exception.printStackTrace();
            return "";
        }
    }

    public ItemStack[] deserialize(String source) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(source));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {

            final ItemStack[] items = new ItemStack[dataInput.readInt()];

            for (int i = 0; i < items.length; i++)
                items[i] = (ItemStack) dataInput.readObject();

            return items;
        } catch (Exception exception) {
            exception.printStackTrace();
            return new ItemStack[0];
        }
    }

    public void load() {
        traderCache = new ConcurrentHashMap<>();
        orderCache = new ConcurrentHashMap<>();
        executorService = Executors.newSingleThreadExecutor();
        RedisTrade.getInstance().getServer().getOnlinePlayers().forEach(player -> {
            getTrader(player.getUniqueId()).thenAccept(trader -> {
                updateTraderCache(trader.orElse(null));
                RedisTrade.getInstance().getLogger().info("Loaded trader " + player.getName());
            });
        });
    }

    public Trader updateTraderCache(@Nullable Trader trader) {
        if (trader == null) return null;
        traderCache.put(trader.getName(), trader);
        return trader;
    }

    public Order updateOrderCache(@Nullable Order order) {
        if (order == null) return null;
        orderCache.put(order.getId(), order);
        return order;
    }

    public abstract CompletableFuture<Optional<Trader>> getTrader(UUID uuid);

    public abstract CompletableFuture<Void> createTrader(Trader trader);

    public abstract CompletableFuture<Void> updateTrader(Trader trader);

    public abstract CompletableFuture<Optional<Order>> createOrder(Player seller, String buyerName, List<ItemStack> items, double offer);

    public abstract CompletableFuture<Void> updateOrder(Order order);

    public abstract CompletableFuture<List<Order>> getOrders(Trader trader);
}