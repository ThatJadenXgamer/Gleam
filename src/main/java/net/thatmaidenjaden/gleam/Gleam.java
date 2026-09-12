package net.thatmaidenjaden.gleam;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import net.thatmaidenjaden.gleam.config.GleamConfigImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Gleam implements ModInitializer {
    public static final String MOD_ID = "gleam";
    public static final Logger LOGGER = LoggerFactory.getLogger("Gleam");

    public void onInitialize() {
        GleamConfigImpl.init();
    }

    public static ResourceLocation gleamPath(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static ResourceLocation idPath(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}