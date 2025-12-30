package dev.unnm3d.redistrade.api;

import java.util.Date;

public record ArchivedTrade(Date archivedAt, INewTrade archivedTrade) {
}
