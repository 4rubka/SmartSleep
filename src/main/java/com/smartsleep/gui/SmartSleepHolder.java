package com.smartsleep.gui;

import org.bukkit.World;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class SmartSleepHolder implements InventoryHolder {
    private final World world;

    public SmartSleepHolder(World world) {
        this.world = world;
    }

    public World world() {
        return world;
    }

    @Override
    public @NotNull Inventory getInventory() {
        throw new UnsupportedOperationException("SmartSleepHolder is only used as an inventory marker.");
    }
}
