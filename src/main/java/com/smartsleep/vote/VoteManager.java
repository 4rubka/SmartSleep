package com.smartsleep.vote;

import com.smartsleep.config.PluginConfig;
import com.smartsleep.gui.GuiManager;
import com.smartsleep.hook.AfkService;
import com.smartsleep.model.PlayerStats;
import com.smartsleep.reward.RewardService;
import com.smartsleep.storage.StatsStorage;
import com.smartsleep.util.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VoteManager {
    private final JavaPlugin plugin;
    private PluginConfig config;
    private MessageService messages;
    private StatsStorage storage;
    private AfkService afkService;
    private RewardService rewards;
    private GuiManager gui;
    private final Map<String, VoteSession> sessions = new ConcurrentHashMap<>();
    private BukkitTask task;

    public VoteManager(JavaPlugin plugin, PluginConfig config, MessageService messages, StatsStorage storage, AfkService afkService, RewardService rewards, GuiManager gui) {
        this.plugin = plugin;
        reconfigure(config, messages, storage, afkService, rewards, gui);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void reconfigure(PluginConfig config, MessageService messages, StatsStorage storage, AfkService afkService, RewardService rewards, GuiManager gui) {
        this.config = config;
        this.messages = messages;
        this.storage = storage;
        this.afkService = afkService;
        this.rewards = rewards;
        this.gui = gui;
    }

    public void reload() {
        for (VoteSession session : sessions.values()) {
            updateBossBar(session);
            refreshGui(session);
        }
    }

    public boolean start(Player starter, VoteType type) {
        if (!config.isWorldAllowed(starter.getWorld())) {
            messages.send(starter, "world-disabled");
            return false;
        }
        if (type == VoteType.NIGHT && !config.nightEnabled()) {
            return false;
        }
        if (type == VoteType.RAIN && !config.rainEnabled()) {
            return false;
        }
        if (sessions.containsKey(key(starter.getWorld()))) {
            messages.send(starter, "vote-already-running");
            return false;
        }
        Set<UUID> eligible = eligiblePlayers(starter.getWorld());
        if (eligible.isEmpty()) {
            messages.send(starter, "not-eligible");
            return false;
        }

        int percent = config.requiredPercent(starter.getWorld(), eligible.size());
        int required = config.requiredVotes(starter.getWorld(), eligible.size());
        VoteSession session = new VoteSession(type, starter.getWorld(), starter.getUniqueId(), eligible, required, percent, config.duration(starter.getWorld(), type));
        sessions.put(key(starter.getWorld()), session);

        storage.player(starter.getUniqueId(), starter.getName()).addVoteStart();
        storage.saveAsync();

        createBossBar(session);
        broadcast(starter.getWorld(), "vote-started", placeholders(session, starter));
        for (Player player : starter.getWorld().getPlayers()) {
            if (session.eligible(player.getUniqueId())) {
                gui.openMainMenu(player, storage, this);
            }
        }
        return true;
    }

    public boolean stop(World world, boolean failed) {
        VoteSession session = sessions.remove(key(world));
        if (session == null) {
            return false;
        }
        finish(session, !failed && session.hasPassed());
        return true;
    }

    public Optional<VoteSession> session(World world) {
        return Optional.ofNullable(sessions.get(key(world)));
    }

    public Optional<VoteSession> anySession() {
        return sessions.values().stream().findFirst();
    }

    public void vote(Player player, VoteChoice choice) {
        VoteSession session = sessions.get(key(player.getWorld()));
        if (session == null) {
            messages.send(player, "vote-not-running");
            return;
        }
        if (!session.eligible(player.getUniqueId())) {
            messages.send(player, "not-eligible");
            return;
        }
        session.vote(player.getUniqueId(), choice);

        PlayerStats stats = storage.player(player.getUniqueId(), player.getName());
        stats.addParticipation();
        if (choice == VoteChoice.YES) {
            stats.addYesVote();
        } else {
            stats.addNoVote();
        }
        rewards.participation(player);
        storage.saveAsync();

        gui.playVoteSound(player, choice == VoteChoice.YES);
        broadcast(player.getWorld(), choice == VoteChoice.YES ? "vote-yes" : "vote-no", placeholders(session, player));
        updateVisuals(session);

        if (choice == VoteChoice.YES && config.endImmediately(session.type()) && session.hasPassed()) {
            sessions.remove(key(session.world()));
            finish(session, true);
        }
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
        }
        for (VoteSession session : sessions.values()) {
            removeBossBar(session);
        }
        sessions.clear();
    }

    public int yesVotes() {
        return anySession().map(VoteSession::yesVotes).orElse(0);
    }

    public int noVotes() {
        return anySession().map(VoteSession::noVotes).orElse(0);
    }

    public int timer() {
        return anySession().map(VoteSession::remainingSeconds).orElse(0);
    }

    private void tick() {
        for (VoteSession session : new HashSet<>(sessions.values())) {
            session.remainingSeconds(session.remainingSeconds() - 1);
            updateVisuals(session);
            if (session.remainingSeconds() <= 0) {
                sessions.remove(key(session.world()));
                finish(session, session.hasPassed());
            }
        }
    }

    private void finish(VoteSession session, boolean success) {
        removeBossBar(session);
        if (success) {
            if (session.type() == VoteType.NIGHT) {
                session.world().setTime(config.skipTime());
                session.world().setStorm(false);
                session.world().setThundering(false);
                storage.global().addNightSkipped();
            } else {
                session.world().setStorm(false);
                session.world().setThundering(false);
                storage.global().addRainSkipped();
            }
            storage.global().addSuccessfulVote();
            Player starter = Bukkit.getPlayer(session.starter());
            if (starter != null) {
                storage.player(starter.getUniqueId(), starter.getName()).addSuccessfulStart();
            }
            for (UUID uuid : session.eligible()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    rewards.success(player);
                    gui.playFinishSound(player, true);
                }
            }
            broadcast(session.world(), "vote-success", session.placeholders());
        } else {
            storage.global().addFailedVote();
            for (UUID uuid : session.eligible()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    gui.playFinishSound(player, false);
                }
            }
            broadcast(session.world(), "vote-failed", session.placeholders());
        }
        closeVoteGuis(session.world());
        storage.saveAsync();
    }

    private Set<UUID> eligiblePlayers(World world) {
        Set<UUID> eligible = new HashSet<>();
        String permission = config.permission("vote");
        for (Player player : world.getPlayers()) {
            if (player.hasPermission(permission) && !afkService.isAfk(player)) {
                eligible.add(player.getUniqueId());
            }
        }
        return eligible;
    }

    private void updateVisuals(VoteSession session) {
        updateBossBar(session);
        sendActionBars(session);
        refreshGui(session);
    }

    private void createBossBar(VoteSession session) {
        if (!config.bossBarEnabled()) {
            return;
        }
        BossBar bossBar = Bukkit.createBossBar(bossBarTitle(session), bossBarColor(), bossBarStyle());
        bossBar.setProgress(session.progress());
        session.bossBar(bossBar);
        for (UUID uuid : session.eligible()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                bossBar.addPlayer(player);
            }
        }
    }

    private void updateBossBar(VoteSession session) {
        BossBar bossBar = session.bossBar();
        if (bossBar == null) {
            return;
        }
        bossBar.setTitle(bossBarTitle(session));
        bossBar.setProgress(session.progress());
    }

    private void removeBossBar(VoteSession session) {
        BossBar bossBar = session.bossBar();
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    private String bossBarTitle(VoteSession session) {
        String raw = plugin.getConfig().getString("bossbar.title", "SmartSleep {timer}s");
        return messages.plain(raw, session.placeholders());
    }

    private BarColor bossBarColor() {
        try {
            return BarColor.valueOf(plugin.getConfig().getString("bossbar.color", "BLUE").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return BarColor.BLUE;
        }
    }

    private BarStyle bossBarStyle() {
        try {
            return BarStyle.valueOf(plugin.getConfig().getString("bossbar.style", "SEGMENTED_10").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return BarStyle.SEGMENTED_10;
        }
    }

    private void sendActionBars(VoteSession session) {
        if (!config.actionBarEnabled()) {
            return;
        }
        String raw = plugin.getConfig().getString("actionbar.message", "{yes}/{required}");
        Component component = messages.raw(raw, session.placeholders());
        for (UUID uuid : session.eligible()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendActionBar(component);
            }
        }
    }

    private void refreshGui(VoteSession session) {
        for (UUID uuid : session.eligible()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                gui.refreshMainMenu(player, storage, this);
            }
        }
    }

    private void closeVoteGuis(World world) {
        for (Player player : world.getPlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof com.smartsleep.gui.SmartSleepHolder
                || player.getOpenInventory().getTopInventory().getHolder() instanceof com.smartsleep.gui.MainMenuHolder) {
                player.closeInventory();
            }
        }
    }

    private void broadcast(World world, String messageKey, Map<String, String> placeholders) {
        for (Player player : world.getPlayers()) {
            messages.send(player, messageKey, placeholders);
        }
        plugin.getServer().getConsoleSender().sendMessage(messages.component(messageKey, placeholders));
    }

    private Map<String, String> placeholders(VoteSession session, Player player) {
        Map<String, String> placeholders = new HashMap<>(session.placeholders());
        placeholders.put("player", player.getName());
        return placeholders;
    }

    private String key(World world) {
        return world.getUID().toString();
    }
}
