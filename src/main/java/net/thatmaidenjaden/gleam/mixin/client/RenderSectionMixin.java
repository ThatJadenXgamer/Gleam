package net.thatmaidenjaden.gleam.mixin.client;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.thatmaidenjaden.gleam.client.lighting.GleamLight;
import net.thatmaidenjaden.gleam.client.lighting.GleamLightEngine;
import net.thatmaidenjaden.gleam.client.lighting.SectionLightHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class RenderSectionMixin implements SectionLightHolder {

    @Unique private List<GleamLight> gleam$cachedLights = Collections.emptyList();

    @Override
    public List<GleamLight> gleam$lights() {
        return gleam$cachedLights;
    }

    @Override
    public void gleam$assignLights(List<GleamLight> lights) {
        this.gleam$cachedLights = lights;
        if (lights != null && !lights.isEmpty()) GleamLightEngine.getInstance().trackSection(this);
        else GleamLightEngine.getInstance().untrackSection(this);
    }

    @Inject(
            method = "reset",
            at = @At("HEAD")
    )
    private void gleam$onReset(CallbackInfo ci) {
        this.gleam$cachedLights = Collections.emptyList();
        GleamLightEngine.getInstance().untrackSection(this);
    }
}