package dev.unnm3d.redistrade;

import com.jonahseguin.drink.CommandService;
import com.jonahseguin.drink.Drink;
import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.ConfigurationException;
import de.exlll.configlib.YamlConfigurations;
import dev.unnm3d.redistrade.commands.*;
import dev.unnm3d.redistrade.configs.GuiSettings;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.TradeManager;
import dev.unnm3d.redistrade.data.*;
import dev.unnm3d.redistrade.hooks.EconomyHook;
import dev.unnm3d.redistrade.hooks.VaultEconomyHook;
import dev.unnm3d.redistrade.hooks.RedisEconomyHook;
import dev.unnm3d.redistrade.integrity.IntegritySystem;
import dev.unnm3d.redistrade.utils.Metrics;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class RedisTrade extends JavaPlugin {
    @Getter
    private static final int serverId = new Random().nextInt();
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
    @Getter
    private IntegritySystem integritySystem;
    private Metrics metrics;


    @Override
    public void onEnable() {
        instance = this;
        loadYML();

        try {
            dataCache = switch (settings.cacheType) {
                case REDIS -> new RedisDataManager(this, craftRedisClient(),
                        settings.redis.poolSize());
                case MEMORY -> {
                    this.integritySystem = new IntegritySystem(this, null);
                    yield ICacheData.createEmpty();
                }
                case PLUGIN_MESSAGE -> null;
            };
        } catch (RedisConnectionException e) {
            getLogger().severe("Cannot connect to Redis server");
            getLogger().severe("Check your configuration and try again");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        dataStorage = switch (settings.storageType) {
            case MYSQL -> new MySQLDatabase(this, this.settings.mysql);
            case SQLITE -> new SQLiteDatabase(this);
        };
        ((Database) dataStorage).connect();


        this.playerListManager = new PlayerListManager(this);
        if(!loadEconomy()){
            getLogger().severe("Economy not found");
            getLogger().severe("Check your economy plugin and try again");
            this.economyHook = null;
            getServer().getPluginManager().disablePlugin(this);
        }
        this.tradeManager = new TradeManager(this);
        loadCommands();
        //bStats
        this.metrics = new Metrics(this, 23912);
        metrics.addCustomChart(new Metrics.SimplePie("storage_type", () -> this.settings.storageType.name()));
        metrics.addCustomChart(new Metrics.SimplePie("cache_type", () -> this.settings.cacheType.name()));
        metrics.addCustomChart(new Metrics.SimplePie("player_count", () -> {
            int count = getServer().getOnlinePlayers().size();
            return count > 100 ? "100+" : count > 50 ? "50-100" : count > 20 ? "20-50" : count > 10 ? "10-20" : count > 5 ? "5-10" : "less than 5";
        }));
    }

    @Override
    public void onDisable() {
        if (metrics != null)
            metrics.shutdown();
        if (tradeManager != null)
            tradeManager.close();
        if (playerListManager != null)
            playerListManager.stop();
        if (dataStorage != null)
            dataStorage.close();
        if (dataCache != null)
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
        final RedisClient redisClient = RedisClient.create(redisURIBuilder.build());
        this.integritySystem = new IntegritySystem(this, redisClient);
        return redisClient;
    }

    private boolean loadEconomy(){
        if(this.getServer().getPluginManager().isPluginEnabled("RedisEconomy")){
            this.economyHook = new RedisEconomyHook(this);
            return true;
        }
        try {
            this.economyHook = new VaultEconomyHook(this);
        } catch (IllegalStateException e) {
            return false;
        }
        return true;
    }

    private void loadCommands() {
        CommandService drink = Drink.get(this);
        drink.bind(PlayerListManager.Target.class).toProvider(new TargetProvider(playerListManager));
        drink.bind(LocalDateTime.class).toProvider(new LocalDateProvider(settings.dateFormat, settings.timeZone));
        drink.register(new TradeCommand(this), "trade", "t");
        drink.register(new TradeGuiCommand(this), "trade-gui");
        drink.register(new TradeIgnoreCommand(tradeManager), "trade-ignore");
        drink.register(new BrowseTradeCommand(this), "browse-trade");
        drink.register(new TradeReloadCommand(), "trade-reload");
        drink.register(new StresserCommand(), "trade-stresser");
        drink.registerCommands();
    }

    public void loadYML() throws ConfigurationException {
        Path configFile = new File(getDataFolder(), "config.yml").toPath();
        this.settings = Settings.initSettings(configFile);
        Path messagesFile = new File(getDataFolder(), "messages.yml").toPath();
        Messages.loadMessages(messagesFile);
        Path guisFile = new File(getDataFolder(), "guis.yml").toPath();
        GuiSettings.loadGuiSettings(guisFile);
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
        Path guisFile = new File(getDataFolder(), "guis.yml").toPath();
        GuiSettings.saveGuiSettings(guisFile);
    }


}
