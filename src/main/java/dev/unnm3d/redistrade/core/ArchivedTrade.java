package dev.unnm3d.redistrade.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;

@AllArgsConstructor
@Getter
public class ArchivedTrade {
    private Date archivedAt;
    private NewTrade trade;
}
