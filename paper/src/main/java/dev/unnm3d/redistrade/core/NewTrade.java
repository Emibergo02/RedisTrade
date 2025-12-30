package dev.unnm3d.redistrade.core;

import dev.unnm3d.redistrade.RedisTrade;
import dev.unnm3d.redistrade.api.INewTrade;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.api.enums.*;
import dev.unnm3d.redistrade.data.Database;
import dev.unnm3d.redistrade.guis.buttons.ReceiptButton;
import dev.unnm3d.redistrade.hooks.currencies.CurrencyHook;
import dev.unnm3d.redistrade.utils.Permissions;
import dev.unnm3d.redistrade.utils.Utils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.inventory.VirtualInventory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

@Getter
@EqualsAndHashCode
@ToString
public class NewTrade implements INewTrade {
    private CompletionTimer completionTimer;
    private final UUID uuid;
    private final TradeSide traderSide;
    private final TradeSide customerSide;


    public NewTrade(UUID traderUUID, UUID targetUUID, String traderName, String targetName) {
        this(UUID.randomUUID(), new TradeSide(traderUUID, traderName, new OrderInfo(25), false),
          new TradeSide(targetUUID, targetName, new OrderInfo(25), false));
    }

    public NewTrade(UUID uuid, TradeSide traderSide, TradeSide customerSide) {
        this.completionTimer = null;
        this.uuid = uuid;
        this.traderSide = traderSide;
        this.customerSide = customerSide;
    }

    public Actor getActor(Player player) {
        if (traderSide.getTraderUUID().equals(player.getUniqueId())) return Actor.TRADER;
        if (customerSide.getTraderUUID().equals(player.getUniqueId())) return Actor.CUSTOMER;
        if (player.hasPermission(Permissions.MODIFY_TRADE.getPermission())) return Actor.ADMIN;
        return Actor.SPECTATOR;
    }

    public Actor getActor(UUID playerUUID) {
        if (traderSide.getTraderUUID().equals(playerUUID)) return Actor.TRADER;
        if (customerSide.getTraderUUID().equals(playerUUID)) return Actor.CUSTOMER;
        return Actor.SPECTATOR;
    }

    public boolean isParticipant(UUID playerUUID) {
        return isTrader(playerUUID) || isCustomer(playerUUID);
    }

    public TradeSide getTradeSide(Actor actor) {
        if (actor == Actor.CUSTOMER) return customerSide;
        return traderSide;
    }

    //GETTERS
    public boolean isTrader(UUID playerUUID) {
        return traderSide.getTraderUUID().equals(playerUUID);
    }

    public boolean isCustomer(UUID playerUUID) {
        return customerSide.getTraderUUID().equals(playerUUID);
    }

    /**
     * Set the price of the trade
     *
     * @param price     The price to set
     * @param actorSide If the price is for the trader side
     */
    public void setPrice(String currencyName, double price, Actor actorSide) {
        getTradeSide(actorSide).setPrice(currencyName, price);
        getTradeSide(actorSide).notifyOppositePrice();
        getTradeSide(actorSide.opposite()).notifyOppositePrice();
        RedisTrade.debug(uuid + " Setting price to " + price + " for " + actorSide.name());
    }


    /**
     * Set the price of the trade and send the update to the database/cache
     *
     * @param price     The price to set
     * @param actorSide The side to set the price for
     */
    public void setAndSendPrice(String currencyName, double price, Actor actorSide) {
        setPrice(currencyName, price, actorSide);
        final ViewerUpdate updateType = ViewerUpdate.valueOf(actorSide, UpdateType.PRICE);
        RedisTrade.getInstance().getDataCache().updateTrade(uuid,
          updateType,
          currencyName + ":" + price);
    }

    /**
     * Returns the items of the trade to the player
     *
     * @param player    The player to return the items to
     * @param tradeSide The side of the trade to return the items from
     * @return The amount of returned items
     */
    public short returnItems(Player player, Actor tradeSide) {
        short returnedItems = 0;
        if (RedisTrade.getInstance().getIntegritySystem().isFaulted()) {
            player.sendRichMessage(Messages.instance().newTradesLock);
            return returnedItems;
        }
        Actor actor = getActor(player);
        if (!actor.isParticipant()) return returnedItems;

        final VirtualInventory traderInventory = getTradeSide(tradeSide).getOrder().getVirtualInventory();
        for (int i = 0; i < traderInventory.getItems().length; i++) {
            final ItemStack item = traderInventory.getItem(i);
            if (item == null) continue;
            //Set the item to null and send update to other servers
            traderInventory.setItemSilently(i, null);

            sendItemUpdate(i, null, tradeSide);

            player.getInventory().addItem(item).forEach((slot, itemStack) ->
              player.getWorld().dropItem(player.getLocation(), itemStack));
            returnedItems++;
        }
        return returnedItems;
    }

