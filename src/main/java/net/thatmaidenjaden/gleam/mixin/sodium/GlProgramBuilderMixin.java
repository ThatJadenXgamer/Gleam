package net.thatmaidenjaden.gleam.mixin.sodium;

import net.caffeinemc.mods.sodium.client.gl.shader.GlProgram;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ShaderBindingContext;
import net.thatmaidenjaden.gleam.client.lighting.GleamLightEngine;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL43;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Pseudo
@Mixin(GlProgram.Builder.class)
public class GlProgramBuilderMixin {

    @Inject(
            method = "link",
            at = @At("RETURN")
    )
    private <U> void gleam$bindSSBOs(Function<ShaderBindingContext, U> factory, CallbackInfoReturnable<GlProgram<U>> cir) {
        GlProgram<U> program = cir.getReturnValue();
        int id = program.handle();

        int index = GL43.glGetProgramResourceIndex(id, GL43.GL_SHADER_STORAGE_BLOCK, "GleamLight");
        if (index != GL31.GL_INVALID_INDEX) GL43.glShaderStorageBlockBinding(id, index, 10);

        index = GL31.glGetUniformBlockIndex(id, "GleamScene");
        if (index != GL31.GL_INVALID_INDEX) GL31.glUniformBlockBinding(id, index, 11);

        index = GL43.glGetProgramResourceIndex(id, GL43.GL_SHADER_STORAGE_BLOCK, "GleamGrid");
        if (index != GL31.GL_INVALID_INDEX) GL43.glShaderStorageBlockBinding(id, index, 12);

        GleamLightEngine.getInstance().registerProgram(id);
    }
}