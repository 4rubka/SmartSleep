package com.smartsleep.hook;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class VaultHook {
    private final Economy economy;

    public VaultHook(JavaPlugin plugin) {
        Economy detected = null;
        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (provider != null) {
                detected = provider.getProvider();
                plugin.getLogger().info("Vault economy hook enabled.");
            }
        }
        economy = detected;
    }

    public boolean enabled() {
        return economy != null;
    }

    public void deposit(Player player, double amount) {
        if (economy != null && amount > 0) {
            economy.depositPlayer(player, amount);
        }
    }
}
