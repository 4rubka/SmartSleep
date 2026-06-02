package com.smartsleep.hook;

import com.smartsleep.config.PluginConfig;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

public final class AfkService {
    private final JavaPlugin plugin;
    private final PluginConfig config;

    public AfkService(JavaPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public boolean isAfk(Player player) {
        if (!config.afkEnabled()) {
            return false;
        }
        if (config.afkMetadataHook() && hasAfkMetadata(player)) {
            return true;
        }
        return config.afkEssentialsHook() && isEssentialsAfk(player);
    }

    private boolean hasAfkMetadata(Player player) {
        for (String key : new String[]{"afk", "AFK", "essentials:afk"}) {
            if (!player.hasMetadata(key)) {
                continue;
            }
            for (MetadataValue value : player.getMetadata(key)) {
                if (value.asBoolean()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isEssentialsAfk(Player player) {
        Plugin essentials = plugin.getServer().getPluginManager().getPlugin("Essentials");
        if (essentials == null || !essentials.isEnabled()) {
            return false;
        }
        try {
            Method getUser = essentials.getClass().getMethod("getUser", Player.class);
            Object user = getUser.invoke(essentials, player);
            if (user == null) {
                return false;
            }
            Method isAfk = user.getClass().getMethod("isAfk");
            Object result = isAfk.invoke(user);
            return result instanceof Boolean afk && afk;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
