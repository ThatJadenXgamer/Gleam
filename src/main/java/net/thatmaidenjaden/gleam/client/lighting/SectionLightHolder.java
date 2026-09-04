package net.thatmaidenjaden.gleam.client.lighting;

import net.minecraft.core.BlockPos;
import java.util.List;

public interface SectionLightHolder {
    List<GleamLight> gleam$lights();
    void gleam$assignLights(List<GleamLight> lights);
    BlockPos gleam$getOrigin();
}