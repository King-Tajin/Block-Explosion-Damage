package com.king_tajin.block_explosion_damage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.loading.FMLPaths;
import net.minecraft.core.registries.BuiltInRegistries;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConfigFileHandler {

    private static final File CONFIG_FILE = new File(FMLPaths.CONFIGDIR.get().toFile(), "block_explosion_damage.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static ConfigData loadConfig() {
        if (!CONFIG_FILE.exists()) {
            return createDefaultConfig();
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
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
            System.err.println("Failed to load TNT Multi-Hit config, using defaults: " + e.getMessage());
            return createDefaultConfig();
        }
    }

    private static ConfigData createDefaultConfig() {
        ConfigData config = new ConfigData();
        config.defaultHitsMultiplier = 6.0;
        config.damageDecayTime = 2400;

        config.customBlockHits.put("minecraft:glass", 2);
        config.customBlockHits.put("minecraft:obsidian", 12);
        config.customBlockHits.put("minecraft:crying_obsidian", 12);

        config.protectiveBlocks.add("minecraft:barrier");
        config.protectiveBlocks.add("minecraft:command_block");
        config.protectiveBlocks.add("minecraft:chain_command_block");
        config.protectiveBlocks.add("minecraft:repeating_command_block");
        config.protectiveBlocks.add("minecraft:structure_block");
        config.protectiveBlocks.add("minecraft:jigsaw");

        saveConfig(config);
        return config;
    }

    public static void saveConfig(ConfigData config) {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            JsonObject json = new JsonObject();
            json.addProperty("defaultHitsMultiplier", config.defaultHitsMultiplier);
            json.addProperty("damageDecayTime", config.damageDecayTime);

            JsonObject customHits = new JsonObject();
            for (Map.Entry<String, Integer> entry : config.customBlockHits.entrySet()) {
                customHits.addProperty(entry.getKey(), entry.getValue());
            }
            json.add("customBlockHits", customHits);

            JsonObject protectiveBlocksJson = new JsonObject();
            for (String blockId : config.protectiveBlocks) {
                protectiveBlocksJson.addProperty(blockId, true);
            }
            json.add("protectiveBlocks", protectiveBlocksJson);

            GSON.toJson(json, writer);
        } catch (IOException e) {
            System.err.println("Failed to save TNT Multi-Hit config: " + e.getMessage());
        }
    }

    public static Block getBlockFromString(String blockId) {
        ResourceLocation resourceLocation = ResourceLocation.tryParse(blockId);
        if (resourceLocation == null) {
            return null;
        }
        return BuiltInRegistries.BLOCK.get(resourceLocation);
    }

    public static class ConfigData {
        public double defaultHitsMultiplier = 6.0;
        public int damageDecayTime = 2400;
        public Map<String, Integer> customBlockHits = new HashMap<>();
        public Set<String> protectiveBlocks = new HashSet<>();
    }
}