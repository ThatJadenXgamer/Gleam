package net.thatmaidenjaden.gleam.mixin.client.buffer;

import net.minecraft.client.Minecraft;
import net.thatmaidenjaden.gleam.client.lighting.GleamLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(
            method = "destroy",
            at = @At(value = "HEAD")
    )
    private void gleam$disposeLightingEngine(CallbackInfo ci) {
        GleamLightEngine.getInstance().dispose();
    }
}