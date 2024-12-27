package dev.unnm3d.redistrade.integrity;

import lombok.Getter;

import java.util.UUID;

@Getter
public class RedisTradeStorageException extends Exception {
    private final ExceptionSource source;
    private final UUID tradeUUID;

    public RedisTradeStorageException(Throwable cause, ExceptionSource source, UUID tradeUUID) {
        super(cause);
        this.source = source;
        this.tradeUUID = tradeUUID;
    }
    public RedisTradeStorageException(Throwable cause, ExceptionSource source) {
        this(cause, source, null);
    }


    public enum ExceptionSource {
        BACKUP_TRADE,
        RESTORE_TRADE,
        SERIALIZATION,
        ARCHIVE_TRADE,
        UNARCHIVE_TRADE,
        IGNORED_PLAYER,
        PLAYERLIST

    }
}
