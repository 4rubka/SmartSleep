package com.smartsleep.hook;

import com.smartsleep.SmartSleepPlugin;
import com.smartsleep.model.PlayerStats;
import com.smartsleep.storage.StatsStorage;
import com.smartsleep.vote.VoteManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PlaceholderHook extends PlaceholderExpansion {
    private final SmartSleepPlugin plugin;
    private final VoteManager voteManager;
    private final StatsStorage storage;

    public PlaceholderHook(SmartSleepPlugin plugin, VoteManager voteManager, StatsStorage storage) {
        this.plugin = plugin;
        this.voteManager = voteManager;
        this.storage = storage;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "smartsleep";
    }

    @Override
    public @NotNull String getAuthor() {
        return "SmartSleep";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        return switch (params.toLowerCase()) {
            case "nights_skipped" -> String.valueOf(storage.global().nightsSkipped());
            case "rain_skipped" -> String.valueOf(storage.global().rainSkipped());
            case "vote_yes" -> String.valueOf(voteManager.yesVotes());
            case "vote_no" -> String.valueOf(voteManager.noVotes());
            case "vote_timer" -> String.valueOf(voteManager.timer());
            case "player_votes" -> player == null ? "0" : String.valueOf(playerStats(player).participation());
            case "top_voter" -> storage.top("participation", 1).stream().findFirst().map(PlayerStats::name).orElse("None");
            default -> null;
        };
    }

    private PlayerStats playerStats(OfflinePlayer player) {
        return storage.player(player.getUniqueId(), player.getName() == null ? "Unknown" : player.getName());
    }
}
