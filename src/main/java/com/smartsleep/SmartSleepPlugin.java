package com.smartsleep;

import com.smartsleep.command.SmartSleepCommand;
import com.smartsleep.config.PluginConfig;
import com.smartsleep.gui.GuiManager;
import com.smartsleep.hook.AfkService;
import com.smartsleep.hook.PlaceholderHook;
import com.smartsleep.hook.VaultHook;
import com.smartsleep.listener.BedListener;
import com.smartsleep.listener.GuiListener;
import com.smartsleep.reward.RewardService;
import com.smartsleep.storage.StatsStorage;
import com.smartsleep.storage.StorageFactory;
import com.smartsleep.util.MessageService;
import com.smartsleep.vote.VoteManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class SmartSleepPlugin extends JavaPlugin {
    private PluginConfig pluginConfig;
    private MessageService messages;
    private StatsStorage storage;
    private AfkService afkService;
    private VaultHook vaultHook;
    private RewardService rewardService;
    private GuiManager guiManager;
    private VoteManager voteManager;
    private PlaceholderHook placeholderHook;

    @Override
    public void onEnable() {
        syncDefaultConfig();
        loadServices();

        registerListeners();

        registerCommand();

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholderHook = new PlaceholderHook(this, voteManager, storage);
            placeholderHook.register();
            getLogger().info("PlaceholderAPI hook enabled.");
        }

        getLogger().info("SmartSleep enabled.");
    }

    @Override
    public void onDisable() {
        if (voteManager != null) {
            voteManager.shutdown();
        }
        if (placeholderHook != null) {
            placeholderHook.unregister();
        }
        if (storage != null) {
            storage.close();
        }
    }

    public void reloadPlugin() {
        syncDefaultConfig();
        if (voteManager != null) {
            voteManager.reload();
        }
        HandlerList.unregisterAll(this);
        loadServices();
        registerListeners();
        registerCommand();
    }

    private void loadServices() {
        pluginConfig = new PluginConfig(this);
        messages = new MessageService(this, pluginConfig);

        if (storage == null || !storage.type().equalsIgnoreCase(pluginConfig.storageType())) {
            if (storage != null) {
                storage.close();
            }
            storage = StorageFactory.create(this, pluginConfig);
            storage.initialize();
        }

        afkService = new AfkService(this, pluginConfig);
        vaultHook = new VaultHook(this);
        rewardService = new RewardService(this, pluginConfig, vaultHook);
        guiManager = new GuiManager(this, pluginConfig, messages);

        if (voteManager == null) {
            voteManager = new VoteManager(this, pluginConfig, messages, storage, afkService, rewardService, guiManager);
        } else {
            voteManager.reconfigure(pluginConfig, messages, storage, afkService, rewardService, guiManager);
        }
    }

    private void syncDefaultConfig() {
        saveDefaultConfig();
        reloadConfig();

        try (InputStream stream = getResource("config.yml")) {
            if (stream == null) {
                return;
            }
            FileConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
            getConfig().setDefaults(defaults);
            getConfig().options().copyDefaults(true);

            boolean forceModernVisuals = getConfig().getBoolean("settings.force-modern-visuals", true);
            if (forceModernVisuals) {
                for (String path : List.of("gui", "main-menu", "messages", "bossbar", "actionbar")) {
                    getConfig().set(path, defaults.get(path));
                }
            }
            saveConfig();
            reloadConfig();
        } catch (Exception exception) {
            getLogger().warning("Could not sync default config: " + exception.getMessage());
        }
    }

    private void registerCommand() {
        SmartSleepCommand executor = new SmartSleepCommand(this, voteManager, storage, pluginConfig, messages, guiManager);
        PluginCommand command = getCommand("smartsleep");
        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new BedListener(voteManager, pluginConfig, messages), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this, guiManager, voteManager, storage, messages), this);
    }
}
