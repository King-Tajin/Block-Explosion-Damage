package com.king_tajin.block_explosion_damage.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class ConfigFileHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("block_explosion_damage");
    private static final File CONFIG_FILE = new File(FMLPaths.CONFIGDIR.get().toFile(), "block_explosion_damage.toml");
    private static final File LEGACY_JSON_CONFIG_FILE = new File(FMLPaths.CONFIGDIR.get().toFile(), "block_explosion_damage.json");
    private static final int PROTECTIVE_BLOCKS_PER_LINE = 4;
    private static final String PROTECTIVE_BLOCKS_KEY = "protectiveBlocks";

    public static ConfigData loadConfig() {
        if (CONFIG_FILE.exists()) {
            return loadTomlConfig();
        }

        if (LEGACY_JSON_CONFIG_FILE.exists()) {
            return migrateLegacyJsonConfig();
        }

        ConfigData config = buildDefaultConfig();
        saveConfig(config);
        return config;
    }

    private static ConfigData migrateLegacyJsonConfig() {
        ConfigData config = loadLegacyJsonConfig();
        if (config == null) {
            return buildDefaultConfig();
        }

        saveConfig(config);

        File migratedFile = new File(LEGACY_JSON_CONFIG_FILE.getParentFile(), "block_explosion_damage.json.migrated");
        if (!LEGACY_JSON_CONFIG_FILE.renameTo(migratedFile)) {
            LOGGER.warn("block_explosion_damage: Migrated config to TOML, but could not rename the old JSON file");
        }

        LOGGER.info("block_explosion_damage: Migrated config from JSON to TOML");
        return config;
    }

    private static ConfigData loadLegacyJsonConfig() {
        try (FileReader reader = new FileReader(LEGACY_JSON_CONFIG_FILE)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            ConfigData config = new ConfigData();
            config.defaultHitsMultiplier = json.get("defaultHitsMultiplier").getAsDouble();
            config.damageDecayTime = json.get("damageDecayTime").getAsInt();

            JsonObject customHits = json.getAsJsonObject("customBlockHits");
            for (String key : customHits.keySet()) {
                config.customBlockHits.put(key, customHits.get(key).getAsInt());
            }

            JsonObject protectiveBlocksJson = json.getAsJsonObject("protectiveBlocks");
            for (String key : protectiveBlocksJson.keySet()) {
                if (protectiveBlocksJson.get(key).getAsBoolean()) {
                    config.protectiveBlocks.add(key);
                }
            }

            return config;
        } catch (Exception e) {
            LOGGER.warn("block_explosion_damage: Failed to read legacy JSON config, using defaults without overwriting any files: {}", e.getMessage());
            return null;
        }
    }

    private static ConfigData loadTomlConfig() {
        try (CommentedFileConfig fileConfig = CommentedFileConfig.of(CONFIG_FILE)) {
            fileConfig.load();

            ConfigData config = new ConfigData();
            config.defaultHitsMultiplier = getNumberOrElse(fileConfig, "defaultHitsMultiplier", 6.0).doubleValue();
            config.damageDecayTime = getNumberOrElse(fileConfig, "damageDecayTime", 550).intValue();

            boolean layoutNeedsRewrite = false;

            Object customHitsValue = fileConfig.get("customBlockHits");
            if (customHitsValue instanceof Config customHits) {
                for (Config.Entry entry : customHits.entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof Number number) {
                        config.customBlockHits.put(entry.getKey(), number.intValue());
                    } else if (PROTECTIVE_BLOCKS_KEY.equals(entry.getKey()) && value instanceof List<?> list) {
                        addStrings(config.protectiveBlocks, list);
                        layoutNeedsRewrite = true;
                    } else {
                        LOGGER.warn("block_explosion_damage: Ignoring invalid customBlockHits entry '{}'", entry.getKey());
                    }
                }
            }

            Object protectiveBlocksValue = fileConfig.get(PROTECTIVE_BLOCKS_KEY);
            if (protectiveBlocksValue instanceof List<?> list) {
                addStrings(config.protectiveBlocks, list);
            }

            if (layoutNeedsRewrite) {
                saveConfig(config);
                LOGGER.info("block_explosion_damage: Fixed config layout, protectiveBlocks moved out of [customBlockHits]");
            }

            return config;
        } catch (Exception e) {
            LOGGER.warn("block_explosion_damage: Failed to load config, using defaults without overwriting the file: {}", e.getMessage());
            return buildDefaultConfig();
        }
    }

    private static Number getNumberOrElse(Config config, String key, Number fallback) {
        Object value = config.get(key);
        return value instanceof Number number ? number : fallback;
    }

    private static void addStrings(Set<String> target, List<?> values) {
        for (Object value : values) {
            if (value instanceof String string) {
                target.add(string);
            }
        }
    }

    private static ConfigData buildDefaultConfig() {
        ConfigData config = new ConfigData();
        config.defaultHitsMultiplier = 6.0;
        config.damageDecayTime = 550;

        config.customBlockHits.put("minecraft:glass", 2);
        config.customBlockHits.put("minecraft:obsidian", 18);
        config.customBlockHits.put("minecraft:crying_obsidian", 16);

        config.protectiveBlocks.add("minecraft:barrier");
        config.protectiveBlocks.add("minecraft:bedrock");
        config.protectiveBlocks.add("minecraft:command_block");
        config.protectiveBlocks.add("minecraft:chain_command_block");
        config.protectiveBlocks.add("minecraft:repeating_command_block");
        config.protectiveBlocks.add("minecraft:structure_block");
        config.protectiveBlocks.add("minecraft:jigsaw");

        return config;
    }

    public static void saveConfig(ConfigData config) {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            writer.write(buildTomlText(config));
        } catch (IOException e) {
            LOGGER.warn("block_explosion_damage: Failed to save config: {}", e.getMessage());
        }
    }

    private static String buildTomlText(ConfigData config) {
        StringBuilder toml = new StringBuilder();

        toml.append("# Block Explosion Damage Configuration\n");
        toml.append("# This mod is only needed server-side!\n");
        toml.append("# If server is running while config is changed, use /block_explosion_damage reload\n");
        toml.append("\n");

        toml.append("# Multiplier applied to block hardness to calculate required hits.\n");
        toml.append("# Higher values = blocks need more hits to break\n");
        toml.append("defaultHitsMultiplier = ").append(config.defaultHitsMultiplier).append("\n");
        toml.append("\n");

        toml.append("# Time in ticks before damage heals by 1 hit (20 ticks = 1 second)\n");
        toml.append("# 6000 ticks = 5 minutes\n");
        toml.append("# Can be disabled with /gamerule tnt_block_damage_decay false\n");
        toml.append("damageDecayTime = ").append(config.damageDecayTime).append("\n");
        toml.append("\n");

        toml.append("# Blocks that shield other blocks from explosion damage\n");
        toml.append("# Blocks behind these won't take damage from explosions\n");
        toml.append("# Format: [\"minecraft:block_name\", ...]\n");
        toml.append("protectiveBlocks = [\n");
        appendWrappedStringArray(toml, new TreeSet<>(config.protectiveBlocks));
        toml.append("]\n");
        toml.append("\n");

        toml.append("# Override specific blocks to require exact number of hits\n");
        toml.append("# Format: \"minecraft:block_name\" = number_of_hits\n");
        toml.append("# These override the defaultHitsMultiplier calculation\n");
        toml.append("[customBlockHits]\n");
        for (Map.Entry<String, Integer> entry : new TreeMap<>(config.customBlockHits).entrySet()) {
            toml.append(quoteTomlString(entry.getKey())).append(" = ").append(entry.getValue()).append("\n");
        }

        return toml.toString();
    }

    private static void appendWrappedStringArray(StringBuilder toml, Set<String> values) {
        List<String> quotedValues = new ArrayList<>();
        for (String value : values) {
            quotedValues.add(quoteTomlString(value));
        }

        for (int i = 0; i < quotedValues.size(); i += PROTECTIVE_BLOCKS_PER_LINE) {
            int end = Math.min(i + PROTECTIVE_BLOCKS_PER_LINE, quotedValues.size());
            toml.append("    ")
                    .append(String.join(", ", quotedValues.subList(i, end)))
                    .append(",\n");
        }
    }

    private static String quoteTomlString(String value) {
        String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }

    public static Block getBlockFromString(String blockId) {
        Identifier identifier = Identifier.tryParse(blockId);
        if (identifier == null) {
            LOGGER.warn("block_explosion_damage: Invalid block ID in config: '{}'", blockId);
            return null;
        }
        return BuiltInRegistries.BLOCK.getValue(identifier);
    }

    public static class ConfigData {
        public double defaultHitsMultiplier = 6.0;
        public int damageDecayTime = 550;
        public Map<String, Integer> customBlockHits = new HashMap<>();
        public Set<String> protectiveBlocks = new HashSet<>();
    }
}