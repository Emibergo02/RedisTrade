package dev.unnm3d.redistrade.utils;

import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.objects.NewTrade;
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
        for (List<String> formatList : Settings.instance().receiptIntestationFormat) {
            Component text = Component.empty();
            final Iterator<String> itStr = formatList.iterator();
            while (itStr.hasNext()) {
                text = text.append(MiniMessage.miniMessage().deserialize(itStr.next()
                        .replace("%trader%", trade.getTraderName())
                        .replace("%target%", trade.getTargetName())
                        .replace("%timestamp%", parsedDate)
                        .replace("%trader_price%", decimalFormat.format(trade.getTraderSideInfo().getProposed()))
                        .replace("%target_price%", decimalFormat.format(trade.getTargetSideInfo().getProposed()))
                ));
                if (itStr.hasNext()) {
                    text = text.append(Component.newline());
                }
            }
            writtenMeta.addPages(text);
        }

        buildPages(true, trade.getTraderSideInfo().getVirtualInventory().getItems())
                .forEach(writtenMeta::addPages);

        buildPages(false, trade.getTargetSideInfo().getVirtualInventory().getItems())
                .forEach(writtenMeta::addPages);

        writtenMeta.itemName(MiniMessage.miniMessage().deserialize(
                Settings.instance().receiptBookDisplayName
                        .replace("%trader%", trade.getTraderName())
                        .replace("%target%", trade.getTargetName())
                        .replace("%id%", String.valueOf(trade.getUuid().getMostSignificantBits()))
        ));

        final List<Component> lore = new ArrayList<>();
        for (String loreString : Settings.instance().receiptBookLore) {
            if (loreString.contains("%items%")) {
                //Add items to lore
                Arrays.stream(trade.getTraderSideInfo().getVirtualInventory().getItems())
                        .filter(Objects::nonNull)
                        .map(item -> MiniMessage.miniMessage().deserialize(Settings.instance().itemDisplayLoreFormat
                                        .replace("%amount%", String.valueOf(item.getAmount())))
                                .replaceText(rBuilder -> rBuilder.matchLiteral("%item_display%")
                                        .replacement(getItemDisplay(item.getItemMeta(), item.translationKey()))))
                                .forEach(lore::add);
                Arrays.stream(trade.getTargetSideInfo().getVirtualInventory().getItems())
                        .filter(Objects::nonNull)
                        .map(item -> MiniMessage.miniMessage().deserialize(Settings.instance().itemDisplayLoreFormat
                                        .replace("%amount%", String.valueOf(item.getAmount())))
                                .replaceText(rBuilder -> rBuilder.matchLiteral("%item_display%")
                                        .replacement(getItemDisplay(item.getItemMeta(), item.translationKey()))))
                        .forEach(lore::add);
                continue;
            }
            lore.add(MiniMessage.miniMessage().deserialize("<white><!i>" + loreString
                    .replace("%trader%", trade.getTraderName())
                    .replace("%target%", trade.getTargetName())
                    .replace("%timestamp%", parsedDate)
                    .replace("%trader_price%", decimalFormat.format(trade.getTraderSideInfo().getProposed()))
                    .replace("%target_price%", decimalFormat.format(trade.getTargetSideInfo().getProposed())))
            );
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

    private List<Component> buildPages(boolean trader, ItemStack... items) {
        final List<Component> pages = new ArrayList<>();
        Component currentPage = MiniMessage.miniMessage().deserialize(
                trader ? Settings.instance().traderItemsIntestation : Settings.instance().targetItemsIntestation
        ).appendNewline();
        int line = 1;
        for (ItemStack item : items) {
            if (item == null) continue;
            if (line > 6) {//2*6=12 +1 title = 13 (14 is the line limit)
                pages.add(currentPage);
                currentPage = MiniMessage.miniMessage().deserialize(
                        trader ? Settings.instance().traderItemsIntestation : Settings.instance().targetItemsIntestation
                ).appendNewline();
                line = 1;
            }

            final ItemMeta itemMeta = item.getItemMeta();

            Component itemName = MiniMessage.miniMessage().deserialize(Settings.instance().itemFormat
                    .replace("%amount%", String.valueOf(item.getAmount())));
            itemName = itemName.replaceText(rBuilder -> rBuilder.matchLiteral("%item_name%").replacement(
                    getItemDisplay(itemMeta, item.translationKey()))
            ).hoverEvent(item.asHoverEvent());

            currentPage = currentPage.append(itemName);
            currentPage = currentPage.appendNewline();
            line++;
        }
        pages.add(currentPage);
        return pages;
    }

    private Component getItemDisplay(ItemMeta itemMeta, String translationKey) {
        if (itemMeta.hasItemName()) return itemMeta.itemName();
        return Component.translatable(translationKey);
    }

}
