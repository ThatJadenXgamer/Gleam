package net.thatmaidenjaden.gleam.event;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.thatmaidenjaden.gleam.Gleam;
import net.thatmaidenjaden.gleam.client.lighting.GleamLightEngine;
import net.thatmaidenjaden.gleam.config.GleamConfigImpl;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@EventBusSubscriber(value = Dist.CLIENT, modid = Gleam.MOD_ID)
public class GleamClientEvents {

    // we need to do this otherwise the game reloads the pack TWICE for the actual config interface confirmation and file change
    private static final AtomicBoolean gleam$reloadScheduled = new AtomicBoolean(false);

    @SubscribeEvent
    public static void onShaderRegistration(RegisterShadersEvent event) {
        GleamLightEngine.getInstance().rebindBlocks();
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (Util.getPlatform() == Util.OS.OSX) {
            Gleam.LOGGER.error("Gleam is not compatible with macOS; We need features from OpenGL 4.3+ which is unsupported on this operating system.");
        }
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != GleamConfigImpl.CLIENT) return;
        if (!gleam$reloadScheduled.compareAndSet(false, true)) return;

        GleamLightEngine.getInstance().markDirty();
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            try {
                client.reloadResourcePacks().whenComplete((unit, throwable) -> gleam$reloadScheduled.set(false));
            } catch (Throwable throwable) {
                gleam$reloadScheduled.set(false);
            }
        });
    }
}