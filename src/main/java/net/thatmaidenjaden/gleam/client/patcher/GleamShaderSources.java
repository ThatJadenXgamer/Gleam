package net.thatmaidenjaden.gleam.client.patcher;

public final class GleamShaderSources {
    private GleamShaderSources() {}

    public static final String COMMON_VERTEX_DECLARATIONS = """
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

            out float v_GleamBlacklight;

            const vec3 LUM_WEIGHTS = vec3(0.2126, 0.7152, 0.0722);
            const float MIN_VISIBLE_LUM = 0.02;

            vec3 applyTonemap(vec3 color) {
                float lum = dot(color, LUM_WEIGHTS);
                return color / (1.0 + lum);
            }
            """;

    public static final String VANILLA_VERTEX_FUNCTION = """
            vec4 computeLighting(vec3 pos, vec4 baseColor, vec2 lightmapCoord) {
                v_GleamBlacklight = 0.0;

                if (lightCount == 0) return baseColor;

                float blockLight = lightmapCoord.x;
                float skyLight = lightmapCoord.y;

                float occlusionFactor = smoothstep(0.02, 0.08, blockLight);
                if (blockLight < 0.005) occlusionFactor = 0.0;

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

                vec3 coloredLightSum = vec3(0.0);
                vec3 blacklightSum = vec3(0.0);
                int safeLimit = min(localCount, 127);

                float skyLightLevel = skyLight * 16.0;
                float blacklightAccum = 0.0;
                float maxColoredLum = 0.0;

                for (int i = 0; i < safeLimit; i++) {
                    int lightIndex = cells[cellIndex].indices[i];
                    if (lightIndex < 0 || lightIndex >= lightCount) continue;

                    GleamLightSource light = lights[lightIndex];

                    vec3 delta = light.posRadius.xyz - pos;
                    float normalizedDistSq = dot(delta, delta) * light.posRadius.w;
                    if (normalizedDistSq >= 1.0) continue;

                    float falloff = 1.0 - normalizedDistSq;
                    falloff = falloff * falloff * falloff;

                    bool isBlacklight = dot(light.color.rgb, vec3(1.0)) <= 0.001 && light.color.a > 0.01;

                    if (isBlacklight) {
                        if (skyLightLevel <= 0.5) {
                            float contrib = falloff * light.color.a;
                            blacklightSum += vec3(0.35, 0.0, 1.0) * contrib;
                            blacklightAccum += contrib;
                        }
                    } else {
                        vec3 contrib = light.color.rgb * falloff * occlusionFactor;
                        coloredLightSum += contrib;
                        float lightLum = dot(light.color.rgb, LUM_WEIGHTS);
                        maxColoredLum = max(maxColoredLum, lightLum);
                    }
                }
                v_GleamBlacklight = min(blacklightAccum, 1.0);

                float totalColoredLum = dot(coloredLightSum, LUM_WEIGHTS);
                float maxSingleColoredLum = maxColoredLum * occlusionFactor;
                float maxAllowed = max(maxSingleColoredLum, MIN_VISIBLE_LUM);
                if (maxSingleColoredLum > 0.0 && totalColoredLum > maxAllowed) {
                    coloredLightSum *= maxAllowed / totalColoredLum;
                }

                vec3 composedLight = coloredLightSum + blacklightSum;
                composedLight = applyTonemap(composedLight);
                composedLight = clamp(composedLight, 0.0, 1.0);

                return vec4(baseColor.rgb + composedLight, baseColor.a);
            }
            """;

    public static final String SODIUM_VERTEX_FUNCTION = """
            vec4 computeLighting(vec3 pos, vec4 baseColor, vec2 lightmapCoord) {
                v_GleamBlacklight = 0.0;

                if (lightCount == 0) return baseColor;

                float blockLight = lightmapCoord.x;
                float skyLight = lightmapCoord.y;

                float blockFactor = smoothstep(0.01, 0.25, blockLight);
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

                vec3 coloredLightSum = vec3(0.0);
                vec3 blacklightSum = vec3(0.0);
                int safeLimit = min(localCount, 127);

                float skyLightLvl = skyLight * 16.0;
                float blacklightAccum = 0.0;
                float maxColoredLum = 0.0;

                for (int i = 0; i < safeLimit; i++) {
                    int lightIndex = cells[cellIndex].indices[i];
                    if (lightIndex < 0 || lightIndex >= lightCount) continue;

                    GleamLightSource light = lights[lightIndex];

                    vec3 delta = light.posRadius.xyz - pos;
                    float normalizedDistSq = dot(delta, delta) * light.posRadius.w;
                    if (normalizedDistSq >= 1.0) continue;

                    float falloff = 1.0 - normalizedDistSq;
                    falloff = falloff * falloff * falloff;

                    bool isBlacklight = dot(light.color.rgb, vec3(1.0)) <= 0.001 && light.color.a > 0.01;

                    if (isBlacklight) {
                        if (skyLightLvl <= 0.5) {
                            float contrib = falloff * light.color.a;
                            blacklightSum += vec3(0.35, 0.0, 1.0) * contrib;
                            blacklightAccum += contrib;
                        }
                    } else {
                        vec3 contrib = light.color.rgb * falloff * blockFactor;
                        coloredLightSum += contrib;
                        float lightLum = dot(light.color.rgb, LUM_WEIGHTS);
                        maxColoredLum = max(maxColoredLum, lightLum);
                    }
                }
                v_GleamBlacklight = min(blacklightAccum, 1.0);

                float totalColoredLum = dot(coloredLightSum, LUM_WEIGHTS);
                float maxSingleColoredLum = maxColoredLum * blockFactor;
                float maxAllowed = max(maxSingleColoredLum, MIN_VISIBLE_LUM);
                if (maxSingleColoredLum > 0.0 && totalColoredLum > maxAllowed) {
                    coloredLightSum *= maxAllowed / totalColoredLum;
                }

                vec3 composedLight = coloredLightSum + blacklightSum;
                composedLight = applyTonemap(composedLight);
                composedLight = clamp(composedLight, 0.0, 1.0);

                return vec4(baseColor.rgb + composedLight, baseColor.a);
            }
            """;

