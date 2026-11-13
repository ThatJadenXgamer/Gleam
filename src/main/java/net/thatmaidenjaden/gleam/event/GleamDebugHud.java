package net.thatmaidenjaden.gleam.event;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.thatmaidenjaden.gleam.Gleam;
import net.thatmaidenjaden.gleam.client.lighting.GleamLightEngine;
import net.thatmaidenjaden.gleam.config.GleamConfigs;

import java.util.List;

@EventBusSubscriber(value = Dist.CLIENT, modid = Gleam.MOD_ID)
public class GleamDebugHud {
    public static String SimplifyBoolean(boolean targetBoolean) {
        return targetBoolean ? "§aEnabled" : "§cDisabled";
    }

    @SubscribeEvent
    public static void onDebugHudRender(CustomizeGuiOverlayEvent.DebugText debugEvent) {
        GleamLightEngine clientEngine = GleamLightEngine.getInstance();
        if (clientEngine == null) return;

        List<String> debugInfoSide = debugEvent.getLeft();
        debugInfoSide.add("");
        debugInfoSide.add("§eGleam Rendering Info:");

        boolean coloredLightingActive = GleamConfigs.ENABLE_COLORED_LIGHTS.get();
        debugInfoSide.add("Colored Lighting: " + SimplifyBoolean(coloredLightingActive));
        if (coloredLightingActive) {
            debugInfoSide.add("Active Lights: §b" + clientEngine.getActiveLights() + "/" + GleamLightEngine.MAX_TOTAL_LIGHTS);
            debugInfoSide.add("Total Light Active Sections: §b" + clientEngine.getActiveSections().size());
            debugInfoSide.add("Anchor Refresh Distance: §b" + GleamConfigs.ANCHOR_DRIFT_DISTANCE.get());
            debugInfoSide.add("Lighting Distance: §b" + GleamConfigs.LIGHT_RENDER_DISTANCE.get());
            debugInfoSide.add("Light Gathering Distance: §b" + GleamConfigs.LIGHT_GATHERING_DISTANCE.get());
            debugInfoSide.add("Lighting Saturation: §b" + GleamConfigs.GLOBAL_LIGHT_SATURATION.get());
            debugInfoSide.add("Lighting Intensity: §b" + GleamConfigs.GLOBAL_LIGHT_INTENSITY.get());
            debugInfoSide.add("UV Blacklight Rendering: " + SimplifyBoolean(GleamConfigs.ENABLE_UV_BLACKLIGHTS.get()));
            debugInfoSide.add("Dim Farther Lights: " + SimplifyBoolean(GleamConfigs.DIM_FARTHER_LIGHTS.get()));
        }
    }
}
