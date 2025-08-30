package dev.unnm3d.redistrade.guis;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.GuiSettings;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.core.enums.Actor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.util.UUID;

public class BedrockMoneySelectorGUI extends MoneySelector {

    public BedrockMoneySelectorGUI(NewTrade trade, Actor playerSide, Player player, String currencyName) {
        super(trade, playerSide, player, currencyName);
    }

    public static boolean isBedrockPlayer(UUID playerUUID) {
        return FloodgateApi.getInstance().isFloodgatePlayer(playerUUID);
    }

    @Override
    public void openWindow() {
        player.closeInventory();
        sendDelayedForm(createForm(null));
    }

    private CustomForm createForm(@Nullable String errorMessage) {
        final CustomForm.Builder builder = CustomForm.builder()
                .title(LegacyComponentSerializer.legacySection().serialize(
                        MiniMessage.miniMessage().deserialize(GuiSettings.instance().moneyEditorTitle)))
                .input(GuiSettings.instance().moneyEditorLabel, "", changingPriceString);
        if (errorMessage != null) {
            builder.label(errorMessage);
        }
        return builder.validResultHandler(result -> {
            changingPriceString = result.asInput();
            handleConfirm();
        }).build();
    }

    public void handleConfirm() {
        try {
            double nextPrice = parseNextPrice();
            double balance = RedisTrade.getInstance()
                    .getIntegrationManager().getCurrencyHook(currencyName)
                    .getBalance(player.getUniqueId());
            double priceDifference = previousPrice - nextPrice;

            if (processTransaction(priceDifference)) {
                if (priceDifference != 0) {
                    trade.setAndSendPrice(currencyName, nextPrice, playerSide);
                }
                RedisTrade.getInstance().getServer().getScheduler().runTaskLater(RedisTrade.getInstance(), () ->
                        trade.openWindow(player, playerSide), 10L);
                return;
            }

            changingPriceString = Settings.getDecimalFormat().format(previousPrice + balance);
            sendDelayedForm(createForm("§cInsufficient funds!"));
            return;
        } catch (NumberFormatException | ParseException ignored) {
            changingPriceString = Settings.getDecimalFormat().format(previousPrice);
        }
        sendDelayedForm(createForm("§cInvalid format!"));
    }

    /**
     * Floodgate requires forms to be sent not immediately after closing an inventory
     * Doesn't work every time, it depends if the packets arrive in the right order (can't do much about it)
     *
     * @param form the form to send
     */
    private void sendDelayedForm(CustomForm form) {
        RedisTrade.getInstance().getServer().getScheduler().runTaskLaterAsynchronously(RedisTrade.getInstance(), () ->
                FloodgateApi.getInstance().sendForm(player.getUniqueId(), form), 10L);
    }

    private double parseNextPrice() throws ParseException, NumberFormatException {
        if (changingPriceString.equalsIgnoreCase("NaN")) {
            throw new NumberFormatException("NaN is not a supported notation");
        }
        return Math.abs(Settings.getDecimalFormat().parse(changingPriceString).doubleValue());
    }
}