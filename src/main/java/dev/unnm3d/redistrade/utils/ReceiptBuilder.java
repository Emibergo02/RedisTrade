package dev.unnm3d.redistrade.utils;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.GuiSettings;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.NewTrade;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
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
        // Create the receipt item
        ItemStack receipt = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta writtenMeta = (BookMeta) receipt.getItemMeta();

        // Format the timestamp
        String parsedDate = dateFormat.format(new Date(timestamp));

        // Build the receipt intestation
        for (List<String> formatList : GuiSettings.instance().receiptIntestationFormat) {
            Component pageContent = Component.empty();
            for (Iterator<String> it = formatList.iterator(); it.hasNext(); ) {
                pageContent = pageContent.append(MiniMessage.miniMessage().deserialize(
                        tradePlaceholders(trade, it.next()).replace("%timestamp%", parsedDate)
                ));
                if (it.hasNext()) {
                    pageContent = pageContent.append(Component.newline());
                }
            }
            writtenMeta.addPages(pageContent);
        }

        // Add pages for trader and customer items
        buildPages(true, trade.getTraderSide().getOrder().getVirtualInventory().getItems())
                .forEach(writtenMeta::addPages);

        buildPages(false, trade.getCustomerSide().getOrder().getVirtualInventory().getItems())
                .forEach(writtenMeta::addPages);

        // Set the book display name
        writtenMeta.itemName(MiniMessage.miniMessage().deserialize(
                tradePlaceholders(trade, "<!i>" + GuiSettings.instance().receiptBookDisplayName)
                        .replace("%timestamp%", parsedDate)
        ));

        // Build the book lore
        List<Component> lore = new ArrayList<>();
        for (String loreString : GuiSettings.instance().receiptBookLore) {
            if (loreString.contains("%items%")) {
                // Add trader items to lore
                addItemsToLore(lore, trade.getTraderSide().getOrder().getVirtualInventory().getItems());

                // Add customer items to lore
                addItemsToLore(lore, trade.getCustomerSide().getOrder().getVirtualInventory().getItems());
            } else {
                lore.add(MiniMessage.miniMessage().deserialize("<white><!i>" +
                        tradePlaceholders(trade, loreString).replace("%timestamp%", parsedDate)
                ));
            }
        }
        writtenMeta.lore(lore);

        // Finalize the receipt item meta
        receipt.setItemMeta(writtenMeta);

        // Return the customized Item instance
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

    private void addItemsToLore(List<Component> lore, ItemStack[] items) {
        Arrays.stream(items)
                .filter(Objects::nonNull)
                .map(item -> MiniMessage.miniMessage().deserialize(
                        GuiSettings.instance().itemDisplayLoreFormat.replace("%amount%", String.valueOf(item.getAmount()))
                ).replaceText(rBuilder -> rBuilder.matchLiteral("%item_display%")
                        .replacement(getItemDisplay(item.getItemMeta(), item.translationKey()))))
                .forEach(lore::add);
    }


    public String tradePlaceholders(NewTrade trade, String toParse) {
        String strText = toParse.replace("%trader%", trade.getTraderSide().getTraderName())
                .replace("%customer%", trade.getCustomerSide().getTraderName())
                .replace("%trade_uuid%", trade.getUuid().toString());

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

    private List<Component> buildPages(boolean isTrader, ItemStack... items) {
        List<Component> pages = new ArrayList<>();
        String headerText = isTrader ? GuiSettings.instance().traderItemsIntestation : GuiSettings.instance().customerItemsIntestation;

        // Initialize the first page with the header
        Component currentPage = MiniMessage.miniMessage().deserialize(headerText).appendNewline();
        int lineCount = 0;
        final int maxLinesPerPage = 6;

        for (ItemStack item : items) {
            if (item == null) continue;

            // Add the current page to the list if it exceeds the line limit
            if (lineCount >= maxLinesPerPage) {
                pages.add(currentPage);
                currentPage = MiniMessage.miniMessage().deserialize(headerText).appendNewline();
                lineCount = 0;
            }

            // Format the item name with placeholders and hover event
            final Component itemName = MiniMessage.miniMessage().deserialize(
                            GuiSettings.instance().itemFormat.replace("%amount%", String.valueOf(item.getAmount())))
                    .replaceText(rBuilder -> rBuilder.matchLiteral("%item_name%")
                            .replacement(getItemDisplay(item.getItemMeta(), item.translationKey())))
                    .hoverEvent(item.asHoverEvent());

            // Append the item name to the current page
            currentPage = currentPage.append(itemName).appendNewline();
            lineCount++;
        }

        // Add the last page to the list
        if (!currentPage.equals(Component.empty())) {
            pages.add(currentPage);
        }

        return pages;
    }


    private Component getItemDisplay(ItemMeta itemMeta, String translationKey) {
        if (itemMeta.hasDisplayName()) return itemMeta.displayName();
        if (itemMeta.hasItemName()) return itemMeta.itemName();
        return Component.translatable(translationKey);
    }

}
