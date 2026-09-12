package net.thatmaidenjaden.gleam;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.thatmaidenjaden.gleam.client.patcher.GleamVeilPreProcessor;
import net.thatmaidenjaden.gleam.event.GleamClientEvents;
import net.thatmaidenjaden.gleam.event.GleamEvents;

public final class GleamClient implements ClientModInitializer {

    public void onInitializeClient() {
        if (FabricLoader.getInstance().getModContainer("veil").isPresent()) GleamVeilPreProcessor.initializePatch();
        GleamEvents.registerReloadListener();
        GleamClientEvents.onClientSetup();
        GleamClientEvents.onShaderRegistration();
    }
}