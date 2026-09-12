package net.thatmaidenjaden.gleam.config;

import folk.sisby.kaleido.api.KaleidoConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.Config;
import folk.sisby.kaleido.lib.quiltconfig.api.Constraint;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.values.TrackedValue;
import folk.sisby.kaleido.lib.quiltconfig.impl.builders.ConfigBuilderImpl;
import net.fabricmc.loader.api.FabricLoader;

public class GleamConfigImpl {
    public static Config CONFIG;

    public static final TrackedValue<Boolean> ENABLE_COLORED_LIGHTS = TrackedValue.create(true, "enableColoredLights",
            builder -> builder.metadata(Comment.TYPE, comments ->comments.add("Toggle colored lights")));
    public static final TrackedValue<Boolean> ENABLE_UV_BLACKLIGHTS = TrackedValue.create(true, "enableUvBlacklights",
            builder -> builder.metadata(Comment.TYPE, comments -> comments.add("Toggle UV blacklight emission")));
    public static final TrackedValue<Double> GLOBAL_LIGHT_INTENSITY = TrackedValue.create(1.0, "globalLightIntensity",
            builder -> builder.metadata(Comment.TYPE, comments -> comments.add("Global multiplier applied to every colored light's intensity")));
    public static final TrackedValue<Double> GLOBAL_LIGHT_SATURATION = TrackedValue.create(1.0, "globalLightSaturation",
            builder -> builder.constraint(Constraint.range(0.0, 2.0)).metadata(Comment.TYPE, comments -> comments.add("Global saturation multiplier applied to every colored light's color")));
    public static final TrackedValue<Double> LIGHT_GATHERING_DISTANCE = TrackedValue.create(128.0, "lightGatheringDistance",
            builder -> builder.constraint(Constraint.range(16.0, 512.0)).metadata(Comment.TYPE, comments -> comments.add("Maximum block distance at which light-emitting sections are gathered")));
    public static final TrackedValue<Double> LIGHT_RENDER_DISTANCE = TrackedValue.create(192.0, "lightRenderDistance",
            builder -> builder.constraint(Constraint.range(16.0, 512.0)).metadata(Comment.TYPE, comments -> comments.add("Maximum block distance at which gathered lights are uploaded and rendered")));
    public static final TrackedValue<Boolean> DIM_FARTHER_LIGHTS = TrackedValue.create(true, "dimFartherLights",
            builder -> builder.metadata(Comment.TYPE, comments -> comments.add("Apply distance-based brightness falloff to lights near the render distance limit")));
    public static final TrackedValue<Double> ANCHOR_DRIFT_DISTANCE = TrackedValue.create(256.0, "anchorDriftDistance",
            builder -> builder.constraint(Constraint.range(16.0, 4096.0)).metadata(Comment.TYPE, comments -> comments.add("Maximum block distance the camera may drift from the light anchor before lights are re-uploaded")));

    public static void init() {
        ConfigBuilderImpl impl = new ConfigBuilderImpl(KaleidoConfig.tomlEnvironment(FabricLoader.getInstance().getConfigDir()), "", "gleam-client", FabricLoader.getInstance().getConfigDir());
        impl.section("coloredLightSettings", coloredLights -> {
            coloredLights.metadata(Comment.TYPE, comments -> comments.add("Colored Light Settings"))
                    .field(ENABLE_COLORED_LIGHTS).field(ENABLE_UV_BLACKLIGHTS).field(GLOBAL_LIGHT_INTENSITY).field(GLOBAL_LIGHT_SATURATION).field(LIGHT_GATHERING_DISTANCE).field(LIGHT_RENDER_DISTANCE).field(DIM_FARTHER_LIGHTS).field(ANCHOR_DRIFT_DISTANCE);
        });
        CONFIG = impl.build();
    }
}