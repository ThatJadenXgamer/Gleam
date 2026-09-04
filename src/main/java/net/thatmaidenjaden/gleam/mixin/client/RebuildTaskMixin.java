package net.thatmaidenjaden.gleam.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.thatmaidenjaden.gleam.client.lighting.GleamEmitterRegistry;
import net.thatmaidenjaden.gleam.client.lighting.GleamLight;
import net.thatmaidenjaden.gleam.client.lighting.SectionLightHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$RebuildTask")
public abstract class RebuildTaskMixin {

    @Unique private static final int SECTION_EDGE = 16;
    @Shadow @Final SectionRenderDispatcher.RenderSection this$1;

    @Inject(
            method = "doTask",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/chunk/SectionCompiler;compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;",
                    shift = At.Shift.AFTER)
    )
    private void gleam$captureLights(SectionBufferBuilderPack pack, CallbackInfoReturnable<CompletableFuture<SectionRenderDispatcher.SectionTaskResult>> cir, @Local RenderChunkRegion region) {
        List<GleamLight> foundLights = gleam$scanChunk(region, this$1.getOrigin());
        if (this$1 instanceof SectionLightHolder holder) holder.gleam$assignLights(foundLights);
    }

    @Unique
    private static List<GleamLight> gleam$scanChunk(RenderChunkRegion region, BlockPos origin) {
        List<GleamLight> lights = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();

        int baseX = origin.getX(), baseY = origin.getY(), baseZ = origin.getZ();
        for (int x = 0; x < SECTION_EDGE; x++) {
            for (int y = 0; y < SECTION_EDGE; y++) {
                for (int z = 0; z < SECTION_EDGE; z++) {
                    cursor.set(baseX + x, baseY + y, baseZ + z);
                    BlockState state;
                    try { state = region.getBlockState(cursor); } catch (ArrayIndexOutOfBoundsException ignored) { continue; }

                    if (GleamEmitterRegistry.isEmitter(state)) {
                        if (state.getFluidState().isSource()) {
                            BlockPos abovePos = cursor.above();
                            BlockState aboveState;
                            try { aboveState = region.getBlockState(abovePos); } catch (ArrayIndexOutOfBoundsException ignored) { aboveState = null; }
                            if (aboveState != null && aboveState.getFluidState().isSource() && aboveState.getBlock() == state.getBlock()) continue;
                        }

                        boolean isExposed = false;
                        for (Direction dir : Direction.values()) {
                            neighbor.setWithOffset(cursor, dir);
                            BlockState neighborState;
                            try { neighborState = region.getBlockState(neighbor); } catch (ArrayIndexOutOfBoundsException ignored) { continue; }

                            if (!neighborState.isSolidRender(region, neighbor)) {
                                isExposed = true;
                                break;
                            }
                        }
                        if (!isExposed) continue;

                        GleamLight light = GleamEmitterRegistry.createLight(state, cursor.getX(), cursor.getY(), cursor.getZ());
                        if (light != null) lights.add(light);
                    }
                }
            }
        }
        return lights.isEmpty() ? List.of() : lights;
    }
}