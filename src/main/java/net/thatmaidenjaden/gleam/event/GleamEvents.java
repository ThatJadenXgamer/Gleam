package net.thatmaidenjaden.gleam.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.thatmaidenjaden.gleam.Gleam;
import net.thatmaidenjaden.gleam.client.assetdriven.LightProviderManager;

@EventBusSubscriber(modid = Gleam.MOD_ID)
public class GleamEvents {

    @SubscribeEvent
    public static void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new LightProviderManager());
    }
}