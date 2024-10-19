package dev.unnm3d.redistrade;

import com.jonahseguin.drink.CommandService;
import com.jonahseguin.drink.Drink;
import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.ConfigurationException;
import de.exlll.configlib.YamlConfigurations;
import dev.unnm3d.redistrade.commands.*;
import dev.unnm3d.redistrade.data.*;
import dev.unnm3d.redistrade.guis.TradeManager;
import dev.unnm3d.redistrade.hooks.EconomyHook;
import dev.unnm3d.redistrade.objects.Order;
import dev.unnm3d.redistrade.objects.Trader;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class RedisTrade extends JavaPlugin {

    @Getter
    private static RedisTrade instance;
    private Settings settings;
    @Getter
    private DataCache dataCache;
    @Getter
    private TradeManager tradeManager;
    @Getter
    private RedisDataManager redisDataManager;
    @Getter
    private PlayerListManager playerListManager;
    @Getter
    private EconomyHook economyHook;


    @Override
    public void onEnable() {
        instance = this;
        loadYML();

        dataCache = settings.databaseType.equalsIgnoreCase("mysql") ?
                new MySQLDatabase(this, settings.mysql) :
                new SQLiteDatabase(this);
        ((Database) dataCache).connect();
        loadRedis();

        this.playerListManager = new PlayerListManager(this);
        this.economyHook = new EconomyHook(this);
        this.tradeManager = new TradeManager(this);
        loadCommands();

    }

    @Override
    public void onDisable() {
        tradeManager.close();
        playerListManager.stop();
        ((Database) dataCache).destroy();
        redisDataManager.close();
        Drink.unregister(this);
    }

    private void loadRedis() {
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
        this.redisDataManager = new RedisDataManager(
                this,
                RedisClient.create(redisURIBuilder.build()),
                settings.redis.poolSize()
        );
        getLogger().info("Connected to Redis successfully: " + settings.redis.host() + ":" + settings.redis.port());
    }

    public Optional<? extends Player> getPlayer(String name) {
        return getServer().getOnlinePlayers().stream().filter(player -> player.getName().equals(name)).findFirst();
    }

    private void loadCommands() {
        CommandService drink = Drink.get(this);
        drink.bind(PlayerListManager.Target.class).toProvider(new TargetProvider(playerListManager));
        drink.register(new TradeCommand(this), "trade", "t");
        drink.register(new TradeGuiCommand(this), "trade-gui");
        drink.register(new TradeIgnoreCommand(tradeManager), "trade-ignore");
        drink.registerCommands();
    }

    public void loadYML() throws ConfigurationException {
        Path configFile = new File(getDataFolder(), "config.yml").toPath();
        this.settings = Settings.initSettings(configFile);
        Path messagesFile = new File(getDataFolder(), "messages.yml").toPath();
        Messages.initSettings(messagesFile);
    }

    public void saveYML(){
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
