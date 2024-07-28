package dev.unnm3d.redistrade;


import de.exlll.configlib.Configuration;

@Configuration
public class Settings {

    public String databaseType = "sqlite";
    public MySQL mysql = new MySQL("localhost", 3306, "org.mariadb.jdbc.Driver",
            "redistrade", "root", "password",
            10, 10, 1800000, 0, 5000);



    public record MySQL(String databaseHost, int databasePort,String driverClass,
                        String databaseName, String databaseUsername, String databasePassword,
                        int maximumPoolSize, int minimumIdle, int maxLifetime,
                        int keepAliveTime, int connectionTimeout) {
    }
}
