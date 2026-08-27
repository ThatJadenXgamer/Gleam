package net.thatmaidenjaden.gleam.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import net.thatmaidenjaden.gleam.client.lighting.GleamLight;
import net.thatmaidenjaden.gleam.client.lighting.GleamLightEngine;
import net.thatmaidenjaden.gleam.client.lighting.SectionLightHolder;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Unique private static final List<GleamLight> gleam$collectedLights = new ArrayList<>(16384);

    @Inject(
            method = "renderLevel",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSectionLayer(Lnet/minecraft/client/renderer/RenderType;DDDLorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
                    ordinal = 0,
                    shift = At.Shift.BEFORE)
    )
    private void gleam$uploadLights(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        Vec3 camPos = camera.getPosition();
        gleam$gatherLights(camPos);

        int totalSize = Math.min(gleam$collectedLights.size(), GleamLightEngine.MAX_TOTAL_LIGHTS);
        List<GleamLight> uploadedLights = gleam$collectedLights.subList(0, totalSize);

        GleamLightEngine.getInstance().uploadLights(uploadedLights, camPos.x, camPos.y, camPos.z);
    }

    @Unique
    private void gleam$gatherLights(Vec3 camPos) {
        gleam$collectedLights.clear();
        double anchorX = (Math.floor(camPos.x / 16.0) * 16.0) + 8.0;
        double anchorY = (Math.floor(camPos.y / 16.0) * 16.0) + 8.0;
        double anchorZ = (Math.floor(camPos.z / 16.0) * 16.0) + 8.0;

        double maxDistSq = 192.0 * 192.0;

        for (SectionLightHolder holder : GleamLightEngine.getInstance().getActiveSections()) {
            List<GleamLight> lights = holder.gleam$lights();
            if (lights.isEmpty()) continue;

            for (GleamLight light : lights) {
                double dx = light.x() - anchorX;
                double dy = light.y() - anchorY;
                double dz = light.z() - anchorZ;

                if (dx * dx + dy * dy + dz * dz > maxDistSq) continue;
                gleam$collectedLights.add(light);
            }
        }

        gleam$collectedLights.sort((a, b) -> {
            double distA = a.distanceSquaredTo(anchorX, anchorY, anchorZ);
            double distB = b.distanceSquaredTo(anchorX, anchorY, anchorZ);

            if (distA != distB) return Double.compare(distA, distB);

            if (a.x() != b.x()) return Double.compare(a.x(), b.x());
            if (a.y() != b.y()) return Double.compare(a.y(), b.y());
            return Double.compare(a.z(), b.z());
        });
    }
}