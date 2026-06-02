package com.smartsleep.vote;

import org.bukkit.World;
import org.bukkit.boss.BossBar;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VoteSession {
    private final VoteType type;
    private final World world;
    private final UUID starter;
    private final Set<UUID> eligible;
    private final int requiredVotes;
    private final int requiredPercent;
    private final int durationSeconds;
    private final Map<UUID, VoteChoice> votes = new ConcurrentHashMap<>();
    private BossBar bossBar;
    private int remainingSeconds;

    public VoteSession(VoteType type, World world, UUID starter, Set<UUID> eligible, int requiredVotes, int requiredPercent, int durationSeconds) {
        this.type = type;
        this.world = world;
        this.starter = starter;
        this.eligible = Set.copyOf(eligible);
        this.requiredVotes = requiredVotes;
        this.requiredPercent = requiredPercent;
        this.durationSeconds = durationSeconds;
        this.remainingSeconds = durationSeconds;
    }

    public VoteType type() {
        return type;
    }

    public World world() {
        return world;
    }

    public UUID starter() {
        return starter;
    }

    public Set<UUID> eligible() {
        return eligible;
    }

    public int requiredVotes() {
        return requiredVotes;
    }

    public int requiredPercent() {
        return requiredPercent;
    }

    public int durationSeconds() {
        return durationSeconds;
    }

    public int remainingSeconds() {
        return remainingSeconds;
    }

    public void remainingSeconds(int remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public BossBar bossBar() {
        return bossBar;
    }

    public void bossBar(BossBar bossBar) {
        this.bossBar = bossBar;
    }

    public boolean eligible(UUID uuid) {
        return eligible.contains(uuid);
    }

    public void vote(UUID uuid, VoteChoice choice) {
        votes.put(uuid, choice);
    }

    public int yesVotes() {
        return (int) votes.values().stream().filter(choice -> choice == VoteChoice.YES).count();
    }

    public int noVotes() {
        return (int) votes.values().stream().filter(choice -> choice == VoteChoice.NO).count();
    }

    public boolean hasPassed() {
        return yesVotes() >= requiredVotes;
    }

    public double progress() {
        if (requiredVotes <= 0) {
            return 1.0D;
        }
        return Math.min(1.0D, yesVotes() / (double) requiredVotes);
    }

    public Map<String, String> placeholders() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("type", type.display());
        values.put("yes", String.valueOf(yesVotes()));
        values.put("no", String.valueOf(noVotes()));
        values.put("required", String.valueOf(requiredVotes));
        values.put("required_percent", String.valueOf(requiredPercent));
        values.put("eligible", String.valueOf(eligible.size()));
        values.put("timer", String.valueOf(remainingSeconds));
        values.put("percent", String.valueOf((int) Math.round(progress() * 100.0D)));
        return values;
    }
}
