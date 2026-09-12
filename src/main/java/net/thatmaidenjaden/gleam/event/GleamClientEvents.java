package net.thatmaidenjaden.gleam.event;

import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.thatmaidenjaden.gleam.Gleam;
import net.thatmaidenjaden.gleam.client.lighting.GleamLightEngine;
import net.thatmaidenjaden.gleam.config.GleamConfigImpl;

import java.util.concurrent.atomic.AtomicBoolean;

public class GleamClientEvents {

    // we need to do this otherwise the game reloads the pack TWICE for the actual config interface confirmation and file change
    private static final AtomicBoolean gleam$reloadScheduled = new AtomicBoolean(false);

    public static void onShaderRegistration() {
        CoreShaderRegistrationCallback.EVENT.register(context -> GleamLightEngine.getInstance().rebindBlocks());
    }

    public static void onClientSetup() {
        if (Util.getPlatform() == Util.OS.OSX) {
            Gleam.LOGGER.error("Gleam is not compatible with macOS; We need features from OpenGL 4.3+ which is unsupported on this operating system.");
        }
    }

    public static void onConfigReload() {
        GleamConfigImpl.CONFIG.save();
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