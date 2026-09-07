package net.thatmaidenjaden.gleam.mixin.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.thatmaidenjaden.gleam.client.lighting.GleamLight;
import net.thatmaidenjaden.gleam.client.lighting.GleamLightEngine;
import net.thatmaidenjaden.gleam.client.lighting.SectionLightHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;

@Pseudo
@Mixin(RenderSection.class)
public abstract class RenderSectionMixin implements SectionLightHolder {

    @Shadow public abstract int getOriginX();
    @Shadow public abstract int getOriginY();
    @Shadow public abstract int getOriginZ();

    @Unique
    private List<GleamLight> gleam$cachedLights = Collections.emptyList();

    @Unique
    private BlockPos gleam$cachedOrigin;

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

    @Override
    public BlockPos gleam$getOrigin() {
        if (this.gleam$cachedOrigin == null) this.gleam$cachedOrigin = new BlockPos(this.getOriginX(), this.getOriginY(), this.getOriginZ());
        return this.gleam$cachedOrigin;
    }

    @Inject(
            method = "clearRenderState",
            at = @At(value = "HEAD")
    )
    private void gleam$onClearRenderState(CallbackInfoReturnable<Boolean> cir) {
        if (!this.gleam$cachedLights.isEmpty()) {
            this.gleam$cachedLights = Collections.emptyList();
            GleamLightEngine.getInstance().untrackSection(this);
        }
    }
}