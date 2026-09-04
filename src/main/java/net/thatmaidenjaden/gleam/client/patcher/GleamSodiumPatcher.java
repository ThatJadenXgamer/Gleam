package net.thatmaidenjaden.gleam.client.patcher;

import com.mojang.blaze3d.shaders.Program;

public class GleamSodiumPatcher {
    public static String applyPatch(String source, Program.Type type) {
        if (source.contains("#version 330 core")) {
            source = source.replace("#version 330 core", "#version 430 core");
        } else if (source.contains("#version 330")) {
            source = source.replaceFirst("#version 330\\b", "#version 430");
        }

        String extensions = "#extension GL_ARB_shader_storage_buffer_object : enable\n#extension GL_ARB_shading_language_420pack : enable\n";
        int versionIdx = source.indexOf("#version");
        if (versionIdx != -1) {
            int lineEnd = source.indexOf("\n", versionIdx);
            if (lineEnd != -1) source = source.substring(0, lineEnd + 1) + extensions + source.substring(lineEnd + 1);
        }

        StringBuilder sb = new StringBuilder(source);

        if (type == Program.Type.VERTEX) {
            int mainIdx = sb.indexOf("void main()");
            if (mainIdx == -1) return source;
            sb.insert(mainIdx, GleamShaderSources.COMMON_VERTEX_DECLARATIONS + "\n" + GleamShaderSources.SODIUM_VERTEX_FUNCTION + "\n");
            int closingBrace = sb.lastIndexOf("}");
            if (closingBrace != -1) sb.insert(closingBrace, GleamShaderSources.SODIUM_VERTEX_MAIN_CALL);
        } else if (type == Program.Type.FRAGMENT) {
            int mainIdx = sb.indexOf("void main()");
            if (mainIdx == -1) return source;
            sb.insert(mainIdx, GleamShaderSources.SODIUM_FRAGMENT_FUNCTION + "\n");
            int closingBrace = sb.lastIndexOf("}");
            if (closingBrace != -1) sb.insert(closingBrace, GleamShaderSources.COMMON_FRAGMENT_MAIN_CALL);
        }

        return sb.toString();
    }
}