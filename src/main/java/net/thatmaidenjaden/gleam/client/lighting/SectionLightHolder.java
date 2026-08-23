package net.thatmaidenjaden.gleam.client.lighting;

import java.util.List;

public interface SectionLightHolder {
    List<GleamLight> gleam$lights();
    void gleam$assignLights(List<GleamLight> lights);
}