    /**
     * Refund the player on the specified side on all currencies
     *
     * @param refundSide The side of the trade to refund
     */
    public void refundSide(Actor refundSide) {
        TradeSide side = getTradeSide(refundSide);
        side.getOrder().getPrices().forEach((currency, price) -> {
            setAndSendPrice(currency, 0, refundSide);
            RedisTrade.getInstance().getIntegrationManager().getCurrencyHook(currency)
              .depositPlayer(side.getTraderUUID(), price, "Trade cancellation");
            RedisTrade.debug(uuid + " Refunded " + price + " " + currency + " to " + side.getTraderUUID());
        });
    }

    /**
     * Set the status of the trade
     *
     * @param status    The status to set
     * @param actorSide The side of the trade to set the status for
     */
    public void setStatus(StatusActor status, Actor actorSide) {
        getTradeSide(actorSide).setStatus(status.getStatus());
        //Notify the confirm button on the opposite side and the current side
        getTradeSide(actorSide.opposite()).notifyOppositeStatus();

        RedisTrade.debug(uuid + " Setting status to " + status.getStatus().name() + " for " + actorSide.name());

        switch (status.getStatus()) {
            case REFUSED, CONFIRMED -> confirmPhase();
            case COMPLETED -> {
                if (Settings.instance().deliverReceipt) {
                    getTradeSide(actorSide).getSidePerspective().setIngredient('r', new ReceiptButton(this));
                }
            }
            //If you receive a remote retrieved status, try to finish and delete the trade
            case RETRIEVED -> RedisTrade.getInstance().getTradeManager().finishTrade(uuid, status.getViewerActor());
        }

    }

    /**
     * Set the status of the trade and send the update to the database/cache
     *
     * @param newStatus      The status to set
     * @param previousStatus The previous status
     * @param viewerSide     The side of the trade to set the status for
     * @return The new status
     */
    public CompletionStage<Status> changeAndSendStatus(StatusActor newStatus, Status previousStatus, Actor viewerSide) {
        if (newStatus.getStatus() == previousStatus) return CompletableFuture.completedFuture(newStatus.getStatus());
        final ViewerUpdate updateType = ViewerUpdate.valueOf(viewerSide, UpdateType.STATUS);
        return RedisTrade.getInstance().getDataCache().updateTrade(uuid, updateType, newStatus.toChar())
          .thenApply(aLong -> {
              if (aLong != -1) {
                  setStatus(newStatus, viewerSide);
                  return newStatus.getStatus();
              } else {
                  //If the update failed, revert the status without triggering the completion timer
                  getTradeSide(viewerSide).setStatus(previousStatus);
                  getTradeSide(viewerSide.opposite()).notifyOppositeStatus();
                  return previousStatus;
              }
          });
    }

    public void sendItemUpdate(int slot, ItemStack item, Actor tradeSide) {
        final ViewerUpdate updateType = ViewerUpdate.valueOf(tradeSide, UpdateType.ITEM);
        RedisTrade.getInstance().getDataCache().updateTrade(uuid, updateType, (char) slot + new String(Utils.serialize(item), StandardCharsets.ISO_8859_1));
        RedisTrade.debug(uuid + " Sending item" + slot + ": " + item + " from " + tradeSide.name());
    }

    public void receiveItemUpdate(int slot, ItemStack item, Actor tradeSide) {
        getTradeSide(tradeSide).getOrder().getVirtualInventory().setItemSilently(slot, item);
        retrievedPhase(tradeSide, tradeSide.opposite());
        RedisTrade.debug(uuid + " Receiving item" + slot + ": " + item + " from " + tradeSide.name());
    }

    /**
     * Called when a status is changed
     * Starts the completion timer if both are confirmed
     * Terminates the completion timer if one is refused
     */
    public void confirmPhase() {
        if (traderSide.getOrder().getStatus() == Status.CONFIRMED && customerSide.getOrder().getStatus() == Status.CONFIRMED) {
            if (this.completionTimer == null || this.completionTimer.isCancelled()) {
                this.completionTimer = new CompletionTimer(this);
                this.completionTimer.runTask(RedisTrade.getInstance(), traderSide.getTraderUUID(), customerSide.getTraderUUID());
            }
        } else if (this.completionTimer != null && !this.completionTimer.isCancelled()) {
            this.completionTimer.cancel();
        }
    }

