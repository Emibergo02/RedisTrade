package dev.unnm3d.redistrade.configs;


import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import dev.unnm3d.redistrade.utils.MyItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

@Configuration
public class GuiSettings {
    private static GuiSettings SETTINGS;

    public static GuiSettings instance() {
        return SETTINGS;
    }

    public static void loadGuiSettings(Path configFile) {
        SETTINGS = YamlConfigurations.update(
                configFile,
                GuiSettings.class,
                YamlConfigurationProperties.newBuilder()
                        .header("RedisChat guis")
                        .footer("Authors: Unnm3d")
                        .charset(StandardCharsets.UTF_8)
                        .build()
        );
    }

    public static void saveGuiSettings(Path configFile) {
        YamlConfigurations.save(configFile, GuiSettings.class, SETTINGS,
                YamlConfigurationProperties.newBuilder()
                        .header("RedisChat guis")
                        .footer("Authors: Unnm3d")
                        .charset(StandardCharsets.UTF_8)
                        .build()
        );
    }

    public SimpleSerializableItem nextPage = new SimpleSerializableItem("ARROW", 1, 0, "<blue>Next Page", List.of());
    public SimpleSerializableItem previousPage = new SimpleSerializableItem("ARROW", 1, 0, "<blue>Previous Page", List.of());
    public SimpleSerializableItem refuseButton = new SimpleSerializableItem("RED_WOOL", 1, 0, "<red>Refused trade", List.of("", "<white>Click to <dark_green>confirm</dark_green> the trade</white>"));
    public SimpleSerializableItem confirmButton = new SimpleSerializableItem("GREEN_WOOL", 1, 0, "<green>Confirm trade", List.of("", "<white>Click to <red>cancel</red> the trade</white>"));
    public SimpleSerializableItem cancelTradeButton = new SimpleSerializableItem("BARRIER", 1, 0, "<red>Cancel trade", List.of("", "<white>Click to <red>cancel</red> the trade</white>"));
    public SimpleSerializableItem completedButton = new SimpleSerializableItem("LIME_WOOL", 1, 0, "<green>Completed trade", List.of(""));
    public SimpleSerializableItem retrievedButton = new SimpleSerializableItem("LIGHT_BLUE_WOOL", 1, 0, "<blue>Retrieved trade", List.of(""));
    public SimpleSerializableItem moneyDisplay = new SimpleSerializableItem("GOLD_NUGGET", 1, 0, "<yellow>%amount% %currency%", List.of());
    public SimpleSerializableItem moneyConfirmButton = new SimpleSerializableItem("GOLD_BLOCK", 1, 0, "<yellow>Confirm", List.of());
    public SimpleSerializableItem separator = new SimpleSerializableItem("GRAY_STAINED_GLASS_PANE", 1, 0, "", List.of());

    @Comment({"Remember that a book line contains 20 large characters",
            "(if you use 'i's or 'l's it will be contain more characters)",
            "\"default\" is the name of the currency name of the displayed price or symbol"})
    public List<List<String>> receiptIntestationFormat = List.of(
            List.of(
                    "Trade Receipt",
                    "",
                    "<black>Trader: <blue>%trader%</blue>",
                    "",
                    "<black>Customer: <blue>%customer%</blue>",
                    "",
                    "Date: ",
                    "<blue>%timestamp%</blue>",
                    "",
                    "Trader price: <gold>%price_default_trader%%symbol_default%</gold>",
                    "Customer price: <gold>%price_default_customer%%symbol_default%</gold>"
            )
    );

    public String receiptBookDisplayName = "<white>%trader%'s Receipt";

    @Comment({"Remember that a book line contains 20 large characters",
            "(if you use 'i's or 'l's it will be contain more characters)",
            "\"default\" is the name of the currency name of the displayed price or symbol"})
    public List<String> receiptBookLore = List.of(
            "Trader: <blue>%trader%</blue>",
            "Customer: <blue>%customer%</blue>",
            "Date: ",
            "<blue>%timestamp%</blue>",
            "Trader price: <gold>%price_default_trader%%symbol_default%</gold>",
            "Customer price: <gold>%price_default_customer%%symbol_default%</gold>",
            "Exchanged items:",
            "%items%"
    );
    public String itemDisplayLoreFormat = "<!i><gray>[x%amount% %item_display%]";

    public String traderItemsIntestation = "<bold>Trader items: </bold>";
    public String targetItemsIntestation = "<bold>Customer items: </bold>";
    @Comment("%item_name% - item displayname or itemname or , %amount% - item amount")
    public String itemFormat = "<gray>[x%amount% %item_name%]";

    public String tradeGuiTitle = "Trading with %player%";
    public List<String> tradeGuiStructure = List.of(
            "CMNODonmc",
            "LLLLxRRRR",
            "LLLLxRRRR",
            "LLLLxRRRR",
            "LLLLxRRRR",
            "LLLLxRRRR");

    public record SimpleSerializableItem(String material, int amount, int customModelData, String itemName,
                                         List<String> lore) {
        public static SimpleSerializableItem fromItemStack(@NotNull ItemStack item) {
            final String serializedItemName = MiniMessage.miniMessage().serialize(
                    item.getItemMeta().hasDisplayName() ?
                            item.getItemMeta().displayName() :
                            item.getItemMeta().hasItemName() ?
                                    item.getItemMeta().itemName() :
                                    Component.empty());
            final List<String> serializedLore = item.getItemMeta().hasLore() ?
                    item.getItemMeta().lore().stream()
                            .map(MiniMessage.miniMessage()::serialize)
                            .toList() :
                    List.of();

            return new SimpleSerializableItem(
                    item.getType().name(),
                    item.getAmount(),
                    item.getItemMeta().hasCustomModelData() ?
                            item.getItemMeta().getCustomModelData() :
                            0,
                    serializedItemName,
                    serializedLore
            );
        }

        public MyItemBuilder toItemBuilder() {
            Material mat = Material.getMaterial(material);
            if (mat == null) {
                throw new IllegalArgumentException("Material " + material + " not found in this MC version");
            }
            final MyItemBuilder builder = new MyItemBuilder(mat);
            builder.setAmount(amount);
            builder.setCustomModelData(customModelData);
            builder.setMiniMessageItemName(itemName);
            builder.addMiniMessageLoreLines(lore.toArray(new String[0]));
            return builder;
        }
    }
}
