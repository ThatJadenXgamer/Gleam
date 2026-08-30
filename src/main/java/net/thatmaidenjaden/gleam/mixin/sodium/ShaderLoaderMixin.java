package net.thatmaidenjaden.gleam.mixin.sodium;

import com.mojang.blaze3d.shaders.Program;
import net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader;
import net.minecraft.resources.ResourceLocation;
import net.thatmaidenjaden.gleam.client.patcher.GleamSodiumPatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(ShaderLoader.class)
public class ShaderLoaderMixin {

    @Inject(
            method = "getShaderSource",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void gleam$patchShaderSource(ResourceLocation name, CallbackInfoReturnable<String> cir) {
        String path = name.getPath();
        if (path.startsWith("blocks/block_layer_opaque")) {
            String source = cir.getReturnValue();
            Program.Type type = path.endsWith(".vsh") ? Program.Type.VERTEX : Program.Type.FRAGMENT;
            cir.setReturnValue(GleamSodiumPatcher.applyPatch(source, type));
        }
    }
}