package com.smartsleep.config;

import com.smartsleep.vote.VoteType;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PluginConfig {
    private final JavaPlugin plugin;
    private final Map<String, String> permissions = new HashMap<>();
    private final Set<String> multiworldWorlds = new HashSet<>();
    private final Map<String, Boolean> worldEnabled = new HashMap<>();
    private final List<RequirementRule> defaultRequirements = new ArrayList<>();

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        permissions.clear();
        multiworldWorlds.clear();
        worldEnabled.clear();
        defaultRequirements.clear();

        ConfigurationSection permissionSection = plugin.getConfig().getConfigurationSection("settings.permissions");
        if (permissionSection != null) {
            for (String key : permissionSection.getKeys(false)) {
                permissions.put(key, permissionSection.getString(key, "smartsleep." + key));
            }
        }

        multiworldWorlds.addAll(plugin.getConfig().getStringList("multiworld.worlds"));

        ConfigurationSection worlds = plugin.getConfig().getConfigurationSection("vote-requirements.worlds");
        if (worlds != null) {
            for (String world : worlds.getKeys(false)) {
                worldEnabled.put(world, worlds.getBoolean(world + ".enabled", true));
            }
        }

        for (Map<?, ?> map : plugin.getConfig().getMapList("vote-requirements.default")) {
            String range = String.valueOf(map.containsKey("range") ? map.get("range") : "1-500");
            int percent = Integer.parseInt(String.valueOf(map.containsKey("percent") ? map.get("percent") : "50"));
            defaultRequirements.add(RequirementRule.parse(range, percent));
        }
        if (defaultRequirements.isEmpty()) {
            defaultRequirements.add(new RequirementRule(1, Integer.MAX_VALUE, 50));
        }
    }

    public String storageType() {
        return plugin.getConfig().getString("settings.storage", "YAML").toUpperCase(Locale.ROOT);
    }

    public boolean debug() {
        return plugin.getConfig().getBoolean("settings.debug", false);
    }

    public String permission(String key) {
        return permissions.getOrDefault(key, "smartsleep." + key);
    }

    public boolean nightEnabled() {
        return plugin.getConfig().getBoolean("night-vote.enabled", true);
    }

    public boolean rainEnabled() {
        return plugin.getConfig().getBoolean("rain-vote.enabled", true);
    }

    public boolean startOnBedEnter() {
        return plugin.getConfig().getBoolean("night-vote.start-on-bed-enter", true);
    }

    public boolean preventSleepWhileVoting() {
        return plugin.getConfig().getBoolean("night-vote.prevent-sleep-while-voting", true);
    }

    public boolean endImmediately(VoteType type) {
        return plugin.getConfig().getBoolean(path(type) + ".end-immediately-when-threshold-met", true);
    }

    public int duration(World world, VoteType type) {
        String override = "multiworld.per-world." + world.getName() + "." + (type == VoteType.NIGHT ? "night" : "rain") + "-duration-seconds";
        if (plugin.getConfig().isSet(override)) {
            return Math.max(5, plugin.getConfig().getInt(override));
        }
        return Math.max(5, plugin.getConfig().getInt(path(type) + ".duration-seconds", 30));
    }

    public long skipTime() {
        return plugin.getConfig().getLong("night-vote.skip-time", 1000L);
    }

    public int requiredVotes(World world, int eligiblePlayers) {
        int percent = requiredPercent(world, eligiblePlayers);
        return Math.max(1, (int) Math.ceil(eligiblePlayers * (percent / 100.0D)));
    }

    public int requiredPercent(World world, int eligiblePlayers) {
        List<RequirementRule> rules = requirementsForWorld(world);
        for (RequirementRule rule : rules) {
            if (rule.matches(eligiblePlayers)) {
                return rule.percent();
            }
        }
        return rules.isEmpty() ? 50 : rules.getLast().percent();
    }

    private List<RequirementRule> requirementsForWorld(World world) {
        List<Map<?, ?>> maps = plugin.getConfig().getMapList("vote-requirements.worlds." + world.getName() + ".requirements");
        if (maps.isEmpty()) {
            return defaultRequirements;
        }
        List<RequirementRule> rules = new ArrayList<>();
        for (Map<?, ?> map : maps) {
            rules.add(RequirementRule.parse(String.valueOf(map.get("range")), Integer.parseInt(String.valueOf(map.get("percent")))));
        }
        return rules;
    }

    public boolean isWorldAllowed(World world) {
        if (worldEnabled.containsKey(world.getName()) && !worldEnabled.get(world.getName())) {
            return false;
        }
        String mode = plugin.getConfig().getString("multiworld.mode", "BLACKLIST").toUpperCase(Locale.ROOT);
        boolean listed = multiworldWorlds.contains(world.getName());
        return "WHITELIST".equals(mode) ? listed : !listed;
    }

    public boolean guiEnabled() {
        return plugin.getConfig().getBoolean("gui.enabled", true);
    }

    public boolean updateOpenInventories() {
        return plugin.getConfig().getBoolean("gui.update-open-inventories", true);
    }

    public String guiTheme() {
        return plugin.getConfig().getString("gui.theme", "modern");
    }

    public int guiSize() {
        int size = plugin.getConfig().getInt("gui.size", 27);
        return Math.clamp(size, 9, 54);
    }

    public boolean bossBarEnabled() {
        return plugin.getConfig().getBoolean("bossbar.enabled", true);
    }

    public boolean actionBarEnabled() {
        return plugin.getConfig().getBoolean("actionbar.enabled", true);
    }

    public boolean afkEnabled() {
        return plugin.getConfig().getBoolean("afk.enabled", true);
    }

    public boolean afkMetadataHook() {
        return plugin.getConfig().getBoolean("afk.hooks.metadata", true);
    }

    public boolean afkEssentialsHook() {
        return plugin.getConfig().getBoolean("afk.hooks.essentials", true);
    }

    public boolean rewardsEnabled() {
        return plugin.getConfig().getBoolean("rewards.enabled", false);
    }

    public boolean statisticsEnabled() {
        return plugin.getConfig().getBoolean("statistics.enabled", true);
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    private String path(VoteType type) {
        return type == VoteType.NIGHT ? "night-vote" : "rain-vote";
    }

    private record RequirementRule(int min, int max, int percent) {
        static RequirementRule parse(String range, int percent) {
            String[] parts = range.split("-", 2);
            int min = Integer.parseInt(parts[0].trim());
            int max = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : min;
            return new RequirementRule(min, max, Math.clamp(percent, 1, 100));
        }

        boolean matches(int players) {
            return players >= min && players <= max;
        }
    }
}
