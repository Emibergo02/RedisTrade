package dev.unnm3d.redistrade.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DatabaseType {
    MYSQL("mysql", "MySQL"),
    MARIADB("mariadb", "MariaDB"),
    SQLITE("sqlite", "SQLite");
    private final String id;
    private final String friendlyName;
}