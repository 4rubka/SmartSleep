package com.smartsleep.listener;

import com.smartsleep.SmartSleepPlugin;
import com.smartsleep.gui.GuiAction;
import com.smartsleep.gui.GuiManager;
import com.smartsleep.gui.MainMenuAction;
import com.smartsleep.gui.MainMenuHolder;
import com.smartsleep.gui.SmartSleepHolder;
import com.smartsleep.storage.StatsStorage;
import com.smartsleep.util.MessageService;
import com.smartsleep.vote.VoteChoice;
import com.smartsleep.vote.VoteManager;
import com.smartsleep.vote.VoteType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;

public final class GuiListener implements Listener {
    private final SmartSleepPlugin plugin;
    private final GuiManager guiManager;
    private final VoteManager voteManager;
    private final StatsStorage storage;
    private final MessageService messages;

    public GuiListener(SmartSleepPlugin plugin, GuiManager guiManager, VoteManager voteManager, StatsStorage storage, MessageService messages) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.voteManager = voteManager;
        this.storage = storage;
        this.messages = messages;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SmartSleepHolder holder)) {
            if (event.getInventory().getHolder() instanceof MainMenuHolder) {
                handleMainMenu(event);
            }
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        GuiAction action = guiManager.action(event.getRawSlot());
        if (action == GuiAction.YES) {
            voteManager.vote(player, VoteChoice.YES);
        } else if (action == GuiAction.NO) {
            voteManager.vote(player, VoteChoice.NO);
        } else if (action == GuiAction.STATS) {
            voteManager.session(holder.world()).ifPresent(session -> guiManager.open(player, session));
        }
    }

    private void handleMainMenu(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        MainMenuAction action = guiManager.mainMenuAction(event.getRawSlot());
        switch (action) {
            case VOTE_YES -> voteManager.vote(player, VoteChoice.YES);
            case VOTE_NO -> voteManager.vote(player, VoteChoice.NO);
            case START_NIGHT -> {
                player.closeInventory();
                voteManager.start(player, VoteType.NIGHT);
            }
            case START_RAIN -> {
                player.closeInventory();
                voteManager.start(player, VoteType.RAIN);
            }
            case STOP_VOTE -> {
                player.closeInventory();
                if (!player.hasPermission(plugin.getConfig().getString("settings.permissions.admin", "smartsleep.admin"))) {
                    messages.send(player, "no-permission");
                    return;
                }
                if (!voteManager.stop(player.getWorld(), true)) {
                    messages.send(player, "vote-not-running");
                }
            }
            case STATS -> sendStats(player);
            case TOP -> sendTop(player);
            case RELOAD -> {
                player.closeInventory();
                if (!player.hasPermission(plugin.getConfig().getString("settings.permissions.admin", "smartsleep.admin"))) {
                    messages.send(player, "no-permission");
                    return;
                }
                plugin.reloadPlugin();
                messages.send(player, "reload");
            }
            case VERSION -> messages.send(player, "version", Map.of("version", plugin.getPluginMeta().getVersion()));
            case NONE -> {
            }
        }
    }

    private void sendStats(Player player) {
        player.closeInventory();
        var global = storage.global();
        var stats = storage.player(player.getUniqueId(), player.getName());
        player.sendMessage(messages.raw(plugin.getConfig().getString("messages.stats-header", "<aqua>SmartSleep Statistics</aqua>"), Map.of()));
        player.sendMessage(messages.raw("<dark_gray>- <gray>Nights skipped:</gray> <aqua>{value}</aqua>", Map.of("value", String.valueOf(global.nightsSkipped()))));
        player.sendMessage(messages.raw("<dark_gray>- <gray>Rain cleared:</gray> <blue>{value}</blue>", Map.of("value", String.valueOf(global.rainSkipped()))));
        player.sendMessage(messages.raw("<dark_gray>- <gray>Your participation:</gray> <green>{value}</green>", Map.of("value", String.valueOf(stats.participation()))));
        player.sendMessage(messages.raw("<dark_gray>- <gray>Your YES/NO:</gray> <green>{yes}</green><gray>/</gray><red>{no}</red>", Map.of("yes", String.valueOf(stats.yesVotes()), "no", String.valueOf(stats.noVotes()))));
    }

    private void sendTop(Player player) {
        player.closeInventory();
        player.sendMessage(messages.raw("<dark_gray><st>-----</st> <aqua><bold>SmartSleep Top Voters</bold></aqua> <dark_gray><st>-----</st>", Map.of()));
        int index = 1;
        for (var stats : storage.top("participation", 10)) {
            player.sendMessage(messages.raw("<gray>#{rank}</gray> <white>{name}</white> <dark_gray>-</dark_gray> <green>{value}</green> <gray>votes</gray>", Map.of(
                "rank", String.valueOf(index),
                "name", stats.name(),
                "value", String.valueOf(stats.participation())
            )));
            index++;
        }
    }
}
