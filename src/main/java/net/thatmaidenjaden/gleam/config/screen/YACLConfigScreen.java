package net.thatmaidenjaden.gleam.config.screen;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.DoubleFieldControllerBuilder;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.values.TrackedValue;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.thatmaidenjaden.gleam.config.GleamConfigImpl;
import net.thatmaidenjaden.gleam.event.GleamClientEvents;

import java.util.ArrayList;

public class YACLConfigScreen {
    public static Screen getScreen(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("gleam.configuration.title"))
                .save(GleamClientEvents::onConfigReload)
                .category(ConfigCategory.createBuilder().name(Component.translatable("gleam.configuration.coloredLightSettings"))
                        .option(booleanEntry(GleamConfigImpl.ENABLE_COLORED_LIGHTS, Component.translatable("gleam.configuration.enableColoredLights")))
                        .option(booleanEntry(GleamConfigImpl.ENABLE_UV_BLACKLIGHTS, Component.translatable("gleam.configuration.enableUvBlacklights")))
                        .option(doubleEntry(GleamConfigImpl.GLOBAL_LIGHT_INTENSITY, Component.translatable("gleam.configuration.globalLightIntensity")))
                        .option(doubleEntry(GleamConfigImpl.GLOBAL_LIGHT_SATURATION, Component.translatable("gleam.configuration.globalLightSaturation")))
                        .option(doubleEntry(GleamConfigImpl.LIGHT_GATHERING_DISTANCE, Component.translatable("gleam.configuration.lightGatheringDistance")))
                        .option(doubleEntry(GleamConfigImpl.LIGHT_RENDER_DISTANCE, Component.translatable("gleam.configuration.lightRenderDistance")))
                        .option(booleanEntry(GleamConfigImpl.DIM_FARTHER_LIGHTS, Component.translatable("gleam.configuration.dimFartherLights")))
                        .option(doubleEntry(GleamConfigImpl.ANCHOR_DRIFT_DISTANCE, Component.translatable("gleam.configuration.lightRenderDistance"))).build()
                ).build().generateScreen(parent);
    }

    private static Option<Boolean> booleanEntry(TrackedValue<Boolean> value, MutableComponent name) {
        return Option.<Boolean>createBuilder()
                .name(name).description(OptionDescription.of(getTooltipComponents(value)))
                .binding(value.getDefaultValue(), value::value, value::setValue)
                .controller(BooleanControllerBuilder::create).build();
    }

    private static Option<Double> doubleEntry(TrackedValue<Double> value, MutableComponent name) {
        return Option.<Double>createBuilder()
                .name(name).description(OptionDescription.of(getTooltipComponents(value)))
                .binding(value.getDefaultValue(), value::value, value::setValue)
                .controller(DoubleFieldControllerBuilder::create).build();
    }

    public static Component[] getTooltipComponents(TrackedValue<?> value) {
        ArrayList<Component> tooltipComponents = new ArrayList<>();
        value.metadata(Comment.TYPE).forEach(comment -> tooltipComponents.add(Component.literal(comment)));
        return tooltipComponents.toArray(new Component[0]);
    }
}
