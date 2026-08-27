package net.thatmaidenjaden.gleam.client.patcher;

public final class GleamShaderPatcher {
    private static final String EXTENSION_PREAMBLE =
            """
            #extension GL_ARB_shader_storage_buffer_object : enable
            #extension GL_ARB_shading_language_420pack : enable
            """;

    private static final String LIGHTING_SHADER_CODE = """
            struct GleamLightSource {
                vec4 color;
                vec4 posRadius;
            };
            
            struct LightGridCell {
                int count;
                int indices[127];
            };
            
            layout(std430, binding = 10) buffer GleamLight {
                GleamLightSource lights[];
            };
            
            layout(std140, binding = 11) uniform GleamScene {
                int lightCount;
                int pad1;
                int pad2;
                int pad3;
            };
            
            layout(std430, binding = 12) buffer GleamGrid {
                LightGridCell cells[];
            };
            
            float getLightOcclusion(vec2 lightmapCoord) {
                float blockLight = lightmapCoord.x * 16.0;
                return clamp(blockLight / 15.0, 0.0, 1.0);
            }
            
            vec3 applyTonemap(vec3 color) {
                float lum = dot(color, vec3(0.2126, 0.7152, 0.0722));
                return color / (1.0 + lum);
            }
            
            vec4 computeLighting(vec3 pos, vec4 baseColor, vec2 lightmapCoord) {
                if (lightmapCoord.x <= 0.03 || lightCount == 0) return baseColor;
                vec3 positivePos = pos + vec3(1024.0);
            
                ivec3 chunkOffset = ivec3(floor(positivePos / 16.0));
                ivec3 cellCoord = ivec3(
                        ((chunkOffset.x % 32) + 32) % 32,
                        ((chunkOffset.y % 32) + 32) % 32,
                        ((chunkOffset.z % 32) + 32) % 32
                );
                int cellIndex = (cellCoord.x * 1024) + (cellCoord.y * 32) + cellCoord.z;
                if (cellIndex < 0 || cellIndex >= 32768) return baseColor;
            
                int localCount = cells[cellIndex].count;
                if (localCount <= 0) return baseColor;
            
                float occlusion = getLightOcclusion(lightmapCoord);
                if (occlusion <= 0.01) return baseColor;
            
                vec3 composedLight = vec3(0.0);
                int safeLimit = min(localCount, 127);
            
                for (int i = 0; i < safeLimit; i++) {
                    int lightIndex = cells[cellIndex].indices[i];
                    if (lightIndex < 0 || lightIndex >= lightCount) continue;
            
                    GleamLightSource light = lights[lightIndex];
            
                    vec3 delta = light.posRadius.xyz - pos;
                    float normalizedDistSq = dot(delta, delta) * light.posRadius.w;
                    if (normalizedDistSq >= 1.0) continue;
            
                    float cameraDistSq = dot(light.posRadius.xyz, light.posRadius.xyz);
            
                    float falloff;
                    float lodDimmer = 1.0;
            
                    if (cameraDistSq > 1024.0) {
                        float v = 1.0 - normalizedDistSq;
                        falloff = v * v * v;
                        lodDimmer = smoothstep(192.0, 160.0, (sqrt(cameraDistSq))) * 0.5;
                    } else {
                        float distanceFactor = 1.0 - normalizedDistSq;
                        falloff = distanceFactor * distanceFactor * distanceFactor;
                    }
            
                    composedLight += light.color.rgb * falloff * lodDimmer;
                }
            
                composedLight *= occlusion;
                composedLight = applyTonemap(composedLight);
                composedLight = clamp(composedLight, 0.0, 1.0);
            
                return vec4(baseColor.rgb + composedLight, baseColor.a);
            }""";

    private static final String MAIN_INJECT = "    vertexColor = computeLighting(pos, vertexColor, UV2);\n";

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
        if (closingBrace != -1) sb.insert(closingBrace, MAIN_INJECT);
    }
}