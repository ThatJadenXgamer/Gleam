package net.thatmaidenjaden.gleam.event;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;
import net.thatmaidenjaden.gleam.client.assetdriven.LightProviderManager;

public class GleamEvents {

    public static void registerReloadListener() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new LightProviderManager());
    }
}