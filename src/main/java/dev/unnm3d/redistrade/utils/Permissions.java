package dev.unnm3d.redistrade.utils;

import lombok.Getter;

@Getter
public enum Permissions {
    MODIFY_TRADE("redistrade.modify"),
    URE_CURRENCY_PREFIX("redistrade.usecurrency."),

    ;

    private final String permission;

    Permissions(String permission) {
        this.permission = permission;
    }

}
