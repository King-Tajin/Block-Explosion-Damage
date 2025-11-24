package com.king_tajin.block_explosion_damage.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.NotNull;

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
        config.damageDecayTime = 180;

        config.customBlockHits.put("minecraft:glass", 2);
        config.customBlockHits.put("minecraft:obsidian", 12);
        config.customBlockHits.put("minecraft:crying_obsidian", 12);

        config.protectiveBlocks.add("minecraft:barrier");
        config.protectiveBlocks.add("minecraft:bedrock");
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

            writer.write("// Block Explosion Damage Configuration\n");
            writer.write("// \n");
            writer.write("// If server is running while config is changed, use /block_explosion_damage reload\n");
            writer.write("// \n");
            writer.write("// defaultHitsMultiplier: Multiplier applied to block hardness to calculate required hits.\n");
            writer.write("//   - Higher values = blocks need more hits to break\n");
            writer.write("// \n");
            writer.write("// damageDecayTime: Time in ticks before damage heals by 1 hit (20 ticks = 1 second)\n");
            writer.write("//   - 6000 ticks = 5 minutes\n");
            writer.write("//   - Note: Can be disabled with /gamerule tntBlockDamageDecay false\n");
            writer.write("// \n");
            writer.write("// customBlockHits: Override specific blocks to require exact number of hits\n");
            writer.write("//   - Format: \"minecraft:block_name\": number_of_hits\n");
            writer.write("//   - These override the defaultHitsMultiplier calculation\n");
            writer.write("// \n");
            writer.write("// protectiveBlocks: Blocks that shield other blocks from explosion damage\n");
            writer.write("//   - Blocks behind these won't take damage from explosions\n");
            writer.write("//   - Format: \"minecraft:block_name\n");
            writer.write("// \n");
            writer.write("\n");

            JsonObject json = getJsonObject(config);

            GSON.toJson(json, writer);
        } catch (IOException e) {
            System.err.println("Failed to save TNT Multi-Hit config: " + e.getMessage());
        }
    }

    private static @NotNull JsonObject getJsonObject(ConfigData config) {
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
        return json;
    }

    public static Block getBlockFromString(String blockId) {
        ResourceLocation resourceLocation = ResourceLocation.tryParse(blockId);
        if (resourceLocation == null) {
            return null;
        }
        return BuiltInRegistries.BLOCK.getValue(resourceLocation);
    }

    public static class ConfigData {
        public double defaultHitsMultiplier = 6.0;
        public int damageDecayTime = 180;
        public Map<String, Integer> customBlockHits = new HashMap<>();
        public Set<String> protectiveBlocks = new HashSet<>();
    }
}