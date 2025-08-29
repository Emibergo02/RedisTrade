package dev.unnm3d.redistrade.guis;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.NewTrade;
import dev.unnm3d.redistrade.core.enums.Actor;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.text.ParseException;
import java.util.UUID;

public class BedrockMoneySelectorGUI extends MoneySelector {
    private final CustomForm.Builder bedrockForm;

    public BedrockMoneySelectorGUI(NewTrade trade, Actor playerSide, Player player, String currencyName) {
        super(trade, playerSide, player, currencyName);
        this.bedrockForm = CustomForm.builder().title("Amount editor")
                .input("Amount", "Enter amount", changingPriceString)
                .validResultHandler(result -> {
                    changingPriceString = result.asInput();
                    handleConfirm();
                });
    }

    public static boolean isBedrockPlayer(UUID playerUUID) {
        return FloodgateApi.getInstance().isFloodgatePlayer(playerUUID);
    }

    @Override
    public void openWindow() {
        FloodgateApi.getInstance().sendForm(player.getUniqueId(), bedrockForm);
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
                trade.openWindow(player, playerSide);
                return;
            }
            notifyInsufficientFunds(nextPrice, balance + previousPrice);

        } catch (NumberFormatException | ParseException ignored) {
            changingPriceString = Settings.getDecimalFormat().format(previousPrice);
        }
        openWindow();
    }

    private double parseNextPrice() throws ParseException, NumberFormatException {
        if (changingPriceString.equalsIgnoreCase("NaN")) {
            throw new NumberFormatException("NaN is not a supported notation");
        }
        return Math.abs(Settings.getDecimalFormat().parse(changingPriceString).doubleValue());
    }

    public void handleClose() {
        trade.openWindow(player, playerSide);
    }
}