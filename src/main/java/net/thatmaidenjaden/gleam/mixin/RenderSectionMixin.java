package net.thatmaidenjaden.gleam.mixin;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.thatmaidenjaden.gleam.client.lighting.GleamLight;
import net.thatmaidenjaden.gleam.client.lighting.SectionLightHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import java.util.Collections;
import java.util.List;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class RenderSectionMixin implements SectionLightHolder {
    @Unique
    private List<GleamLight> gleam$cachedLights = Collections.emptyList();

    @Override
    public List<GleamLight> gleam$lights() {
        return gleam$cachedLights;
    }

    @Override
    public void gleam$assignLights(List<GleamLight> lights) {
        this.gleam$cachedLights = lights;
    }
}