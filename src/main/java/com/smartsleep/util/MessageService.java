package com.smartsleep.util;

import com.smartsleep.config.PluginConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class MessageService {
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private String prefix;

    public MessageService(JavaPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.prefix = plugin.getConfig().getString("messages.prefix", "");
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(component(key, placeholders));
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }

    public Component component(String key, Map<String, String> placeholders) {
        String raw = plugin.getConfig().getString("messages." + key, key);
        return parse(apply(raw, placeholders));
    }

    public Component raw(String message, Map<String, String> placeholders) {
        return parse(apply(message, placeholders));
    }

    public String plain(String message, Map<String, String> placeholders) {
        return PlainTextComponentSerializer.plainText().serialize(raw(message, placeholders));
    }

    private Component parse(String message) {
        return miniMessage.deserialize(message);
    }

    private String apply(String raw, Map<String, String> placeholders) {
        String value = raw.replace("{prefix}", prefix);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return value;
    }
}