    public static final String VANILLA_FRAGMENT_FUNCTION = """
            in float v_GleamBlacklight;

            void applyBlacklightEmissive(inout vec4 fragColor) {
                if (v_GleamBlacklight <= 0.001 || fragColor.a <= 0.1) return;

                vec4 rawTexture = texture(Sampler0, texCoord0);

                float maxCol = max(max(rawTexture.r, rawTexture.g), rawTexture.b);
                float minCol = min(min(rawTexture.r, rawTexture.g), rawTexture.b);
                float saturation = maxCol > 0.0 ? (maxCol - minCol) / maxCol : 0.0;

                float neonFactor = smoothstep(0.42, 1.0, saturation) * smoothstep(0.4, 0.65, maxCol);
                float whiteFactor = (1.0 - smoothstep(0.0, 0.25, saturation)) * smoothstep(0.5, 0.95, maxCol);
                float fluorescentFactor = max(neonFactor, whiteFactor);

                float neonBrightness = mix(5.0 / 15.0, 1.0, smoothstep(0.42, 0.62, saturation));
                float saturationBoost = mix(1.0, 2.0, smoothstep(0.42, 0.72, saturation));

                float finalMultiplier = mix(neonBrightness * saturationBoost, 1.1, whiteFactor);

                if (fluorescentFactor > 0.0) {
                    float emissionStrength = fluorescentFactor * v_GleamBlacklight;

                    vec3 mintTint = vec3(0.68, 1.0, 0.92);
                    vec3 baseColor = mix(rawTexture.rgb, rawTexture.rgb * mintTint, whiteFactor);

                    vec3 targetNeonColor = baseColor * finalMultiplier;

                    vec3 baseEmission = max(fragColor.rgb, targetNeonColor * emissionStrength);
                    vec3 bloom = targetNeonColor * (emissionStrength * 0.25);

                    fragColor.rgb = clamp(baseEmission + bloom, 0.0, 1.0);
                }
            }
            """;

    public static final String SODIUM_FRAGMENT_FUNCTION = """
            in float v_GleamBlacklight;

            void applyBlacklightEmissive(inout vec4 fragColor) {
                if (v_GleamBlacklight <= 0.001 || fragColor.a <= 0.1) return;

                vec4 rawTexture = texture(u_BlockTex, v_TexCoord);

                float maxCol = max(max(rawTexture.r, rawTexture.g), rawTexture.b);
                float minCol = min(min(rawTexture.r, rawTexture.g), rawTexture.b);
                float saturation = maxCol > 0.0 ? (maxCol - minCol) / maxCol : 0.0;

                float neonFactor = smoothstep(0.42, 1.0, saturation) * smoothstep(0.4, 0.65, maxCol);
                float whiteFactor = (1.0 - smoothstep(0.0, 0.25, saturation)) * smoothstep(0.5, 0.95, maxCol);
                float fluorescentFactor = max(neonFactor, whiteFactor);

                float neonBrightness = mix(5.0 / 15.0, 1.0, smoothstep(0.42, 0.62, saturation));
                float saturationBoost = mix(1.0, 2.0, smoothstep(0.42, 0.72, saturation));

                float finalMultiplier = mix(neonBrightness * saturationBoost, 1.1, whiteFactor);

                if (fluorescentFactor > 0.0) {
                    float emissionStrength = fluorescentFactor * v_GleamBlacklight;

                    vec3 mintTint = vec3(0.68, 1.0, 0.92);
                    vec3 baseColor = mix(rawTexture.rgb, rawTexture.rgb * mintTint, whiteFactor);

                    vec3 targetNeonColor = baseColor * finalMultiplier;

                    vec3 baseEmission = max(fragColor.rgb, targetNeonColor * emissionStrength);
                    vec3 bloom = targetNeonColor * (emissionStrength * 0.25);

                    fragColor.rgb = clamp(baseEmission + bloom, 0.0, 1.0);
                }
            }
            """;

    public static final String VANILLA_VERTEX_MAIN_CALL = "    vertexColor = computeLighting(pos, vertexColor, UV2);\n";
    public static final String SODIUM_VERTEX_MAIN_CALL = "    v_Color = computeLighting(position, v_Color, _vert_tex_light_coord);\n";
    public static final String VEIL_SODIUM_VERTEX_MAIN_CALL = "    vertexColor = computeLighting(pos, PassVeilVertexColor, PassVeilLightUV);\n";
    public static final String COMMON_FRAGMENT_MAIN_CALL = "    applyBlacklightEmissive(fragColor);\n";
}