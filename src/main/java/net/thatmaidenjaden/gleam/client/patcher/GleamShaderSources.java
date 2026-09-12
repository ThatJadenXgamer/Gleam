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
                float anchorOffsetX;
                float anchorOffsetY;
                float anchorOffsetZ;
            };

            layout(std430, binding = 12) buffer GleamGrid {
                LightGridCell cells[];
            };

            out float v_GleamBlacklight;

            const vec3 LUM_WEIGHTS = vec3(0.2126, 0.7152, 0.0722);
            const float MIN_VISIBLE_LUM = 0.02;
            const float SURFACE_LUM_CEILING = 1.10;

            vec3 applyTonemap(vec3 color) {
                float lum = dot(color, LUM_WEIGHTS);
                return color / (1.0 + lum);
            }

            void processLight(int lightIndex, vec3 pos, vec3 anchorOffset, float occludeFactor, float skyLightLevel, inout vec3 coloredLightSum, inout vec3 blacklightSum, inout float blacklightAccum, inout float maxOccludedLum, inout float maxNonOccludedLum) {
                GleamLightSource light = lights[lightIndex];
                vec3 anchoredPos = light.posRadius.xyz + anchorOffset;
                vec3 delta = anchoredPos - pos;
                float rawInvRadiusSq = light.posRadius.w;
                bool occlude = rawInvRadiusSq >= 0.0;
                float invRadiusSq = abs(rawInvRadiusSq);
                float normalizedDistSq = dot(delta, delta) * invRadiusSq;
                if (normalizedDistSq >= 1.0) return;
                float falloff = 1.0 - normalizedDistSq;
                falloff = falloff * falloff * falloff;
                if (light.color.a < 0.0) {
                    if (skyLightLevel <= 0.5) {
                        float contrib = falloff * -light.color.a;
                        blacklightSum += vec3(0.35, 0.0, 1.0) * contrib;
                        blacklightAccum += contrib;
                    }
                } else {
                    float factor = occlude ? occludeFactor : 1.0;
                    vec3 contrib = light.color.rgb * (falloff * factor);
                    coloredLightSum += contrib;
                    float lightLum = dot(light.color.rgb, LUM_WEIGHTS);
                    if (occlude) maxOccludedLum = max(maxOccludedLum, lightLum);
                    else maxNonOccludedLum = max(maxNonOccludedLum, lightLum);
                }
            }
            """;

    public static final String VANILLA_VERTEX_FUNCTION = """
            vec4 computeLighting(vec3 pos, vec4 baseColor, vec2 lightmapCoord) {
                vec3 anchorOffset = vec3(anchorOffsetX, anchorOffsetY, anchorOffsetZ);
                v_GleamBlacklight = 0.0;
                if (lightCount == 0) return baseColor;

                float blockLight = lightmapCoord.x;
                float skyLight = lightmapCoord.y;

                float occlusionFactor = smoothstep(0.02, 0.08, blockLight);
                if (blockLight < 0.005) occlusionFactor = 0.0;

                vec3 positivePos = (pos - anchorOffset) + vec3(1024.0);
                ivec3 chunkOffset = ivec3(floor(positivePos / 16.0));
                ivec3 cellCoord = ivec3(
                        ((chunkOffset.x % 32) + 32) % 32,
                        ((chunkOffset.y % 32) + 32) % 32,
                        ((chunkOffset.z % 32) + 32) % 32
                );
                int cellIndex = (cellCoord.x * 1024) + (cellCoord.y * 32) + cellCoord.z;
                if (cellIndex < 0 || cellIndex >= 32768) return baseColor;

                int localCount = cells[cellIndex].count;
                if (localCount == 0) return baseColor;

                vec3 coloredLightSum = vec3(0.0);
                vec3 blacklightSum = vec3(0.0);

                float skyLightLevel = skyLight * 16.0;
                float blacklightAccum = 0.0;
                float maxOccludedLum = 0.0;
                float maxNonOccludedLum = 0.0;

                if (localCount > 0) {
                    int safeLimit = min(localCount, 127);
                    for (int i = 0; i < safeLimit; i++) {
                        int lightIndex = cells[cellIndex].indices[i];
                        processLight(lightIndex, pos, anchorOffset, occlusionFactor, skyLightLevel, coloredLightSum, blacklightSum, blacklightAccum, maxOccludedLum, maxNonOccludedLum);
                    }
                } else {
                    int actualCount = -localCount;
                    int safeLimit = min(actualCount, 254);
                    for (int i = 0; i < safeLimit; i++) {
                        int packedData = cells[cellIndex].indices[i / 2];
                        int lightIndex = (i % 2 == 0) ? (packedData & 0xFFFF) : ((packedData >> 16) & 0xFFFF);
                        processLight(lightIndex, pos, anchorOffset, occlusionFactor, skyLightLevel, coloredLightSum, blacklightSum, blacklightAccum, maxOccludedLum, maxNonOccludedLum);
                    }
                }

                v_GleamBlacklight = min(blacklightAccum, 1.0);

                float totalColoredLum = dot(coloredLightSum, LUM_WEIGHTS);
                float maxSingleColoredLum = max(maxOccludedLum * occlusionFactor, maxNonOccludedLum);
                float maxAllowed = max(maxSingleColoredLum, MIN_VISIBLE_LUM);
                if (maxSingleColoredLum > 0.0 && totalColoredLum > maxAllowed) coloredLightSum *= maxAllowed / totalColoredLum;

                vec3 composedLight = coloredLightSum + blacklightSum;
                composedLight = applyTonemap(composedLight);
                composedLight = clamp(composedLight, 0.0, 1.0);

                float lightLum = dot(composedLight, LUM_WEIGHTS);
                vec3 lightChroma = composedLight - vec3(lightLum);

                float baseLum = dot(baseColor.rgb, LUM_WEIGHTS);
                float allowedLum = min(lightLum, max(0.0, SURFACE_LUM_CEILING - baseLum));

                vec3 finalLight = lightChroma + vec3(allowedLum);
                vec3 finalColor = clamp(baseColor.rgb + finalLight, 0.0, 1.40);

                return vec4(finalColor, baseColor.a);
            }
            """;

    public static final String SODIUM_VERTEX_FUNCTION = """
            vec4 computeLighting(vec3 pos, vec4 baseColor, vec2 lightmapCoord) {
                vec3 anchorOffset = vec3(anchorOffsetX, anchorOffsetY, anchorOffsetZ);

                v_GleamBlacklight = 0.0;
                if (lightCount == 0) return baseColor;

                float blockLight = lightmapCoord.x;
                float skyLight = lightmapCoord.y;

                float blockFactor = smoothstep(0.01, 0.25, blockLight);
                vec3 positivePos = (pos - anchorOffset) + vec3(1024.0);

                ivec3 chunkOffset = ivec3(floor(positivePos / 16.0));
                ivec3 cellCoord = ivec3(
                        ((chunkOffset.x % 32) + 32) % 32,
                        ((chunkOffset.y % 32) + 32) % 32,
                        ((chunkOffset.z % 32) + 32) % 32
                );
                int cellIndex = (cellCoord.x * 1024) + (cellCoord.y * 32) + cellCoord.z;
                if (cellIndex < 0 || cellIndex >= 32768) return baseColor;

                int localCount = cells[cellIndex].count;
                if (localCount == 0) return baseColor;

                vec3 coloredLightSum = vec3(0.0);
                vec3 blacklightSum = vec3(0.0);

                float skyLightLvl = skyLight * 16.0;
                float blacklightAccum = 0.0;
                float maxOccludedLum = 0.0;
                float maxNonOccludedLum = 0.0;

                if (localCount > 0) {
                    int safeLimit = min(localCount, 127);
                    for (int i = 0; i < safeLimit; i++) {
                        int lightIndex = cells[cellIndex].indices[i];
                        processLight(lightIndex, pos, anchorOffset, blockFactor, skyLightLvl, coloredLightSum, blacklightSum, blacklightAccum, maxOccludedLum, maxNonOccludedLum);
                    }
                } else {
                    int actualCount = -localCount;
                    int safeLimit = min(actualCount, 254);
                    for (int i = 0; i < safeLimit; i++) {
                        int packedData = cells[cellIndex].indices[i / 2];
                        int lightIndex = (i % 2 == 0) ? (packedData & 0xFFFF) : ((packedData >> 16) & 0xFFFF);
                        processLight(lightIndex, pos, anchorOffset, blockFactor, skyLightLvl, coloredLightSum, blacklightSum, blacklightAccum, maxOccludedLum, maxNonOccludedLum);
                    }
                }

                v_GleamBlacklight = min(blacklightAccum, 1.0);

                float totalColoredLum = dot(coloredLightSum, LUM_WEIGHTS);
                float maxSingleColoredLum = max(maxOccludedLum * blockFactor, maxNonOccludedLum);
                float maxAllowed = max(maxSingleColoredLum, MIN_VISIBLE_LUM);
                if (maxSingleColoredLum > 0.0 && totalColoredLum > maxAllowed) coloredLightSum *= maxAllowed / totalColoredLum;

                vec3 composedLight = coloredLightSum + blacklightSum;
                composedLight = applyTonemap(composedLight);
                composedLight = clamp(composedLight, 0.0, 1.0);

                float lightLum = dot(composedLight, LUM_WEIGHTS);
                vec3 lightChroma = composedLight - vec3(lightLum);

                float baseLum = dot(baseColor.rgb, LUM_WEIGHTS);
                float allowedLum = min(lightLum, max(0.0, SURFACE_LUM_CEILING - baseLum));

                vec3 finalLight = lightChroma + vec3(allowedLum);
                vec3 finalColor = clamp(baseColor.rgb + finalLight, 0.0, 1.40);

                return vec4(finalColor, baseColor.a);
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