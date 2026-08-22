package net.jadenxgamer.template;

import net.jadenxgamer.template.registry.ModBlocks;
import net.jadenxgamer.template.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Template.MOD_ID)
public final class Template {
    public static final String MOD_ID = "template";
    public static final Logger LOGGER = LoggerFactory.getLogger("Template");

    public Template(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.init(modEventBus);
        ModItems.init(modEventBus);
    }

    public static ResourceLocation templatePath(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static ResourceLocation idPath(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
