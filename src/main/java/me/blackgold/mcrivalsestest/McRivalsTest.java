package me.blackgold.mcrivalsestest;

import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;

public final class McRivalsTest extends JavaPlugin implements Listener {
    private HikariDataSource hikari;
    @Override
    public void onEnable() {
        loadDataBase();
        createCoinsTable();
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void loadDataBase() {
        hikari = new HikariDataSource();
        hikari.setJdbcUrl("jdbc:mysql://204.2.195.99:16759/McRivalsTest");
        hikari.setUsername("UserTest");
        hikari.setPassword("McRivals");
        hikari.addDataSourceProperty("cachePrepStmts",true);
        hikari.addDataSourceProperty("prepStmtCacheSize",250);
        hikari.addDataSourceProperty("prepStmtCacheSqlLimit",2048);
        hikari.addDataSourceProperty("useServerPrepStmts",true);
        hikari.addDataSourceProperty("useLocalSessionState",true);
        hikari.addDataSourceProperty("rewriteBatchedStatements",true);
        hikari.addDataSourceProperty("cacheResultSetMetadata",true);
        hikari.addDataSourceProperty("cacheServerConfiguration",true);
        hikari.addDataSourceProperty("elideSetAutoCommits",true);
        hikari.addDataSourceProperty("maintainTimeStats",false);
    }

    @Override
    public void onDisable() {
        if (hikari != null)
            hikari.close();
    }

    public void createCoinsTable(){
        try(Connection connection = hikari.getConnection();
            Statement statement = connection.createStatement();){
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS Coins(UUID varchar(36), name VARCHAR(16), COINS int, PRIMARY KEY (UUID))");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void InitPlayer(Player player){
        try (Connection connection = hikari.getConnection()){
             PreparedStatement insert = connection.prepareStatement("INSERT IGNORE INTO Coins VALUES(?,?,?) ON DUPLICATE KEY UPDATE name=?");
             insert.setString(1, player.getUniqueId().toString());
             insert.setString(2, player.getName());
             insert.setInt(3, 0);
             insert.setString(4, player.getName());
             insert.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void UpdatePlayer(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                try (Connection connection = hikari.getConnection();
                     PreparedStatement statement = connection.prepareStatement("UPDATE Coins SET coins=? WHERE uuid=?")){
                    statement.setInt(1, getCoins(player) + 5);
                    statement.setString(2, player.getUniqueId().toString());
                    statement.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public int getCoins(Player player){
        try (Connection connection = hikari.getConnection()){
            PreparedStatement select = connection.prepareStatement("SELECT coins FROM Coins WHERE uuid=?");
            select.setString(1, player.getUniqueId().toString());
            ResultSet result = select.executeQuery();
            if (result.next())
                return result.getInt("coins");
            result.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        InitPlayer(player);
    }

    @EventHandler
    public void onPlayerKill(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if(entity.getType() == EntityType.ZOMBIE
                || entity.getType() == EntityType.ZOMBIE_VILLAGER
                || entity.getType() == EntityType.ZOMBIFIED_PIGLIN
                || entity.getType() == EntityType.HUSK) {

            if(entity.getKiller() instanceof Player){
                event.getDrops().clear();

                Player player = (Player) entity.getKiller();
                UpdatePlayer(player);
                int coins = getCoins(player) + 5;
                player.sendMessage("Your coins are: " + coins);
            }
        }
    }
}
