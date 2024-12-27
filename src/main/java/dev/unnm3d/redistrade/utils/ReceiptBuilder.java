package dev.unnm3d.redistrade.utils;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.GuiSettings;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.NewTrade;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.ItemWrapper;
import xyz.xenondevs.invui.item.impl.AbstractItem;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@UtilityClass
public class ReceiptBuilder {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private static final DecimalFormat decimalFormat = new DecimalFormat("#.##");

    public Item buildReceipt(NewTrade trade, long timestamp) {
        ItemStack receipt = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta writtenMeta = (BookMeta) receipt.getItemMeta();
        String parsedDate = dateFormat.format(new Date(timestamp));

        //Create intestation
        for (List<String> formatList : GuiSettings.instance().receiptIntestationFormat) {
            Component componentText = Component.empty();
            final Iterator<String> itStr = formatList.iterator();
            while (itStr.hasNext()) {
                componentText = componentText.append(MiniMessage.miniMessage().deserialize(
                        tradePlaceholders(trade, itStr.next()).replace("%timestamp%", parsedDate)
                ));
                if (itStr.hasNext()) {
                    componentText = componentText.append(Component.newline());
                }
            }
            writtenMeta.addPages(componentText);
        }

        buildPages(true, trade.getTraderSide().getOrder().getVirtualInventory().getItems())
                .forEach(writtenMeta::addPages);

        buildPages(false, trade.getCustomerSide().getOrder().getVirtualInventory().getItems())
                .forEach(writtenMeta::addPages);

        writtenMeta.itemName(MiniMessage.miniMessage().deserialize(
                tradePlaceholders(trade, GuiSettings.instance().receiptBookDisplayName)
                        .replace("%timestamp%", parsedDate)));

        final List<Component> lore = new ArrayList<>();
        for (String loreString : GuiSettings.instance().receiptBookLore) {
            if (loreString.contains("%items%")) {
                //Add items to lore
                Arrays.stream(trade.getTraderSide().getOrder().getVirtualInventory().getItems())
                        .filter(Objects::nonNull)
                        .map(item -> MiniMessage.miniMessage().deserialize(GuiSettings.instance().itemDisplayLoreFormat
                                        .replace("%amount%", String.valueOf(item.getAmount())))
                                .replaceText(rBuilder -> rBuilder.matchLiteral("%item_display%")
                                        .replacement(getItemDisplay(item.getItemMeta(), item.translationKey()))))
                        .forEach(lore::add);
                Arrays.stream(trade.getCustomerSide().getOrder().getVirtualInventory().getItems())
                        .filter(Objects::nonNull)
                        .map(item -> MiniMessage.miniMessage().deserialize(GuiSettings.instance().itemDisplayLoreFormat
                                        .replace("%amount%", String.valueOf(item.getAmount())))
                                .replaceText(rBuilder -> rBuilder.matchLiteral("%item_display%")
                                        .replacement(getItemDisplay(item.getItemMeta(), item.translationKey()))))
                        .forEach(lore::add);
                continue;
            }

            lore.add(MiniMessage.miniMessage().deserialize("<white><!i>" +
                    tradePlaceholders(trade, loreString)
                            .replace("%timestamp%", parsedDate)));
        }
        writtenMeta.lore(lore);

        receipt.setItemMeta(writtenMeta);
        return new AbstractItem() {
            @Override
            public ItemProvider getItemProvider() {
                return new ItemWrapper(receipt);
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                player.openBook(receipt);
            }
        };
    }

    public String tradePlaceholders(NewTrade trade, String toParse) {
        String strText = toParse.replace("%trader%", trade.getTraderSide().getTraderName())
                .replace("%customer%", trade.getCustomerSide().getTraderName())
                .replace("%id%", String.valueOf(trade.getUuid().getMostSignificantBits()));

        for (String currencyName : Settings.instance().allowedCurrencies.keySet()) {
            strText = strText.replace("%price_" + currencyName + "_trader%",
                            decimalFormat.format(trade.getTraderSide().getOrder().getPrice(currencyName)))
                    .replace("%price_" + currencyName + "_customer%",
                            decimalFormat.format(trade.getCustomerSide().getOrder().getPrice(currencyName)))
                    .replace("%symbol_" + currencyName + "%",
                            RedisTrade.getInstance().getEconomyHook().getCurrencySymbol(currencyName));
        }
        return strText;
    }

    private List<Component> buildPages(boolean trader, ItemStack... items) {
        final List<Component> pages = new ArrayList<>();
        Component currentPage = MiniMessage.miniMessage().deserialize(
                trader ? GuiSettings.instance().traderItemsIntestation : GuiSettings.instance().targetItemsIntestation
        ).appendNewline();
        int line = 1;
        for (ItemStack item : items) {
            if (item == null) continue;
            if (line > 6) {//2*6=12 +1 title = 13 (14 is the line limit)
                pages.add(currentPage);
                currentPage = MiniMessage.miniMessage().deserialize(
                        trader ? GuiSettings.instance().traderItemsIntestation : GuiSettings.instance().targetItemsIntestation
                ).appendNewline();
                line = 1;
            }

            final ItemMeta itemMeta = item.getItemMeta();

            Component itemName = MiniMessage.miniMessage().deserialize(GuiSettings.instance().itemFormat
                    .replace("%amount%", String.valueOf(item.getAmount())));
            itemName = itemName.replaceText(rBuilder -> rBuilder.matchLiteral("%item_name%")
                            .replacement(getItemDisplay(itemMeta, item.translationKey())))
                    .hoverEvent(item.asHoverEvent());

            currentPage = currentPage.append(itemName);
            currentPage = currentPage.appendNewline();
            line++;
        }
        pages.add(currentPage);
        return pages;
    }

    private Component getItemDisplay(ItemMeta itemMeta, String translationKey) {
        if (itemMeta.hasDisplayName()) return itemMeta.displayName();
        if (itemMeta.hasItemName()) return itemMeta.itemName();
        return Component.translatable(translationKey);
    }

}
