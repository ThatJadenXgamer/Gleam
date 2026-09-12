package net.thatmaidenjaden.gleam.config;

import java.util.function.Supplier;

public class GleamConfigs {

    // COLORED LIGHTS
    public static Supplier<Boolean> ENABLE_COLORED_LIGHTS = GleamConfigImpl.ENABLE_COLORED_LIGHTS::value;
    public static Supplier<Boolean> ENABLE_UV_BLACKLIGHTS = GleamConfigImpl.ENABLE_UV_BLACKLIGHTS::value;
    public static Supplier<Double> GLOBAL_LIGHT_INTENSITY = GleamConfigImpl.GLOBAL_LIGHT_INTENSITY::value;
    public static Supplier<Double> GLOBAL_LIGHT_SATURATION = GleamConfigImpl.GLOBAL_LIGHT_SATURATION::value;
    public static Supplier<Double> LIGHT_GATHERING_DISTANCE = GleamConfigImpl.LIGHT_GATHERING_DISTANCE::value;
    public static Supplier<Double> LIGHT_RENDER_DISTANCE = GleamConfigImpl.LIGHT_RENDER_DISTANCE::value;
    public static Supplier<Boolean> DIM_FARTHER_LIGHTS = GleamConfigImpl.DIM_FARTHER_LIGHTS::value;
    public static Supplier<Double> ANCHOR_DRIFT_DISTANCE = GleamConfigImpl.ANCHOR_DRIFT_DISTANCE::value;
}