package net.thatmaidenjaden.gleam.mixin.buffer;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.thatmaidenjaden.gleam.client.lighting.GleamLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(
            method = "reloadShaders",
            at = @At(value = "HEAD")
    )
    private void gleam$clearShadersBeforeReload(ResourceProvider resourceProvider, CallbackInfo ci) {
        GleamLightEngine.getInstance().clearShaders();
    }
}