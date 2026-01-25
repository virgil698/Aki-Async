package org.virgil.akiasync.language;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.virgil.akiasync.AkiAsyncPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

@NullMarked
public class LanguageManager {

    private final AkiAsyncPlugin plugin;
    private final MiniMessage miniMessage;
    private final File languageFolder;
    private final Map<String, String> messages = new HashMap<>();
    private String currentLanguage = "zh_CN";

    public LanguageManager(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.languageFolder = new File(plugin.getDataFolder(), "language");
    }

    public void initialize() {
        if (!languageFolder.exists()) {
            languageFolder.mkdirs();
        }

        saveDefaultLanguageFile("zh_CN.yml");
        saveDefaultLanguageFile("en_US.yml");

        String configuredLanguage = plugin.getConfigManager().getLanguage();
        loadLanguage(configuredLanguage);
    }

    private void saveDefaultLanguageFile(String fileName) {
        File file = new File(languageFolder, fileName);
        if (!file.exists()) {
            try (InputStream in = plugin.getResource("language/" + fileName)) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                    plugin.getLogger().info("[Language] Created default language file: " + fileName);
                } else {
                    plugin.getLogger().warning("[Language] Default language file not found in resources: " + fileName);
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "[Language] Failed to save language file: " + fileName, e);
            }
        }
    }

    public void loadLanguage(String language) {
        this.currentLanguage = language;
        messages.clear();

        File langFile = new File(languageFolder, language + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("[Language] Language file not found: " + language + ".yml, falling back to zh_CN");
            langFile = new File(languageFolder, "zh_CN.yml");
            this.currentLanguage = "zh_CN";
        }

        if (!langFile.exists()) {
            plugin.getLogger().severe("[Language] No language files available!");
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(langFile);

        try (InputStream defaultStream = plugin.getResource("language/" + currentLanguage + ".yml")) {
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
                );
                config.setDefaults(defaultConfig);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[Language] Failed to load default language config", e);
        }

        loadMessagesRecursive(config, "");

        plugin.getLogger().info("[Language] Loaded language: " + currentLanguage + " (" + messages.size() + " messages)");
    }

    private void loadMessagesRecursive(YamlConfiguration config, String prefix) {
        for (String key : config.getKeys(true)) {
            if (config.isString(key)) {
                String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
                messages.put(key, config.getString(key, ""));
            }
        }
    }

    public void reload() {
        String configuredLanguage = plugin.getConfigManager().getLanguage();
        loadLanguage(configuredLanguage);
    }

    public Component get(String key) {
        String message = messages.getOrDefault(key, key);
        return miniMessage.deserialize(message);
    }

    public Component get(String key, String... placeholders) {
        String message = messages.getOrDefault(key, key);
        
        if (placeholders.length == 0 || placeholders.length % 2 != 0) {
            return miniMessage.deserialize(message);
        }

        TagResolver.Builder builder = TagResolver.builder();
        for (int i = 0; i < placeholders.length; i += 2) {
            String placeholderKey = placeholders[i];
            String placeholderValue = placeholders[i + 1];
            builder.resolver(Placeholder.parsed(placeholderKey, placeholderValue));
        }

        return miniMessage.deserialize(message, builder.build());
    }

    public Component get(String key, Map<String, String> placeholders) {
        String message = messages.getOrDefault(key, key);
        
        if (placeholders.isEmpty()) {
            return miniMessage.deserialize(message);
        }

        TagResolver.Builder builder = TagResolver.builder();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            builder.resolver(Placeholder.parsed(entry.getKey(), entry.getValue()));
        }

        return miniMessage.deserialize(message, builder.build());
    }

    public String getRaw(String key) {
        return messages.getOrDefault(key, key);
    }

    public Component prefix() {
        return get("prefix");
    }

    public Component prefixed(String key) {
        return prefix().append(get(key));
    }

    public Component prefixed(String key, String... placeholders) {
        return prefix().append(get(key, placeholders));
    }

    public Component prefixed(String key, Map<String, String> placeholders) {
        return prefix().append(get(key, placeholders));
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }

    public boolean hasMessage(String key) {
        return messages.containsKey(key);
    }
}
