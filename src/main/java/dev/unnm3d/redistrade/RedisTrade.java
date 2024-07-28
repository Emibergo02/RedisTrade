package dev.unnm3d.redistrade;

import de.exlll.configlib.ConfigurationException;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import dev.unnm3d.redistrade.data.DataCache;
import dev.unnm3d.redistrade.data.Database;
import dev.unnm3d.redistrade.data.MySQLDatabase;
import dev.unnm3d.redistrade.data.SQLiteDatabase;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public final class RedisTrade extends JavaPlugin {

    @Getter
    private static RedisTrade instance;
    @Getter
    private Settings settings;
    @Getter
    private DataCache dataCache;


    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this)
                .usePluginNamespace()
                .silentLogs(true)
                .shouldHookPaperReload(true)
                .verboseOutput(false));
    }
    @Override
    public void onEnable() {
        instance = this;
        loadYML();
        CommandAPI.onEnable();
        dataCache = settings.databaseType.equalsIgnoreCase("mysql") ?
                new MySQLDatabase(this, settings.mysql) :
                new SQLiteDatabase(this);
        ((Database)dataCache).connect();

        new Commands(this).getTradeCommand().register();
        new Commands(this).getTradeListCommand().register();
    }

    public void loadYML() throws ConfigurationException {
        Path configFile = new File(getDataFolder(), "config.yml").toPath();
        this.settings = YamlConfigurations.update(
                configFile,
                Settings.class,
                YamlConfigurationProperties.newBuilder()
                        .header("RedisChat config")
                        .footer("Authors: Unnm3d")
                        .charset(StandardCharsets.UTF_8)
                        .build()
        );
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
