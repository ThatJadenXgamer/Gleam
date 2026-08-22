package net.thatmaidenjaden.gleam;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Gleam.MOD_ID)
public final class Gleam {
    public static final String MOD_ID = "gleam";
    public static final Logger LOGGER = LoggerFactory.getLogger("Gleam");

    public Gleam(IEventBus modEventBus, ModContainer modContainer) {

    }

    public static ResourceLocation gleamPath(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static ResourceLocation idPath(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}