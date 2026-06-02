package com.smartsleep.command;

import com.smartsleep.SmartSleepPlugin;
import com.smartsleep.config.PluginConfig;
import com.smartsleep.model.GlobalStats;
import com.smartsleep.model.PlayerStats;
import com.smartsleep.gui.GuiManager;
import com.smartsleep.storage.StatsStorage;
import com.smartsleep.util.MessageService;
import com.smartsleep.vote.VoteManager;
import com.smartsleep.vote.VoteType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SmartSleepCommand implements TabExecutor {
    private final SmartSleepPlugin plugin;
    private final VoteManager voteManager;
    private final StatsStorage storage;
    private final PluginConfig config;
    private final MessageService messages;
    private final GuiManager guiManager;

    public SmartSleepCommand(SmartSleepPlugin plugin, VoteManager voteManager, StatsStorage storage, PluginConfig config, MessageService messages, GuiManager guiManager) {
        this.plugin = plugin;
        this.voteManager = voteManager;
        this.storage = storage;
        this.config = config;
        this.messages = messages;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                guiManager.openMainMenu(player, storage, voteManager);
            } else {
                help(sender);
            }
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> reload(sender);
            case "start" -> start(sender, args);
            case "stop" -> stop(sender);
            case "stats" -> stats(sender);
            case "top" -> top(sender, args);
            case "version" -> messages.send(sender, "version", Map.of("version", plugin.getPluginMeta().getVersion()));
            default -> help(sender);
        }
        return true;
    }

    private void reload(CommandSender sender) {
        if (!has(sender, "admin")) {
            return;
        }
        plugin.reloadPlugin();
        messages.send(sender, "reload");
    }

    private void start(CommandSender sender, String[] args) {
        if (!has(sender, "start")) {
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can start a vote from console without a world argument.");
            return;
        }
        VoteType type = args.length > 1 && args[1].equalsIgnoreCase("rain") ? VoteType.RAIN : VoteType.NIGHT;
        voteManager.start(player, type);
    }

    private void stop(CommandSender sender) {
        if (!has(sender, "admin")) {
            return;
        }
        World world = sender instanceof Player player ? player.getWorld() : Bukkit.getWorlds().getFirst();
        if (!voteManager.stop(world, true)) {
            messages.send(sender, "vote-not-running");
        }
    }

    private void stats(CommandSender sender) {
        if (!has(sender, "stats")) {
            return;
        }
        GlobalStats global = storage.global();
        sender.sendMessage(messages.raw(plugin.getConfig().getString("messages.stats-header", "SmartSleep Statistics"), Map.of()));
        sender.sendMessage("Nights skipped: " + global.nightsSkipped());
        sender.sendMessage("Rain cleared: " + global.rainSkipped());
        sender.sendMessage("Successful votes: " + global.successfulVotes());
        sender.sendMessage("Failed votes: " + global.failedVotes());
        if (sender instanceof Player player) {
            PlayerStats stats = storage.player(player.getUniqueId(), player.getName());
            sender.sendMessage("Your starts: " + stats.voteStarts() + " | successful starts: " + stats.successfulStarts());
            sender.sendMessage("Your YES/NO votes: " + stats.yesVotes() + "/" + stats.noVotes() + " | participation: " + stats.participation());
        }
    }

    private void top(CommandSender sender, String[] args) {
        if (!has(sender, "top")) {
            return;
        }
        String field = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "participation";
        sender.sendMessage("Top SmartSleep players by " + field + ":");
        int index = 1;
        for (PlayerStats stats : storage.top(field, 10)) {
            int value = switch (field) {
                case "starts", "vote-starts" -> stats.voteStarts();
                case "successful", "successful-starts" -> stats.successfulStarts();
                case "yes", "yes-votes" -> stats.yesVotes();
                case "no", "no-votes" -> stats.noVotes();
                default -> stats.participation();
            };
            sender.sendMessage(index + ". " + stats.name() + " - " + value);
            index++;
        }
    }

    private boolean has(CommandSender sender, String node) {
        if (sender.hasPermission(config.permission(node)) || sender.hasPermission(config.permission("admin"))) {
            return true;
        }
        messages.send(sender, "no-permission");
        return false;
    }

    private void help(CommandSender sender) {
        sender.sendMessage("/smartsleep reload");
        sender.sendMessage("/smartsleep start [night|rain]");
        sender.sendMessage("/smartsleep stop");
        sender.sendMessage("/smartsleep stats");
        sender.sendMessage("/smartsleep top [participation|vote-starts|successful-starts|yes-votes|no-votes]");
        sender.sendMessage("/smartsleep version");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(List.of("reload", "start", "stop", "stats", "top", "version"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return filter(List.of("night", "rain"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            return filter(List.of("participation", "vote-starts", "successful-starts", "yes-votes", "no-votes"), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String input) {
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.startsWith(input.toLowerCase(Locale.ROOT))) {
                result.add(value);
            }
        }
        return result;
    }
}
