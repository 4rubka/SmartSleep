package com.smartsleep.reward;

import com.smartsleep.config.PluginConfig;
import com.smartsleep.hook.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class RewardService {
    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final VaultHook vault;

    public RewardService(JavaPlugin plugin, PluginConfig config, VaultHook vault) {
        this.plugin = plugin;
        this.config = config;
        this.vault = vault;
    }

    public void participation(Player player) {
        give(player, "rewards.participation");
    }

    public void success(Player player) {
        give(player, "rewards.successful-vote");
    }

    private void give(Player player, String path) {
        if (!config.rewardsEnabled()) {
            return;
        }
        int xp = plugin.getConfig().getInt(path + ".xp", 0);
        if (xp > 0) {
            player.giveExp(xp);
        }
        double money = plugin.getConfig().getDouble(path + ".money", 0.0D);
        if (money > 0) {
            vault.deposit(player, money);
        }
        for (String command : plugin.getConfig().getStringList(path + ".commands")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{player}", player.getName()));
        }
        List<String> items = plugin.getConfig().getStringList(path + ".items");
        for (String raw : items) {
            String[] parts = raw.split(":", 2);
            Material material = Material.matchMaterial(parts[0]);
            if (material == null) {
                continue;
            }
            int amount = parts.length > 1 ? parseAmount(parts[1]) : 1;
            player.getInventory().addItem(new ItemStack(material, amount));
        }
    }

    private int parseAmount(String raw) {
        try {
            return Math.max(1, Integer.parseInt(raw));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }
}
