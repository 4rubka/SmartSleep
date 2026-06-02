package com.smartsleep.gui;

import com.smartsleep.config.PluginConfig;
import com.smartsleep.model.GlobalStats;
import com.smartsleep.model.PlayerStats;
import com.smartsleep.storage.StatsStorage;
import com.smartsleep.util.Items;
import com.smartsleep.util.MessageService;
import com.smartsleep.vote.VoteManager;
import com.smartsleep.vote.VoteSession;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GuiManager {
    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final MessageService messages;

    public GuiManager(JavaPlugin plugin, PluginConfig config, MessageService messages) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
    }

    public void open(Player player, VoteSession session) {
        if (!config.guiEnabled()) {
            return;
        }
        player.openInventory(create(session));
    }

    public void refresh(Player player, VoteSession session) {
        if (!config.guiEnabled() || !config.updateOpenInventories()) {
            return;
        }
        player.openInventory(create(session));
    }

    public void openMainMenu(Player player, StatsStorage storage, VoteManager voteManager) {
        Map<String, String> placeholders = mainMenuPlaceholders(player, storage, voteManager);
        Component title = messages.raw(plugin.getConfig().getString("main-menu.title", "<aqua>SmartSleep</aqua>"), placeholders);
        Inventory inventory = Bukkit.createInventory(new MainMenuHolder(), normalizedSize(plugin.getConfig().getInt("main-menu.size", 54)), title);
        renderMainMenu(inventory, placeholders);
        player.openInventory(inventory);
    }

    public void refreshMainMenu(Player player, StatsStorage storage, VoteManager voteManager) {
        if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof MainMenuHolder)) {
            return;
        }
        Map<String, String> placeholders = mainMenuPlaceholders(player, storage, voteManager);
        renderMainMenu(player.getOpenInventory().getTopInventory(), placeholders);
    }

    public GuiAction action(int slot) {
        String base = themePath();
        if (slot == plugin.getConfig().getInt(base + ".yes.slot", 11)) {
            return GuiAction.YES;
        }
        if (slot == plugin.getConfig().getInt(base + ".no.slot", 15)) {
            return GuiAction.NO;
        }
        if (slot == plugin.getConfig().getInt(base + ".stats.slot", 13)) {
            return GuiAction.STATS;
        }
        return GuiAction.NONE;
    }

    public MainMenuAction mainMenuAction(int slot) {
        if (slot == plugin.getConfig().getInt("main-menu.items.vote-yes.slot", 11)) {
            return MainMenuAction.VOTE_YES;
        }
        if (slot == plugin.getConfig().getInt("main-menu.items.vote-no.slot", 15)) {
            return MainMenuAction.VOTE_NO;
        }
        if (slot == plugin.getConfig().getInt("main-menu.items.start-night.slot", 20)) {
            return MainMenuAction.START_NIGHT;
        }
        if (slot == plugin.getConfig().getInt("main-menu.items.start-rain.slot", 24)) {
            return MainMenuAction.START_RAIN;
        }
        if (slot == plugin.getConfig().getInt("main-menu.items.stats.slot", 30)) {
            return MainMenuAction.STATS;
        }
        if (slot == plugin.getConfig().getInt("main-menu.items.top.slot", 32)) {
            return MainMenuAction.TOP;
        }
        if (slot == plugin.getConfig().getInt("main-menu.items.stop.slot", 40)) {
            return MainMenuAction.STOP_VOTE;
        }
        if (slot == plugin.getConfig().getInt("main-menu.items.reload.slot", 48)) {
            return MainMenuAction.RELOAD;
        }
        if (slot == plugin.getConfig().getInt("main-menu.items.version.slot", 50)) {
            return MainMenuAction.VERSION;
        }
        return MainMenuAction.NONE;
    }

    public void playVoteSound(Player player, boolean yes) {
        String key = yes ? "sounds.vote-yes" : "sounds.vote-no";
        Sound sound = sound(key);
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 0.8F, yes ? 1.4F : 0.8F);
        }
    }

    public void playFinishSound(Player player, boolean success) {
        Sound sound = sound(success ? "sounds.success" : "sounds.fail");
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 0.9F, success ? 1.2F : 0.7F);
        }
    }

    private Inventory create(VoteSession session) {
        Map<String, String> placeholders = session.placeholders();
        Component title = messages.raw(plugin.getConfig().getString("gui.title", "SmartSleep"), placeholders);
        Inventory inventory = Bukkit.createInventory(new SmartSleepHolder(session.world()), config.guiSize(), title);
        fill(inventory, placeholders);
        setItem(inventory, "yes", placeholders);
        setItem(inventory, "stats", placeholders);
        setItem(inventory, "no", placeholders);
        return inventory;
    }

    private void fillMainMenu(Inventory inventory, Map<String, String> placeholders) {
        String base = "main-menu.filler";
        Material material = material(plugin.getConfig().getString(base + ".material", "BLACK_STAINED_GLASS_PANE"));
        Component name = messages.raw(plugin.getConfig().getString(base + ".name", " "), placeholders);
        ItemStack item = Items.create(material, name, List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, item);
        }
        for (int slot : plugin.getConfig().getIntegerList("main-menu.decorative-slots")) {
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, item);
            }
        }
    }

    private void renderMainMenu(Inventory inventory, Map<String, String> placeholders) {
        fillMainMenu(inventory, placeholders);
        setMainMenuItem(inventory, "vote-yes", placeholders);
        setMainMenuItem(inventory, "vote-no", placeholders);
        setMainMenuItem(inventory, "start-night", placeholders);
        setMainMenuItem(inventory, "start-rain", placeholders);
        setMainMenuItem(inventory, "status", placeholders);
        setMainMenuItem(inventory, "stats", placeholders);
        setMainMenuItem(inventory, "top", placeholders);
        setMainMenuItem(inventory, "settings", placeholders);
        setMainMenuItem(inventory, "stop", placeholders);
        setMainMenuItem(inventory, "reload", placeholders);
        setMainMenuItem(inventory, "version", placeholders);
    }

    private void setMainMenuItem(Inventory inventory, String key, Map<String, String> placeholders) {
        String base = "main-menu.items." + key;
        int slot = plugin.getConfig().getInt(base + ".slot", defaultMainSlot(key));
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        Material material = material(plugin.getConfig().getString(base + ".material", defaultMainMaterial(key)));
        Component name = messages.raw(plugin.getConfig().getString(base + ".name", defaultMainName(key)), placeholders);
        List<Component> lore = new ArrayList<>();
        List<String> configuredLore = plugin.getConfig().getStringList(base + ".lore");
        List<String> loreLines = configuredLore.isEmpty() ? defaultMainLore(key) : configuredLore;
        for (String line : loreLines) {
            lore.add(messages.raw(line, placeholders));
        }
        inventory.setItem(slot, Items.create(material, name, lore));
    }

    private Map<String, String> mainMenuPlaceholders(Player player, StatsStorage storage, VoteManager voteManager) {
        GlobalStats global = storage.global();
        PlayerStats stats = storage.player(player.getUniqueId(), player.getName());
        String status = voteManager.session(player.getWorld())
            .map(session -> session.type().display() + " " + session.yesVotes() + "/" + session.requiredVotes() + " - " + session.remainingSeconds() + "s")
            .orElse("No active vote");
        String voteYes = voteManager.session(player.getWorld()).map(session -> String.valueOf(session.yesVotes())).orElse("0");
        String voteNo = voteManager.session(player.getWorld()).map(session -> String.valueOf(session.noVotes())).orElse("0");
        String required = voteManager.session(player.getWorld()).map(session -> String.valueOf(session.requiredVotes())).orElse("0");
        String timer = voteManager.session(player.getWorld()).map(session -> String.valueOf(session.remainingSeconds())).orElse("0");
        Map<String, String> values = new LinkedHashMap<>();
        values.put("player", player.getName());
        values.put("world", player.getWorld().getName());
        values.put("storage", storage.type());
        values.put("status", status);
        values.put("nights", String.valueOf(global.nightsSkipped()));
        values.put("rain", String.valueOf(global.rainSkipped()));
        values.put("successful", String.valueOf(global.successfulVotes()));
        values.put("failed", String.valueOf(global.failedVotes()));
        values.put("participation", String.valueOf(stats.participation()));
        values.put("starts", String.valueOf(stats.voteStarts()));
        values.put("yes_votes", String.valueOf(stats.yesVotes()));
        values.put("no_votes", String.valueOf(stats.noVotes()));
        values.put("vote_yes", voteYes);
        values.put("vote_no", voteNo);
        values.put("required", required);
        values.put("timer", timer);
        return values;
    }

    private void fill(Inventory inventory, Map<String, String> placeholders) {
        String base = themePath() + ".filler";
        Material material = material(plugin.getConfig().getString(base + ".material", "BLACK_STAINED_GLASS_PANE"));
        Component name = messages.raw(plugin.getConfig().getString(base + ".name", " "), placeholders);
        ItemStack item = Items.create(material, name, List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, item);
        }
    }

    private void setItem(Inventory inventory, String key, Map<String, String> placeholders) {
        String base = themePath() + "." + key;
        int slot = plugin.getConfig().getInt(base + ".slot", key.equals("yes") ? 11 : key.equals("no") ? 15 : 13);
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        Material material = material(plugin.getConfig().getString(base + ".material", "STONE"));
        Component name = messages.raw(plugin.getConfig().getString(base + ".name", key), placeholders);
        List<Component> lore = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList(base + ".lore")) {
            lore.add(messages.raw(line, placeholders));
        }
        inventory.setItem(slot, Items.create(material, name, lore));
    }

    private String themePath() {
        return "gui.themes." + config.guiTheme();
    }

    private Material material(String raw) {
        Material material = Material.matchMaterial(raw == null ? "" : raw.toUpperCase(Locale.ROOT));
        return material == null ? Material.STONE : material;
    }

    private int normalizedSize(int requested) {
        int size = Math.max(9, Math.min(54, requested));
        return size % 9 == 0 ? size : ((size / 9) + 1) * 9;
    }

    private int defaultMainSlot(String key) {
        return switch (key) {
            case "start-night" -> 20;
            case "start-rain" -> 24;
            case "vote-yes" -> 11;
            case "vote-no" -> 15;
            case "status" -> 22;
            case "stats" -> 30;
            case "top" -> 32;
            case "settings" -> 4;
            case "stop" -> 40;
            case "reload" -> 48;
            case "version" -> 50;
            default -> -1;
        };
    }

    private String defaultMainMaterial(String key) {
        return switch (key) {
            case "start-night" -> "BLUE_BED";
            case "start-rain" -> "WATER_BUCKET";
            case "vote-yes" -> "LIME_CONCRETE";
            case "vote-no" -> "RED_CONCRETE";
            case "status" -> "COMPASS";
            case "stats" -> "BOOK";
            case "top" -> "GOLD_INGOT";
            case "settings" -> "NETHER_STAR";
            case "stop" -> "BARRIER";
            case "reload" -> "REPEATER";
            case "version" -> "NAME_TAG";
            default -> "STONE";
        };
    }

    private String defaultMainName(String key) {
        return switch (key) {
            case "start-night" -> "<aqua><bold>Start Night Vote</bold>";
            case "start-rain" -> "<blue><bold>Start Rain Vote</bold>";
            case "vote-yes" -> "<green><bold>Vote YES</bold>";
            case "vote-no" -> "<red><bold>Vote NO</bold>";
            case "status" -> "<yellow><bold>Current Vote Status</bold>";
            case "stats" -> "<green><bold>Your Statistics</bold>";
            case "top" -> "<gold><bold>Leaderboards</bold>";
            case "settings" -> "<aqua><bold>SmartSleep Features</bold>";
            case "stop" -> "<red><bold>Stop Current Vote</bold>";
            case "reload" -> "<yellow><bold>Reload Plugin</bold>";
            case "version" -> "<white><bold>Plugin Version</bold>";
            default -> "<gray>SmartSleep";
        };
    }

    private List<String> defaultMainLore(String key) {
        return switch (key) {
            case "start-night" -> List.of("<dark_gray><st>-------------------</st>", "<gray>Open a vote to skip the night.", "<aqua>Click to start.</aqua>", "<dark_gray><st>-------------------</st>");
            case "start-rain" -> List.of("<dark_gray><st>-------------------</st>", "<gray>Open a vote to clear weather.", "<blue>Click to start.</blue>", "<dark_gray><st>-------------------</st>");
            case "vote-yes" -> List.of("<dark_gray><st>-------------------</st>", "<gray>Active vote: <white>{status}</white>", "<gray>YES: <green>{vote_yes}</green><gray>/</gray><white>{required}</white>", "<green>Click to vote YES.</green>", "<dark_gray><st>-------------------</st>");
            case "vote-no" -> List.of("<dark_gray><st>-------------------</st>", "<gray>Active vote: <white>{status}</white>", "<gray>NO votes: <red>{vote_no}</red>", "<red>Click to vote NO.</red>", "<dark_gray><st>-------------------</st>");
            case "status" -> List.of("<dark_gray><st>-------------------</st>", "<gray>Active vote: <white>{status}</white>", "<gray>World: <white>{world}</white>", "<dark_gray><st>-------------------</st>");
            case "stats" -> List.of("<dark_gray><st>-------------------</st>", "<gray>Participation: <green>{participation}</green>", "<gray>YES/NO: <green>{yes_votes}</green><gray>/</gray><red>{no_votes}</red>", "<green>Click to view.</green>", "<dark_gray><st>-------------------</st>");
            case "top" -> List.of("<dark_gray><st>-------------------</st>", "<gray>Successful votes: <green>{successful}</green>", "<gold>Click to view top voters.</gold>", "<dark_gray><st>-------------------</st>");
            case "settings" -> List.of("<dark_gray><st>-------------------</st>", "<gray>GUI, BossBar, ActionBar, rewards", "<gray>Stats, placeholders and multiworld", "<dark_gray><st>-------------------</st>");
            case "stop" -> List.of("<dark_gray><st>-------------------</st>", "<red>Admin action.</red>", "<gray>Stops the current world vote.", "<dark_gray><st>-------------------</st>");
            case "reload" -> List.of("<dark_gray><st>-------------------</st>", "<yellow>Admin action.</yellow>", "<gray>Reloads config and menus.", "<dark_gray><st>-------------------</st>");
            case "version" -> List.of("<dark_gray><st>-------------------</st>", "<gray>Shows installed version.", "<dark_gray><st>-------------------</st>");
            default -> List.of();
        };
    }

    private Sound sound(String path) {
        try {
            return Sound.valueOf(plugin.getConfig().getString(path, "").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
