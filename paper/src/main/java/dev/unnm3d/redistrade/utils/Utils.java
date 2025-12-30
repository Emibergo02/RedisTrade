package dev.unnm3d.redistrade.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

@UtilityClass
public class Utils {

    public String parseDoubleFormat(double value) {
        return new DecimalFormat("#.##").format(value);
    }

    public byte[] serialize(ItemStack... items) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {

            dataOutput.writeInt(items.length);

            for (ItemStack item : items)
                dataOutput.writeObject(item);

            return compress(outputStream.toByteArray());

        } catch (Exception exception) {
            exception.printStackTrace();
            return new byte[0];
        }
    }

    public ItemStack[] deserialize(byte[] source) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(decompress(source));
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

    public static byte[] compress(byte[] input) {
        Deflater deflater = new Deflater();
        deflater.setInput(input);
        deflater.finish();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];

        while (!deflater.finished()) {
            int compressedSize = deflater.deflate(buffer);
            outputStream.write(buffer, 0, compressedSize);
        }

        return outputStream.toByteArray();
    }

    public static byte[] decompress(byte[] input) throws DataFormatException {
        Inflater inflater = new Inflater();
        inflater.setInput(input);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];

        while (!inflater.finished()) {
            int decompressedSize = inflater.inflate(buffer);
            outputStream.write(buffer, 0, decompressedSize);
        }

        return outputStream.toByteArray();
    }

    public String starsOf(double rating) {
        int fullStars = (int) rating;
        double halfStars = rating - fullStars;
        boolean halfStar = 0.25 < halfStars && halfStars < 0.75;
        if (rating < 0.25) return "N/A";
        return "\uD83D\uDFCA".repeat(fullStars) + (halfStar ? "⯨" : "");
    }
}
