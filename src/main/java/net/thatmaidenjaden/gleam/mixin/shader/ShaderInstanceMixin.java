package net.thatmaidenjaden.gleam.mixin.shader;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.thatmaidenjaden.gleam.client.lighting.GleamLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ShaderInstance.class)
public abstract class ShaderInstanceMixin {

    @Unique
    private static final Set<String> TARGET_SHADERS = Set.of(
            "rendertype_solid",
            "rendertype_cutout",
            "rendertype_cutout_mipped",
            "rendertype_translucent"
    );

    @Inject(
            method = "<init>(Lnet/minecraft/server/packs/resources/ResourceProvider;Lnet/minecraft/resources/ResourceLocation;Lcom/mojang/blaze3d/vertex/VertexFormat;)V",
            at = @At(value = "RETURN")
    )
    private void gleam$captureVanillaShaders(ResourceProvider resourceProvider, ResourceLocation shaderLocation, VertexFormat format, CallbackInfo ci) {
        String name = shaderLocation.getPath();
        if (TARGET_SHADERS.contains(name)) GleamLightEngine.getInstance().registerShader((ShaderInstance) (Object) this);
    }
}