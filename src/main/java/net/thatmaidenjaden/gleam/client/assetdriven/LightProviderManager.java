package net.thatmaidenjaden.gleam.client.assetdriven;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.thatmaidenjaden.gleam.Gleam;
import net.thatmaidenjaden.gleam.client.lighting.GleamEmitterRegistry;

import java.util.HashMap;
import java.util.Map;

public class LightProviderManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();

    public LightProviderManager() {
        super(GSON, "gleam/light_providers");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> elements, ResourceManager manager, ProfilerFiller profiler) {
        GleamEmitterRegistry.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : elements.entrySet()) {
            try {
                parseAndRegister(entry.getValue().getAsJsonObject());
            } catch (Exception e) {
                Gleam.LOGGER.warn("Failed to parse light provider {}: {}", entry.getKey(), e.getMessage());
            }
        }
    }

    private void parseAndRegister(JsonObject json) {
        JsonElement emittersElement = json.get("emitters");
        if (emittersElement == null) {
            Gleam.LOGGER.warn("Missing 'emitters' field in light provider");
            return;
        }

        JsonArray emitterArray;
        if (emittersElement.isJsonPrimitive()) {
            emitterArray = new JsonArray();
            emitterArray.add(emittersElement.getAsString());
        } else if (emittersElement.isJsonArray()) {
            emitterArray = emittersElement.getAsJsonArray();
        } else {
            Gleam.LOGGER.warn("'emitters' must be a singleton or array list of multiple entries");
            return;
        }

        JsonObject props = json.getAsJsonObject("light_properties");
        if (props == null) {
            Gleam.LOGGER.warn("Missing 'light_properties' object");
            return;
        }

        String encoding = props.has("encoding") ? props.get("encoding").getAsString() : "rgb";
        boolean blacklight = false;
        float r, g, b;
        try {
            switch (encoding) {
                case "rgb" -> {
                    r = props.get("red").getAsFloat();
                    g = props.get("green").getAsFloat();
                    b = props.get("blue").getAsFloat();
                }
                case "hsl" -> {
                    float h = props.get("hue").getAsFloat() / 255f;
                    float s = props.get("saturation").getAsFloat() / 255f;
                    float l = props.get("lightness").getAsFloat() / 255f;
                    int rgb = hslToRgb(h, s, l);
                    r = ((rgb >> 16) & 0xFF) / 255f;
                    g = ((rgb >> 8) & 0xFF) / 255f;
                    b = (rgb & 0xFF) / 255f;
                }
                case "hex" -> {
                    String hex = props.get("hex").getAsString();
                    if (hex.startsWith("#")) hex = hex.substring(1);
                    int rgb = Integer.parseInt(hex, 16);
                    r = ((rgb >> 16) & 0xFF) / 255f;
                    g = ((rgb >> 8) & 0xFF) / 255f;
                    b = (rgb & 0xFF) / 255f;
                }
                case "blacklight" -> {
                    r = 0.0f;
                    g = 0.0f;
                    b = 0.0f;
                    blacklight = true;
                }
                default -> {
                    Gleam.LOGGER.warn("Unknown encoding '{}', falling back to rgb", encoding);
                    r = 1.0f; g = 1.0f; b = 1.0f;
                }
            }
        } catch (Exception e) {
            Gleam.LOGGER.warn("Failed to parse color properties: {}", e.getMessage());
            return;
        }

        float intensity = props.has("intensity") ? props.get("intensity").getAsFloat() : 1.0f;
        float radius = props.has("radius") ? props.get("radius").getAsFloat() : 8.0f;
        boolean occludeToBlocklight = !props.has("occlude_to_blocklight") || props.get("occlude_to_blocklight").getAsBoolean();

        for (JsonElement element : emitterArray) {
            String raw = element.getAsString();
            String blockId;
            Map<String, String> condition = new HashMap<>();

            int bracketStart = raw.indexOf('[');
            if (bracketStart == -1) {
                blockId = raw.trim();
            } else {
                blockId = raw.substring(0, bracketStart).trim();
                int bracketEnd = raw.indexOf(']', bracketStart);
                if (bracketEnd == -1) {
                    Gleam.LOGGER.warn("Malformed blockstate condition, missing closing bracket: {}", raw);
                    continue;
                }
                String conditionStr = raw.substring(bracketStart + 1, bracketEnd).trim();
                if (!conditionStr.isEmpty()) {
                    String[] parts = conditionStr.split(",");
                    for (String part : parts) {
                        String[] kv = part.split("=");
                        if (kv.length != 2) {
                            Gleam.LOGGER.warn("Invalid condition part: {}", part);
                            continue;
                        }
                        condition.put(kv[0].trim(), kv[1].trim());
                    }
                }
            }

            ResourceLocation id;
            try { id = ResourceLocation.parse(blockId); } catch (Exception e) { continue; }

            GleamEmitterRegistry.registerEmitter(id, condition, r, g, b, radius, intensity, blacklight, occludeToBlocklight);
        }
    }

    private static int hslToRgb(float h, float s, float l) {
        float c = (1 - Math.abs(2 * l - 1)) * s;
        float x = c * (1 - Math.abs((h * 6) % 2 - 1));
        float m = l - c / 2;
        float r, g, b;
        if (h < 1f/6) { r = c; g = x; b = 0; }
        else if (h < 2f/6) { r = x; g = c; b = 0; }
        else if (h < 3f/6) { r = 0; g = c; b = x; }
        else if (h < 4f/6) { r = 0; g = x; b = c; }
        else if (h < 5f/6) { r = x; g = 0; b = c; }
        else { r = c; g = 0; b = x; }
        return ((int)((r + m) * 255) << 16) |
                ((int)((g + m) * 255) << 8) |
                (int)((b + m) * 255);
    }
}