package net.thatmaidenjaden.gleam.config.screen;

import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.values.TrackedValue;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.DoubleListEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.thatmaidenjaden.gleam.config.GleamConfigImpl;
import net.thatmaidenjaden.gleam.event.GleamClientEvents;

import java.util.ArrayList;

public class ClothConfigScreen {
    public static Screen getScreen(Screen parent) {
        ConfigBuilder configBuilder = ConfigBuilder.create()
                .setTitle(Component.translatable("gleam.configuration.title"))
                .setSavingRunnable(GleamClientEvents::onConfigReload);
        ConfigCategory coloredLightSettings = configBuilder.getOrCreateCategory(Component.translatable("gleam.configuration.coloredLightSettings"));
        coloredLightSettings.addEntry(booleanEntry(configBuilder, GleamConfigImpl.ENABLE_COLORED_LIGHTS, Component.translatable("gleam.configuration.enableColoredLights")));
        coloredLightSettings.addEntry(booleanEntry(configBuilder, GleamConfigImpl.ENABLE_UV_BLACKLIGHTS, Component.translatable("gleam.configuration.enableUvBlacklights")));
        coloredLightSettings.addEntry(doubleEntry(configBuilder, GleamConfigImpl.GLOBAL_LIGHT_INTENSITY, Component.translatable("gleam.configuration.globalLightIntensity")));
        coloredLightSettings.addEntry(doubleEntry(configBuilder, GleamConfigImpl.GLOBAL_LIGHT_SATURATION, Component.translatable("gleam.configuration.globalLightSaturation")));
        coloredLightSettings.addEntry(doubleEntry(configBuilder, GleamConfigImpl.LIGHT_GATHERING_DISTANCE, Component.translatable("gleam.configuration.lightGatheringDistance")));
        coloredLightSettings.addEntry(doubleEntry(configBuilder, GleamConfigImpl.LIGHT_RENDER_DISTANCE, Component.translatable("gleam.configuration.lightRenderDistance")));
        coloredLightSettings.addEntry(booleanEntry(configBuilder, GleamConfigImpl.DIM_FARTHER_LIGHTS, Component.translatable("gleam.configuration.dimFartherLights")));
        coloredLightSettings.addEntry(doubleEntry(configBuilder, GleamConfigImpl.ANCHOR_DRIFT_DISTANCE, Component.translatable("gleam.configuration.lightRenderDistance")));
        return configBuilder.setParentScreen(parent).build();
    }

    private static BooleanListEntry booleanEntry(ConfigBuilder configBuilder, TrackedValue<Boolean> value, MutableComponent name) {
        return configBuilder.entryBuilder()
                .startBooleanToggle(name, value.value())
                .setDefaultValue(value.getDefaultValue())
                .setTooltip(getTooltipComponents(value, name)).setSaveConsumer(value::setValue).build();
    }

    public static DoubleListEntry doubleEntry(ConfigBuilder configBuilder, TrackedValue<Double> value, MutableComponent name) {
        return configBuilder.entryBuilder()
                .startDoubleField(name, value.value())
                .setDefaultValue(value.getDefaultValue())
                .setTooltip(getTooltipComponents(value, name)).setSaveConsumer(value::setValue).build();
    }

    public static Component[] getTooltipComponents(TrackedValue<?> value, MutableComponent name) {
        ArrayList<Component> tooltipComponents = new ArrayList<>();
        tooltipComponents.add(name.copy().withStyle(ChatFormatting.BOLD));
        value.metadata(Comment.TYPE).forEach(comment -> tooltipComponents.add(Component.literal(comment)));
        return tooltipComponents.toArray(new Component[0]);
    }
}
