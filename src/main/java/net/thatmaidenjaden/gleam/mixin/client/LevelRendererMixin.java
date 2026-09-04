package net.thatmaidenjaden.gleam.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.thatmaidenjaden.gleam.client.lighting.GleamLight;
import net.thatmaidenjaden.gleam.client.lighting.GleamLightEngine;
import net.thatmaidenjaden.gleam.client.lighting.SectionLightHolder;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow private Frustum cullingFrustum;

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

        double camX = camPos.x;
        double camY = camPos.y;
        double camZ = camPos.z;

        double maxSectionDistSq = 208.0 * 208.0;
        double maxLightDistSq = 192.0 * 192.0;

        Set<SectionLightHolder> sections = GleamLightEngine.getInstance().getActiveSections();

        for (SectionLightHolder holder : sections) {
            List<GleamLight> lights = holder.gleam$lights();
            if (lights.isEmpty()) continue;

            BlockPos origin = holder.gleam$getOrigin();
            double sDx = (origin.getX() + 8.0) - camX;
            double sDy = (origin.getY() + 8.0) - camY;
            double sDz = (origin.getZ() + 8.0) - camZ;

            if (sDx * sDx + sDy * sDy + sDz * sDz > maxSectionDistSq) continue;

            for (GleamLight light : lights) {
                double dx = light.x() - camX;
                double dy = light.y() - camY;
                double dz = light.z() - camZ;

                if (dx * dx + dy * dy + dz * dz > maxLightDistSq) continue;
                if (!cullingFrustum.isVisible(light.box())) continue;

                gleam$collectedLights.add(light);
            }
        }
    }
}