    /**
     * This phase is called when the CompletionTimer is finished
     * It switches the sides of trader and target
     */
    public void completePhase() {
        this.completionTimer.cancel();
        final BiConsumer<Status, Status> finallyConsumer = (status1, status2) -> {
            if (status1 != Status.COMPLETED || status2 != Status.COMPLETED) return;

            //Apply the economy changes only if the current server is the owner of the trade
            //Owner means the last server that modified the trade
            if (RedisTrade.getInstance().getTradeManager().isOwner(uuid))
                for (CurrencyHook currencyHook : RedisTrade.getInstance().getIntegrationManager().getCurrencyHooks()) {
                    double traderCurrencyPrice = customerSide.getOrder().getPrice(currencyHook.getName());
                    if (traderCurrencyPrice > 0) {
                        currencyHook.depositPlayer(traderSide.getTraderUUID(), traderCurrencyPrice, "Trade completion");
                        RedisTrade.debug(uuid + " Depositing trader " + traderCurrencyPrice + " " + currencyHook.getName() + " to " + traderSide.getTraderName());
                    }

                    double customerCurrencyPrice = traderSide.getOrder().getPrice(currencyHook.getName());
                    if (customerCurrencyPrice > 0) {
                        currencyHook.depositPlayer(customerSide.getTraderUUID(), customerCurrencyPrice, "Trade completion");
                        RedisTrade.debug(uuid + " Depositing customer " + customerCurrencyPrice + " " + currencyHook.getName() + " to " + customerSide.getTraderName());
                    }
                }

            retrievedPhase(Actor.TRADER, Actor.CUSTOMER);
            retrievedPhase(Actor.CUSTOMER, Actor.TRADER);
            //Change cancel trade to get all items
            traderSide.getSidePerspective().notifyItem('D');
            customerSide.getSidePerspective().notifyItem('D');
            //Show the review trade button
            traderSide.getSidePerspective().notifyItem('W');
            customerSide.getSidePerspective().notifyItem('W');

            //Archive the completed trade
            if (RedisTrade.getInstance().getDataStorage() instanceof Database database) {
                database.archiveTrade(this);
            }

        };
        //Check if both sides are confirmed

        changeAndSendStatus(StatusActor.valueOf(Actor.TRADER, Status.COMPLETED), traderSide.getOrder().getStatus(), Actor.TRADER)
          .thenAcceptBothAsync(
            changeAndSendStatus(StatusActor.valueOf(Actor.CUSTOMER, Status.COMPLETED), customerSide.getOrder().getStatus(), Actor.CUSTOMER),
            finallyConsumer);
    }

    /**
     * Check if an inventory is empty and the status is completed
     * Then it resets the trade for the player and locks the whole GUI
     *
     * @param tradeSide    The side of the player
     * @param whoIsEditing The side of the player that is editing
     * @return If the phase was successful
     */
    public CompletionStage<Status> retrievedPhase(Actor tradeSide, Actor whoIsEditing) {
        final TradeSide operatingSide = getTradeSide(tradeSide);

        if (operatingSide.getOrder().getVirtualInventory().isEmpty()) {
            final StatusActor statusActor = StatusActor.valueOf(whoIsEditing, Status.RETRIEVED);
            if (operatingSide.getOrder().getStatus() == Status.REFUSED && tradeSide.isSideOf(whoIsEditing)) {
                RedisTrade.debug(uuid + " Cancelled trade " + tradeSide.name() + " retrieved");
                return changeAndSendStatus(statusActor, operatingSide.getOrder().getStatus(), tradeSide);
            } else if (operatingSide.getOrder().getStatus() == Status.COMPLETED) {
                RedisTrade.debug(uuid + " Completed trade " + tradeSide.name() + " retrieved");
                //If the status is completed, set the status to retrieved
                return changeAndSendStatus(statusActor, operatingSide.getOrder().getStatus(), tradeSide);
            }
        }
        return CompletableFuture.completedFuture(operatingSide.getOrder().getStatus());
    }

    public byte[] serialize() {
        RedisTrade.debug("Serializing " + uuid + " trader side: " + traderSide);
        RedisTrade.debug("Serializing " + uuid + " customer side: " + customerSide);

        byte[] traderSide = this.traderSide.serialize();
        byte[] customerSide = this.customerSide.serialize();

        //Allocate bytes for TradeUUID, TraderSideSize, OtherSideSize, TraderSide, OtherSide
        ByteBuffer bb = ByteBuffer.allocate(16 + 4 + 4 + traderSide.length + customerSide.length);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());

        bb.putInt(traderSide.length);
        bb.putInt(customerSide.length);

        bb.put(traderSide);
        bb.put(customerSide);

        return bb.array();
    }

    public static NewTrade deserialize(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        UUID uuid = new UUID(bb.getLong(), bb.getLong());
        int traderSideSize = bb.getInt();
        int customerSideSize = bb.getInt();

        byte[] traderSideData = new byte[traderSideSize];
        byte[] customerSideData = new byte[customerSideSize];
        bb.get(traderSideData);
        bb.get(customerSideData);
        final TradeSide traderSide = TradeSide.deserialize(traderSideData);
        final TradeSide customerSide = TradeSide.deserialize(customerSideData);

        RedisTrade.debug("Deserializing " + uuid + " trader side: " + traderSide);
        RedisTrade.debug("Deserializing " + uuid + " customer side: " + customerSide);
        return new NewTrade(uuid, traderSide, customerSide);
    }

}