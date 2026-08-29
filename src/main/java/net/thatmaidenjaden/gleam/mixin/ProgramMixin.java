package net.thatmaidenjaden.gleam.mixin;

import com.mojang.blaze3d.shaders.Program;
import net.thatmaidenjaden.gleam.Gleam;
import net.thatmaidenjaden.gleam.client.patcher.GleamShaderPatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Mixin(Program.class)
public abstract class ProgramMixin {

    @Unique
    private static final Set<String> TARGET_SHADERS = Set.of(
            "rendertype_solid",
            "rendertype_cutout",
            "rendertype_cutout_mipped",
            "rendertype_translucent"
    );

    @ModifyVariable(
            method = "compileShaderInternal",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private static InputStream gleam$patchShader(InputStream original, Program.Type type, String name) {
        if (!TARGET_SHADERS.contains(name)) return original;
        try {
            String source = new String(original.readAllBytes(), StandardCharsets.UTF_8);
            String patched = GleamShaderPatcher.applyPatch(source, type);
            Gleam.LOGGER.info("Gleam: patched {} shader '{}'", type.getName(), name);
            return new ByteArrayInputStream(patched.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Gleam.LOGGER.error("Gleam: failed to patch {} shader '{}'", type.getName(), name, e);
            return original;
        }
    }
}