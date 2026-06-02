package com.smartsleep.storage;

import com.smartsleep.config.PluginConfig;
import org.bukkit.plugin.java.JavaPlugin;

public final class StorageFactory {
    private StorageFactory() {
    }

    public static StatsStorage create(JavaPlugin plugin, PluginConfig config) {
        if ("SQLITE".equalsIgnoreCase(config.storageType())) {
            return new SQLiteStatsStorage(plugin);
        }
        return new YamlStatsStorage(plugin);
    }
}
