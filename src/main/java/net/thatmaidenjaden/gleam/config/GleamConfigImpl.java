package net.thatmaidenjaden.gleam.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import static net.thatmaidenjaden.gleam.config.GleamConfigs.*;

public class GleamConfigImpl {

    public static ModConfigSpec CLIENT;

    static {
        ModConfigSpec.Builder CLIENT = new ModConfigSpec.Builder();

        CLIENT.comment("Colored Light Settings").push("coloredLightSettings");
        GleamConfigImpl.ColoredLightSettings.init(CLIENT);
        CLIENT.pop();

        GleamConfigImpl.CLIENT = CLIENT.build();
    }

    private static class ColoredLightSettings {

        public static void init(ModConfigSpec.Builder builder) {
            ENABLE_COLORED_LIGHTS = builder
                    .comment("Toggle colored lights")
                    .define("enableColoredLights", true);

            ENABLE_UV_BLACKLIGHTS = builder
                    .comment("Toggle UV blacklight emission")
                    .define("enableUvBlacklights", true);

            GLOBAL_LIGHT_INTENSITY = builder
                    .comment("Global multiplier applied to every colored light's intensity")
                    .defineInRange("globalLightIntensity", 1.0, Double.MIN_VALUE, Double.MAX_VALUE);

            GLOBAL_LIGHT_SATURATION = builder
                    .comment("Global saturation multiplier applied to every colored light's color")
                    .defineInRange("globalLightSaturation", 1.0, 0.0, 2.0);

            LIGHT_GATHERING_DISTANCE = builder
                    .comment("Maximum block distance at which light-emitting sections are gathered")
                    .defineInRange("lightGatheringDistance", 128.0, 16.0, 512.0);

            LIGHT_RENDER_DISTANCE = builder
                    .comment("Maximum block distance at which gathered lights are uploaded and rendered")
                    .defineInRange("lightRenderDistance", 192.0, 16.0, 512.0);

            DIM_FARTHER_LIGHTS = builder
                    .comment("Apply distance-based brightness falloff to lights near the render distance limit")
                    .define("dimFartherLights", true);

            ANCHOR_DRIFT_DISTANCE = builder
                    .comment("Maximum block distance the camera may drift from the light anchor before lights are re-uploaded")
                    .defineInRange("anchorDriftDistance", 256.0, 16.0, 4096.0);
        }
    }
}