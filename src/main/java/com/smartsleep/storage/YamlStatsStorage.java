package com.smartsleep.storage;

import com.smartsleep.model.GlobalStats;
import com.smartsleep.model.PlayerStats;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class YamlStatsStorage implements StatsStorage {
    private final JavaPlugin plugin;
    private final File file;
    private final GlobalStats global = new GlobalStats();
    private final Map<UUID, PlayerStats> players = new ConcurrentHashMap<>();

    public YamlStatsStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
    }

    @Override
    public void initialize() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create data folder.");
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        global.nightsSkipped(yaml.getInt("global.nights-skipped", 0));
        global.rainSkipped(yaml.getInt("global.rain-skipped", 0));
        global.successfulVotes(yaml.getInt("global.successful-votes", 0));
        global.failedVotes(yaml.getInt("global.failed-votes", 0));

        ConfigurationSection section = yaml.getConfigurationSection("players");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                PlayerStats stats = new PlayerStats(uuid, section.getString(key + ".name", "Unknown"));
                stats.voteStarts(section.getInt(key + ".vote-starts", 0));
                stats.successfulStarts(section.getInt(key + ".successful-starts", 0));
                stats.yesVotes(section.getInt(key + ".yes-votes", 0));
                stats.noVotes(section.getInt(key + ".no-votes", 0));
                stats.participation(section.getInt(key + ".participation", 0));
                players.put(uuid, stats);
            }
        }
    }

    @Override
    public String type() {
        return "YAML";
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
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("global.nights-skipped", globalSnapshot.nightsSkipped());
            yaml.set("global.rain-skipped", globalSnapshot.rainSkipped());
            yaml.set("global.successful-votes", globalSnapshot.successfulVotes());
            yaml.set("global.failed-votes", globalSnapshot.failedVotes());
            for (PlayerStats stats : playerSnapshot) {
                String base = "players." + stats.uuid();
                yaml.set(base + ".name", stats.name());
                yaml.set(base + ".vote-starts", stats.voteStarts());
                yaml.set(base + ".successful-starts", stats.successfulStarts());
                yaml.set(base + ".yes-votes", stats.yesVotes());
                yaml.set(base + ".no-votes", stats.noVotes());
                yaml.set(base + ".participation", stats.participation());
            }
            try {
                yaml.save(file);
            } catch (IOException exception) {
                plugin.getLogger().warning("Could not save stats.yml: " + exception.getMessage());
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
    }
}
