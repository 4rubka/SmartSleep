package com.smartsleep.listener;

import com.smartsleep.config.PluginConfig;
import com.smartsleep.util.MessageService;
import com.smartsleep.vote.VoteManager;
import com.smartsleep.vote.VoteType;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;

public final class BedListener implements Listener {
    private final VoteManager voteManager;
    private final PluginConfig config;
    private final MessageService messages;

    public BedListener(VoteManager voteManager, PluginConfig config, MessageService messages) {
        this.voteManager = voteManager;
        this.config = config;
        this.messages = messages;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (!config.startOnBedEnter() || !config.nightEnabled()) {
            return;
        }
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
            return;
        }
        World world = event.getPlayer().getWorld();
        if (!config.isWorldAllowed(world)) {
            messages.send(event.getPlayer(), "world-disabled");
            return;
        }
        if (config.preventSleepWhileVoting()) {
            event.setCancelled(true);
        }
        voteManager.start(event.getPlayer(), VoteType.NIGHT);
    }
}
