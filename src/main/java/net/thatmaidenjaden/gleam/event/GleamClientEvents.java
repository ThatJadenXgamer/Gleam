package net.thatmaidenjaden.gleam.event;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.thatmaidenjaden.gleam.Gleam;
import net.thatmaidenjaden.gleam.client.lighting.GleamLightEngine;

@EventBusSubscriber(value = Dist.CLIENT, modid = Gleam.MOD_ID)
public class GleamClientEvents {

    @SubscribeEvent
    public static void onShaderRegistration(RegisterShadersEvent event) {
        GleamLightEngine.getInstance().rebindBlocks();
    }
}