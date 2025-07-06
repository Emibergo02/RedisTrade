package dev.unnm3d.redistrade.hooks;

import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;
import java.util.function.Consumer;

public class BedrockHook {
    private final FloodgateApi floodgateApi;

    public BedrockHook() {
        this.floodgateApi = FloodgateApi.getInstance();
    }

    public void openAmountSelector(Player player, Consumer<String> resultHandler, Runnable closeHandler) {
        if (!floodgateApi.isFloodgatePlayer(player.getUniqueId())) {
            return;
        }
        CustomForm cf = CustomForm.builder().title("Amount editor")
                .input("Amount", "Enter amount")
                .validResultHandler(result -> resultHandler.accept(result.asInput()))
                .closedResultHandler(form -> closeHandler.run())
                .build();
        floodgateApi.sendForm(player.getUniqueId(), cf);
    }
}
