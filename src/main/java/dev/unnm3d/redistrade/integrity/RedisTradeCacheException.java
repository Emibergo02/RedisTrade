package dev.unnm3d.redistrade.integrity;

import lombok.Getter;

import java.util.UUID;

@Getter
public class RedisTradeCacheException extends Exception {
    private final ExceptionSource source;
    private final UUID tradeUUID;

    public RedisTradeCacheException(Throwable cause, ExceptionSource source, UUID tradeUUID) {
        super(cause);
        this.source = source;
        this.tradeUUID = tradeUUID;
    }

    public enum ExceptionSource {
        CACHE_UPDATE_TRADE,
        CACHE_CREATE_TRADE
    }
}
