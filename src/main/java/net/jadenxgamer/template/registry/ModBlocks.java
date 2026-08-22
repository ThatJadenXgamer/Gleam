package net.jadenxgamer.template.registry;

import net.jadenxgamer.template.Template;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, Template.MOD_ID);

    public static final Supplier<Block> TEST = BLOCKS.register("test", () ->
            new Block(BlockBehaviour.Properties.of()));

    public static void init(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
