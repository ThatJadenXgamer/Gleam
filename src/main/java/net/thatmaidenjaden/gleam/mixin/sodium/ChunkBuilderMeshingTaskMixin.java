package net.thatmaidenjaden.gleam.mixin.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import net.caffeinemc.mods.sodium.client.util.task.CancellationToken;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.thatmaidenjaden.gleam.client.lighting.GleamEmitterRegistry;
import net.thatmaidenjaden.gleam.client.lighting.GleamLight;
import net.thatmaidenjaden.gleam.client.lighting.SectionLightHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Pseudo
@Mixin(ChunkBuilderMeshingTask.class)
public class ChunkBuilderMeshingTaskMixin {

    @Inject(
            method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
            at = @At("RETURN")
    )
    private void gleam$captureLights(ChunkBuildContext buildContext, CancellationToken cancellationToken, CallbackInfoReturnable<ChunkBuildOutput> cir) {
        ChunkBuildOutput output = cir.getReturnValue();
        if (output == null) return;
        RenderSection section = output.render;
        if (section == null) return;
        BuiltSectionInfo info = output.info;
        if (info == null) return;

        BlockGetter slice = buildContext.cache.getWorldSlice();
        List<GleamLight> lights = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();

        int baseX = section.getOriginX();
        int baseY = section.getOriginY();
        int baseZ = section.getOriginZ();

        if (baseY < -64 || baseY > 319) {
            if (section instanceof SectionLightHolder holder) holder.gleam$assignLights(List.of());
            return;
        }

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    int blockX = baseX + x;
                    int blockY = baseY + y;
                    int blockZ = baseZ + z;
                    cursor.set(blockX, blockY, blockZ);
                    BlockState state;
                    try { state = slice.getBlockState(cursor); } catch (Exception ignored) { continue; }

                    if (GleamEmitterRegistry.isEmitter(state)) {
                        if (state.getFluidState().isSource()) {
                            BlockPos abovePos = cursor.above();
                            try {
                                BlockState aboveState = slice.getBlockState(abovePos);
                                if (aboveState.getFluidState().isSource() && aboveState.getBlock() == state.getBlock()) {
                                    continue;
                                }
                            } catch (Exception ignored) {}
                        }

                        boolean isExposed = false;
                        for (Direction dir : Direction.values()) {
                            neighbor.setWithOffset(cursor, dir);
                            if (neighbor.getY() < -64 || neighbor.getY() > 319) {
                                isExposed = true;
                                break;
                            }
                            try {
                                BlockState neighborState = slice.getBlockState(neighbor);
                                if (!neighborState.isSolidRender(slice, neighbor)) {
                                    isExposed = true;
                                    break;
                                }
                            } catch (Exception ignored) {
                                isExposed = true;
                                break;
                            }
                        }
                        if (!isExposed) continue;

                        GleamLight light = GleamEmitterRegistry.createLight(state, blockX, blockY, blockZ);
                        if (light != null) lights.add(light);
                    }
                }
            }
        }

        if (section instanceof SectionLightHolder holder) holder.gleam$assignLights(lights);
    }
}