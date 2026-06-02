package com.smartsleep.storage;

import com.smartsleep.model.GlobalStats;
import com.smartsleep.model.PlayerStats;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class SQLiteStatsStorage implements StatsStorage {
    private final JavaPlugin plugin;
    private final GlobalStats global = new GlobalStats();
    private final Map<UUID, PlayerStats> players = new ConcurrentHashMap<>();
    private Connection connection;

    public SQLiteStatsStorage(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create data folder.");
            }
            File database = new File(plugin.getDataFolder(), "stats.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS global_stats (id INTEGER PRIMARY KEY CHECK (id = 1), nights_skipped INTEGER, rain_skipped INTEGER, successful_votes INTEGER, failed_votes INTEGER)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_stats (uuid TEXT PRIMARY KEY, name TEXT, vote_starts INTEGER, successful_starts INTEGER, yes_votes INTEGER, no_votes INTEGER, participation INTEGER)");
                statement.executeUpdate("INSERT OR IGNORE INTO global_stats (id, nights_skipped, rain_skipped, successful_votes, failed_votes) VALUES (1,0,0,0,0)");
            }
            load();
        } catch (SQLException exception) {
            plugin.getLogger().severe("Could not initialize SQLite storage: " + exception.getMessage());
        }
    }

    private void load() throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT * FROM global_stats WHERE id = 1")) {
            if (result.next()) {
                global.nightsSkipped(result.getInt("nights_skipped"));
                global.rainSkipped(result.getInt("rain_skipped"));
                global.successfulVotes(result.getInt("successful_votes"));
                global.failedVotes(result.getInt("failed_votes"));
            }
        }
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT * FROM player_stats")) {
            while (result.next()) {
                UUID uuid = UUID.fromString(result.getString("uuid"));
                PlayerStats stats = new PlayerStats(uuid, result.getString("name"));
                stats.voteStarts(result.getInt("vote_starts"));
                stats.successfulStarts(result.getInt("successful_starts"));
                stats.yesVotes(result.getInt("yes_votes"));
                stats.noVotes(result.getInt("no_votes"));
                stats.participation(result.getInt("participation"));
                players.put(uuid, stats);
            }
        }
    }

    @Override
    public String type() {
        return "SQLITE";
    }

    @Override
    public GlobalStats global() {
        return global;
    }

    @Override
    public PlayerStats player(UUID uuid, String name) {
        PlayerStats stats = players.computeIfAbsent(uuid, id -> new PlayerStats(id, name));
        stats.name(name);
        return stats;
    }

    @Override
    public CompletableFuture<Void> saveAsync() {
        GlobalStats globalSnapshot = global;
        List<PlayerStats> playerSnapshot = new ArrayList<>(players.values());
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement globalStatement = connection.prepareStatement("UPDATE global_stats SET nights_skipped=?, rain_skipped=?, successful_votes=?, failed_votes=? WHERE id=1")) {
                globalStatement.setInt(1, globalSnapshot.nightsSkipped());
                globalStatement.setInt(2, globalSnapshot.rainSkipped());
                globalStatement.setInt(3, globalSnapshot.successfulVotes());
                globalStatement.setInt(4, globalSnapshot.failedVotes());
                globalStatement.executeUpdate();
                try (PreparedStatement playerStatement = connection.prepareStatement("INSERT INTO player_stats (uuid,name,vote_starts,successful_starts,yes_votes,no_votes,participation) VALUES (?,?,?,?,?,?,?) ON CONFLICT(uuid) DO UPDATE SET name=excluded.name, vote_starts=excluded.vote_starts, successful_starts=excluded.successful_starts, yes_votes=excluded.yes_votes, no_votes=excluded.no_votes, participation=excluded.participation")) {
                    for (PlayerStats stats : playerSnapshot) {
                        playerStatement.setString(1, stats.uuid().toString());
                        playerStatement.setString(2, stats.name());
                        playerStatement.setInt(3, stats.voteStarts());
                        playerStatement.setInt(4, stats.successfulStarts());
                        playerStatement.setInt(5, stats.yesVotes());
                        playerStatement.setInt(6, stats.noVotes());
                        playerStatement.setInt(7, stats.participation());
                        playerStatement.addBatch();
                    }
                    playerStatement.executeBatch();
                }
            } catch (SQLException exception) {
                plugin.getLogger().warning("Could not save SQLite stats: " + exception.getMessage());
            }
        });
    }

    @Override
    public List<PlayerStats> top(String field, int limit) {
        Comparator<PlayerStats> comparator = switch (field) {
            case "successful-starts" -> Comparator.comparingInt(PlayerStats::successfulStarts);
            case "yes-votes" -> Comparator.comparingInt(PlayerStats::yesVotes);
            case "no-votes" -> Comparator.comparingInt(PlayerStats::noVotes);
            case "participation" -> Comparator.comparingInt(PlayerStats::participation);
            default -> Comparator.comparingInt(PlayerStats::voteStarts);
        };
        return players.values().stream().sorted(comparator.reversed()).limit(limit).toList();
    }

    @Override
    public void close() {
        saveAsync().join();
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException exception) {
                plugin.getLogger().warning("Could not close SQLite connection: " + exception.getMessage());
            }
        }
    }
}
