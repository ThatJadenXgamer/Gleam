package net.thatmaidenjaden.gleam;

import foundry.veil.platform.VeilEventPlatform;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.thatmaidenjaden.gleam.client.patcher.GleamVeilPreProcessor;

@Mod(value = Gleam.MOD_ID, dist = Dist.CLIENT)
public final class GleamClient {

    public GleamClient(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        if (FMLLoader.getLoadingModList().getModFileById("veil") != null) GleamVeilPreProcessor.initializePatch();
    }
}