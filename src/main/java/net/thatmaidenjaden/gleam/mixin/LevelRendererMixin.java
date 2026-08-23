package net.thatmaidenjaden.gleam.mixin;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.phys.Vec3;
import net.thatmaidenjaden.gleam.client.lighting.GleamLight;
import net.thatmaidenjaden.gleam.client.lighting.GleamLightEngine;
import net.thatmaidenjaden.gleam.client.lighting.SectionLightHolder;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow @Final private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

    @Inject(
            method = "renderLevel",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSectionLayer(Lnet/minecraft/client/renderer/RenderType;DDDLorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
                    ordinal = 0,
                    shift = At.Shift.BEFORE)
    )
    private void gleam$uploadLights(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        Vec3 camPos = camera.getPosition();
        List<GleamLight> nearest = gleam$gatherLights(camPos);
        GleamLightEngine.getInstance().uploadLights(nearest, camPos.x, camPos.y, camPos.z);
    }

    @Unique
    private List<GleamLight> gleam$gatherLights(Vec3 camPos) {
        List<GleamLight> gathered = new ArrayList<>();
        for (SectionRenderDispatcher.RenderSection section : visibleSections) {
            if (section instanceof SectionLightHolder holder) {
                gathered.addAll(holder.gleam$lights());
                if (gathered.size() >= GleamLightEngine.MAX_LIGHTS) break;
            }
        }
        return gleam$sortByDistance(gathered, camPos);
    }

    @Unique
    private static List<GleamLight> gleam$sortByDistance(List<GleamLight> lights, Vec3 camPos) {
        List<GleamLight> sorted = new ArrayList<>(lights);
        sorted.sort(Comparator.comparingDouble(light -> light.distanceSquaredTo(camPos.x, camPos.y, camPos.z)));
        return sorted;
    }
}