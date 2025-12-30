package dev.unnm3d.redistrade.data;

import dev.unnm3d.redistrade.RedisTrade;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public interface Database extends IStorageData {

    @NotNull
    default String[] getSchemaStatements(@NotNull String schemaFileName) throws IOException {
        return new String(Objects.requireNonNull(RedisTrade.getInstance().getResource(schemaFileName))
          .readAllBytes(), StandardCharsets.UTF_8).split(";");
    }

    void connect();

    Connection getConnection() throws SQLException;

}
