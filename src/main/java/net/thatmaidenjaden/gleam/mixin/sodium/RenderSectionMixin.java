package net.thatmaidenjaden.gleam.mixin.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.thatmaidenjaden.gleam.client.lighting.GleamLight;
import net.thatmaidenjaden.gleam.client.lighting.GleamLightEngine;
import net.thatmaidenjaden.gleam.client.lighting.SectionLightHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

import java.util.Collections;
import java.util.List;

@Pseudo
@Mixin(RenderSection.class)
public class RenderSectionMixin implements SectionLightHolder {
    @Unique
    private List<GleamLight> gleam$cachedLights = Collections.emptyList();

    @Override
    public List<GleamLight> gleam$lights() {
        return gleam$cachedLights;
    }

    @Override
    public void gleam$assignLights(List<GleamLight> lights) {
        this.gleam$cachedLights = lights != null ? lights : Collections.emptyList();
        if (!this.gleam$cachedLights.isEmpty()) GleamLightEngine.getInstance().trackSection(this);
        else GleamLightEngine.getInstance().untrackSection(this);
    }
}