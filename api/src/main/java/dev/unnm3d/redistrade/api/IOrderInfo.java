package dev.unnm3d.redistrade.api;

import dev.unnm3d.redistrade.api.enums.Status;
import org.jetbrains.annotations.NotNull;


public interface IOrderInfo {

    Status getStatus();

    void setStatus(Status status);

    int getRating();

    void setRating(int rating);

    double getPrice(@NotNull String currency);

    void setPrice(String currency, double price);
}
