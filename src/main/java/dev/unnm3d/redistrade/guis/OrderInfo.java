package dev.unnm3d.redistrade.guis;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Getter
@ToString
@EqualsAndHashCode
public class OrderInfo {
    @Setter
    private boolean confirmed;
    private final ItemStack[] items;
    @Setter
    private double proposed;

    public OrderInfo(int orderSize) {
        this.confirmed = false;
        this.items = new ItemStack[orderSize];
        this.proposed = 0;
    }

    private OrderInfo(boolean confirmed, ItemStack[] items, double proposed) {
        this.confirmed = false;
        this.items = items;
        this.proposed = 0;
    }

    public void toggleConfirmed() {
        confirmed = !confirmed;
    }

    public void setItem(int index, ItemStack item) {
        items[index] = item;
    }

    public ItemStack getItem(int index) {
        return items[index];
    }

    public String serialize() {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeBoolean(confirmed);

            dataOutput.writeInt(items.length);
            for (ItemStack item : items)
                dataOutput.writeObject(item);

            dataOutput.writeDouble(proposed);

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());

        } catch (Exception exception) {
            exception.printStackTrace();
            return "";
        }
    }

    public static OrderInfo deserialize(String source) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(source));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            //Gather data
            boolean confirmed = dataInput.readBoolean();
            final ItemStack[] items = new ItemStack[dataInput.readInt()];
            for (int i = 0; i < items.length; i++)
                items[i] = (ItemStack) dataInput.readObject();
            double proposed = dataInput.readDouble();

            //Build order info
            return new OrderInfo(confirmed, items, proposed);
        } catch (Exception exception) {
            exception.printStackTrace();
            return new OrderInfo(0);
        }
    }

}
