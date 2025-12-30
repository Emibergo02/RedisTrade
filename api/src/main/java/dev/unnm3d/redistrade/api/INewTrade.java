// file: paper/src/main/java/dev/unnm3d/redistrade/core/INewTrade.java
package dev.unnm3d.redistrade.api;

import dev.unnm3d.redistrade.api.enums.Actor;
import dev.unnm3d.redistrade.api.enums.Status;
import dev.unnm3d.redistrade.api.enums.StatusActor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface INewTrade {

    UUID getUuid();

    ITradeSide getTraderSide();

    ITradeSide getCustomerSide();

    Actor getActor(Player player);

    Actor getActor(UUID playerUUID);

    boolean isParticipant(UUID playerUUID);

    ITradeSide getTradeSide(Actor actor);

    boolean isTrader(UUID playerUUID);

    boolean isCustomer(UUID playerUUID);

    void setAndSendPrice(String currencyName, double price, Actor actorSide);

    CompletionStage<Status> changeAndSendStatus(StatusActor newStatus, Status previousStatus, Actor viewerSide);

    void sendItemUpdate(int slot, ItemStack item, Actor tradeSide);
}
