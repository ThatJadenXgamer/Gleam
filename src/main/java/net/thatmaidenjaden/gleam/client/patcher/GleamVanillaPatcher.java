package net.thatmaidenjaden.gleam.client.patcher;

import com.mojang.blaze3d.shaders.Program;

public final class GleamVanillaPatcher {
    private static final String EXTENSION_PREAMBLE =
            """
            #extension GL_ARB_shader_storage_buffer_object : enable
            #extension GL_ARB_shading_language_420pack : enable
            """;

    private GleamVanillaPatcher() {}

    public static String applyPatch(String source, Program.Type type) {
        StringBuilder sb = new StringBuilder(source);
        if (type == Program.Type.VERTEX) {
            insertExtension(sb);
            injectVertexLighting(sb);
        } else if (type == Program.Type.FRAGMENT) {
            injectFragmentLighting(sb);
        }
        return sb.toString();
    }

    private static void insertExtension(StringBuilder sb) {
        int versionIdx = sb.indexOf("#version");
        if (versionIdx == -1) {
            sb.insert(0, EXTENSION_PREAMBLE);
        } else {
            int lineEnd = sb.indexOf("\n", versionIdx);
            sb.insert(lineEnd + 1, EXTENSION_PREAMBLE);
        }
    }

    private static void injectVertexLighting(StringBuilder sb) {
        int mainIdx = sb.indexOf("void main()");
        if (mainIdx == -1) return;
        sb.insert(mainIdx, GleamShaderSources.COMMON_VERTEX_DECLARATIONS + "\n" + GleamShaderSources.VANILLA_VERTEX_FUNCTION + "\n");
        int closingBrace = sb.lastIndexOf("}");
        if (closingBrace != -1) sb.insert(closingBrace, GleamShaderSources.VANILLA_VERTEX_MAIN_CALL);
    }

    private static void injectFragmentLighting(StringBuilder sb) {
        int mainIdx = sb.indexOf("void main()");
        if (mainIdx == -1) return;
        sb.insert(mainIdx, GleamShaderSources.VANILLA_FRAGMENT_FUNCTION + "\n");
        int closingBrace = sb.lastIndexOf("}");
        if (closingBrace != -1) sb.insert(closingBrace, GleamShaderSources.COMMON_FRAGMENT_MAIN_CALL);
    }
}