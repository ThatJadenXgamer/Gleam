package net.thatmaidenjaden.gleam.mixin.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.thatmaidenjaden.gleam.client.lighting.GleamEmitterRegistry;
import net.thatmaidenjaden.gleam.client.lighting.GleamLight;
import net.thatmaidenjaden.gleam.client.lighting.SectionLightHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Pseudo
@Mixin(ChunkBuilderMeshingTask.class)
public class ChunkBuilderMeshingTaskMixin {
    @Unique private static final Method GET_BLOCK_STATE;

    // Fabric API is being an absolute bitchass motherfucker piece of shit cunt and refuses to compile unless I reflect it
    static {
        try {
            Class<?> levelSliceClass = Class.forName("net.caffeinemc.mods.sodium.client.world.LevelSlice");
            GET_BLOCK_STATE = levelSliceClass.getMethod("getBlockState", int.class, int.class, int.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find LevelSlice.getBlockState() method", e);
        }
    }

    @Inject(
            method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
            at = @At("RETURN")
    )
    private void gleam$captureLights(ChunkBuildContext buildContext, net.caffeinemc.mods.sodium.client.util.task.CancellationToken cancellationToken, CallbackInfoReturnable<ChunkBuildOutput> cir) {
        ChunkBuildOutput output = cir.getReturnValue();
        if (output == null) return;
        RenderSection section = output.render;
        if (section == null) return;
        BuiltSectionInfo info = output.info;
        if (info == null) return;

        BlockGetter slice = buildContext.cache.getWorldSlice();
        List<GleamLight> lights = new ArrayList<>();
        BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();

        int baseX = section.getOriginX();
        int baseY = section.getOriginY();
        int baseZ = section.getOriginZ();

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    int blockX = baseX + x;
                    int blockY = baseY + y;
                    int blockZ = baseZ + z;
                    try {
                        BlockState state = (BlockState) GET_BLOCK_STATE.invoke(slice, blockX, blockY, blockZ);
                        if (GleamEmitterRegistry.isEmitter(state)) {
                            if (state.getFluidState().isSource()) {
                                BlockState aboveState;
                                try { aboveState = (BlockState) GET_BLOCK_STATE.invoke(slice, blockX, blockY + 1, blockZ); } catch (Exception ignored) { aboveState = null; }
                                if (aboveState != null && aboveState.getFluidState().isSource() && aboveState.getBlock() == state.getBlock()) continue;
                            }

                            boolean isExposed = false;
                            for (Direction dir : Direction.values()) {
                                neighbor.set(blockX + dir.getStepX(), blockY + dir.getStepY(), blockZ + dir.getStepZ());
                                BlockState neighborState;
                                try { neighborState = (BlockState) GET_BLOCK_STATE.invoke(slice, neighbor.getX(), neighbor.getY(), neighbor.getZ()); } catch (Exception ignored) { continue; }
                                if (!neighborState.isSolidRender(slice, neighbor)) {
                                    isExposed = true;
                                    break;
                                }
                            }
                            if (!isExposed) continue;

                            GleamLight light = GleamEmitterRegistry.createLight(state, blockX, blockY, blockZ);
                            if (light != null) lights.add(light);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        if (section instanceof SectionLightHolder holder) holder.gleam$assignLights(lights);
    }
}