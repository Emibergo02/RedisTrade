package dev.unnm3d.redistrade;

import com.jonahseguin.drink.CommandService;
import com.jonahseguin.drink.Drink;
import de.exlll.configlib.ConfigurationException;
import dev.unnm3d.redistrade.commands.*;
import dev.unnm3d.redistrade.data.*;
import dev.unnm3d.redistrade.guis.TradeManager;
import dev.unnm3d.redistrade.objects.Order;
import dev.unnm3d.redistrade.objects.Trader;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class RedisTrade extends JavaPlugin {

    @Getter
    private static RedisTrade instance;
    @Getter
    private Settings settings;
    @Getter
    private DataCache dataCache;
    @Getter
    private TradeManager tradeManager;
    @Getter
    private RedisDataManager redisDataManager;
    @Getter
    private PlayerListManager playerListManager;


    @Override
    public void onEnable() {
        instance = this;
        loadYML();
        this.playerListManager = new PlayerListManager(this);
        loadCommands();

        dataCache = settings.databaseType.equalsIgnoreCase("mysql") ?
                new MySQLDatabase(this, settings.mysql) :
                new SQLiteDatabase(this);
        ((Database) dataCache).connect();
        loadRedis();
        this.tradeManager = new TradeManager(this);

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
    }

    private void loadCommands() {
        CommandService drink = Drink.get(this);
        drink.bind(PlayerListManager.Target.class).toProvider(new TargetProvider(playerListManager));
        drink.register(new TradeCommand(this), "trade", "t");
        drink.registerCommands();
    }

    public void loadYML() throws ConfigurationException {
        Path configFile = new File(getDataFolder(), "config.yml").toPath();
        this.settings = Settings.initSettings(configFile);
    }

    @Override
    public void onDisable() {
        ((Database) dataCache).destroy();
    }
}
