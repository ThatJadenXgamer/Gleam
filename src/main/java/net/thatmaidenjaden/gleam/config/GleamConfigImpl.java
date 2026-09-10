package net.thatmaidenjaden.gleam.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import static net.thatmaidenjaden.gleam.config.GleamConfigs.*;

public class GleamConfigImpl {

    public static ModConfigSpec CLIENT;

    static {
        ModConfigSpec.Builder COMMON = new ModConfigSpec.Builder();

        COMMON.comment("Colored Light Settings").push("coloredLightSettings");
        GleamConfigImpl.ColoredLightSettings.init(COMMON);
        COMMON.pop();
    }

    private static class ColoredLightSettings {

        public static void init(ModConfigSpec.Builder builder) {
            ENABLE_COLORED_LIGHTS = builder
                    .comment("Toggle colored lights")
                    .define("enableColoredLights", true);
        }
    }
}