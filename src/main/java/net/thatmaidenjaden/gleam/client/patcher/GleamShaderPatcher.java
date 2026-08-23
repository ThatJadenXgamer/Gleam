package net.thatmaidenjaden.gleam.client.patcher;

public final class GleamShaderPatcher {
    private static final String EXTENSION_PREAMBLE = "#extension GL_ARB_shading_language_420pack : enable\n";
    private static final String LIGHTING_SHADER_CODE = """
            struct GleamLightSource {
                vec4 color;
                vec3 position;
                float radius;
            };
            
            layout(std140, binding = 10) uniform GleamLight {
                GleamLightSource lights[64];
            };
            
            layout(std140, binding = 11) uniform GleamScene {
                int lightCount;
                vec3 cameraPos;
            };
            
            vec3 applyTonemap(vec3 color) {
                float lum = dot(color, vec3(0.2126, 0.7152, 0.0722));
                vec3 mapped = color / (color + 1.0);
                return mix(color / (lum + 1.0), mapped, mapped);
            }
            
            vec4 computeLighting(vec3 worldPos, vec4 baseColor) {
                vec3 total = vec3(0.0);
                vec3 fragPos = worldPos + cameraPos;
                
                for (int i = 0; i < lightCount; i++) {
                    GleamLightSource light = lights[i];
                    float distance = distance(light.position, fragPos);
                    float falloff = smoothstep(0.0, 1.0, 1.0 - distance / light.radius);
                    total += light.color.rgb * light.color.a * falloff;
                }
                
                total = applyTonemap(total);
                total = clamp(total, 0.0, 1.0);
                
                return vec4(baseColor.rgb + total, baseColor.a);
            }
            """;
    private static final String MAIN_INJECT = "    vertexColor = computeLighting(pos, vertexColor);\n";

    private GleamShaderPatcher() {}

    public static String applyPatch(String source) {
        StringBuilder sb = new StringBuilder(source);
        insertExtension(sb);
        injectLighting(sb);
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

    private static void injectLighting(StringBuilder sb) {
        int mainIdx = sb.indexOf("void main()");
        if (mainIdx == -1) return;
        sb.insert(mainIdx, LIGHTING_SHADER_CODE + "\n");
        int closingBrace = sb.lastIndexOf("}");
        if (closingBrace != -1) {
            sb.insert(closingBrace, MAIN_INJECT);
        }
    }
}