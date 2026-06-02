package com.smartsleep.storage;

import com.smartsleep.model.GlobalStats;
import com.smartsleep.model.PlayerStats;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StatsStorage {
    void initialize();

    String type();

    GlobalStats global();

    PlayerStats player(UUID uuid, String name);

    CompletableFuture<Void> saveAsync();

    List<PlayerStats> top(String field, int limit);

    void close();
}
