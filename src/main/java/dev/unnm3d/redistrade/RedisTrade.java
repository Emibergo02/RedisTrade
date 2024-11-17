package dev.unnm3d.redistrade;

import com.jonahseguin.drink.CommandService;
import com.jonahseguin.drink.Drink;
import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.ConfigurationException;
import de.exlll.configlib.YamlConfigurations;
import dev.unnm3d.redistrade.commands.*;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.data.*;
import dev.unnm3d.redistrade.guis.TradeManager;
import dev.unnm3d.redistrade.hooks.EconomyHook;
import dev.unnm3d.redistrade.utils.Metrics;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class RedisTrade extends JavaPlugin {

    @Getter
    private static RedisTrade instance;
    private Settings settings;
    @Getter
    private ICacheData dataCache;
    @Getter
    private IStorageData dataStorage;
    @Getter
    private TradeManager tradeManager;
    @Getter
    private PlayerListManager playerListManager;
    @Getter
    private EconomyHook economyHook;
    private Metrics metrics;


    @Override
    public void onEnable() {
        instance = this;
        loadYML();


        dataCache = switch (settings.cacheType) {
            case REDIS -> new RedisDataManager(this, craftRedisClient(),
                    settings.redis.poolSize());

            case PLUGIN_MESSAGE -> null;
        };
        dataStorage = switch (settings.storageType) {
            case REDIS -> dataCache instanceof RedisDataManager rdm ? rdm :
                    new RedisDataManager(this, craftRedisClient(), this.settings.redis.poolSize());
            case MYSQL -> new MySQLDatabase(this, this.settings.mysql);
            case SQLITE -> new SQLiteDatabase(this);
        };
        dataStorage.connect();


        this.playerListManager = new PlayerListManager(this);
        this.economyHook = new EconomyHook(this);
        this.tradeManager = new TradeManager(this);
        loadCommands();
        //bStats
        this.metrics=new Metrics(this, 23912);
        metrics.addCustomChart(new Metrics.SimplePie("storage_type", () -> this.settings.storageType.name()));
        metrics.addCustomChart(new Metrics.SimplePie("cache_type", () -> this.settings.cacheType.name()));
    }

    @Override
    public void onDisable() {
        metrics.shutdown();
        tradeManager.close();
        playerListManager.stop();
        dataStorage.close();
        dataCache.close();
        Drink.unregister(this);
    }

    private RedisClient craftRedisClient() {
        RedisURI.Builder redisURIBuilder = RedisURI.builder()
                .withHost(settings.redis.host())
                .withPort(settings.redis.port())
                .withDatabase(settings.redis.database())
                .withTimeout(Duration.of(settings.redis.timeout(), TimeUnit.MILLISECONDS.toChronoUnit()))
                .withClientName(settings.redis.clientName());
        redisURIBuilder = settings.redis.password().isEmpty() ?
                redisURIBuilder :
                settings.redis.user().isEmpty() ?
                        redisURIBuilder.withPassword(settings.redis.password().toCharArray()) :
                        redisURIBuilder.withAuthentication(settings.redis.user(), settings.redis.password());

        return RedisClient.create(redisURIBuilder.build());
    }

    public Optional<? extends Player> getPlayer(String name) {
        return getServer().getOnlinePlayers().stream().filter(player -> player.getName().equals(name)).findFirst();
    }

    private void loadCommands() {
        CommandService drink = Drink.get(this);
        drink.bind(PlayerListManager.Target.class).toProvider(new TargetProvider(playerListManager));
        drink.bind(LocalDateTime.class).toProvider(new LocalDateProvider(settings.dateFormat, settings.timeZone));
        drink.register(new TradeCommand(this), "trade", "t");
        drink.register(new TradeGuiCommand(this), "trade-gui");
        drink.register(new TradeIgnoreCommand(tradeManager), "trade-ignore");
        drink.register(new BrowseTradeCommand(this), "browse-trade");
        drink.registerCommands();
    }

    public void loadYML() throws ConfigurationException {
        Path configFile = new File(getDataFolder(), "config.yml").toPath();
        this.settings = Settings.initSettings(configFile);
        Path messagesFile = new File(getDataFolder(), "messages.yml").toPath();
        Messages.initSettings(messagesFile);
    }

    public void saveYML() {
        Path configFile = new File(getDataFolder(), "config.yml").toPath();
        YamlConfigurations.save(configFile, Settings.class, Settings.instance(),
                ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder()
                        .header("RedisChat config")
                        .footer("Authors: Unnm3d")
                        .charset(StandardCharsets.UTF_8)
                        .build()
        );
    }


}
