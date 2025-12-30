package dev.unnm3d.redistrade;

import com.jonahseguin.drink.CommandService;
import com.jonahseguin.drink.Drink;
import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.ConfigurationException;
import de.exlll.configlib.YamlConfigurations;
import dev.unnm3d.redistrade.commands.*;
import dev.unnm3d.redistrade.commands.providers.ItemFieldProvider;
import dev.unnm3d.redistrade.commands.providers.LocalDateProvider;
import dev.unnm3d.redistrade.commands.providers.TargetProvider;
import dev.unnm3d.redistrade.commands.providers.UUIDProvider;
import dev.unnm3d.redistrade.configs.GuiSettings;
import dev.unnm3d.redistrade.configs.Messages;
import dev.unnm3d.redistrade.configs.Settings;
import dev.unnm3d.redistrade.core.PlayerListener;
import dev.unnm3d.redistrade.core.TradeManager;
import dev.unnm3d.redistrade.data.*;
import dev.unnm3d.redistrade.hooks.IntegrationManager;
import dev.unnm3d.redistrade.hooks.restrictions.WorldGuardHook;
import dev.unnm3d.redistrade.integrity.IntegritySystem;
import dev.unnm3d.redistrade.restriction.RestrictionService;
import dev.unnm3d.redistrade.restriction.WorldRestriction;
import dev.unnm3d.redistrade.utils.Metrics;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RedisTrade extends JavaPlugin {
    @Getter
    private static final int serverId = new Random().nextInt();
    private static File debugFile;
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
    private IntegrationManager integrationManager;
    @Getter
    private RestrictionService restrictionService;
    @Getter
    private IntegritySystem integritySystem;
    private Metrics metrics;

    public static void debug(String string) {
        if (Settings.instance().debug) {
            try {
                final FileWriter writer = new FileWriter(debugFile.getAbsoluteFile(), true);
                writer.append("[")
                  .append(String.valueOf(LocalDateTime.now()))
                  .append("] ")
                  .append(string);
                if (Settings.instance().debugStrace && Thread.currentThread().getStackTrace().length > 1) {
                    for (int i = 2; i < Math.min(Thread.currentThread().getStackTrace().length, 7); i++) {
                        writer.append("\n\t").append(Thread.currentThread().getStackTrace()[i].toString());
                    }
                }

                writer.append("\r\n");
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onLoad() {
        instance = this;
        loadDebugFile();
        loadYML();
        this.restrictionService = new RestrictionService(this);
        this.restrictionService.addRestrictionHook(new WorldRestriction());
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            this.restrictionService.addRestrictionHook(new WorldGuardHook());
            getLogger().info("WorldGuard found, hooking into it");
        } else {
            getLogger().info("WorldGuard not found, region support disabled");
        }
    }

    @Override
    public void onEnable() {
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
            case POSTGRESQL -> new PostgresSQLDatabase(this, this.settings.mysql);
            case SQLITE -> new SQLiteDatabase(this);
        };
        ((Database) dataStorage).connect();

        this.playerListManager = new PlayerListManager(this);
        this.integrationManager = new IntegrationManager(this);
        this.tradeManager = new TradeManager(this);
        try {
            loadCommands();
        } catch (Exception e) {
            getLogger().severe("Error loading commands");
            getLogger().severe("Check your configuration and try again");
        }

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

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
          .withSsl(settings.redis.ssl())
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

    private void loadCommands() {
        CommandService drink = Drink.get(this);
        drink.bind(PlayerListManager.Target.class).toProvider(new TargetProvider(playerListManager));
        drink.bind(LocalDateTime.class).toProvider(new LocalDateProvider(settings.dateFormat, settings.timeZone));
        drink.bind(UUID.class).toProvider(new UUIDProvider());
        drink.bind(Field.class).toProvider(new ItemFieldProvider());

        applyAliasesAndRegister(drink, new TradeCommand(this), "trade");
        applyAliasesAndRegister(drink, new IgnoreCommand(tradeManager), "trade-ignore");
        applyAliasesAndRegister(drink, new BrowseTradeCommand(this), "trade-browse");
        applyAliasesAndRegister(drink, new SpectateTradeCommand(this), "trade-spectate");
        applyAliasesAndRegister(drink, new RateCommand(this), "trade-rate");

        drink.register(new TradeAdminCommand(this), "redistrade");
        drink.registerCommands();
    }

    private void applyAliasesAndRegister(CommandService drink, Object commandInstance, String commandName) {
        final List<String> aliases = settings.commandAliases.getOrDefault(commandName, List.of(commandName));
        if (aliases.isEmpty()) {
            getLogger().severe("No command or aliases found for " + commandName);
            getLogger().severe("The command will not be registered");
            return;
        }
        drink.register(commandInstance, aliases.getFirst(), aliases.subList(1, aliases.size()).toArray(new String[0]));
    }

    public void loadYML() throws ConfigurationException {
        Path configFile = new File(getDataFolder(), "config.yml").toPath();
        this.settings = Settings.initSettings(configFile);
        Path messagesFile = new File(getDataFolder(), "messages.yml").toPath();
        Messages.loadMessages(messagesFile);
        Path guisFile = new File(getDataFolder(), "guis.yml").toPath();
        GuiSettings.loadGuiSettings(guisFile);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void loadDebugFile() {
        final File pluginDir = new File(getServer().getPluginsFolder(), "RedisTrade");
        if (!pluginDir.exists()) {
            pluginDir.mkdir();
        }
        final File parentDir = new File(pluginDir, "logs");
        if (!parentDir.exists()) {
            parentDir.mkdir();
        }
        debugFile = new File(parentDir, "debug" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".log");
        if (!debugFile.exists()) {
            try {
                debugFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void saveYML() {
        Path configFile = new File(getDataFolder(), "config.yml").toPath();
        YamlConfigurations.save(configFile, Settings.class, Settings.instance(),
          ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder()
            .header("RedisTrade config")
            .footer("Authors: Unnm3d")
            .charset(StandardCharsets.UTF_8)
            .build()
        );
        Path guisFile = new File(getDataFolder(), "guis.yml").toPath();
        GuiSettings.saveGuiSettings(guisFile);
    }
}
