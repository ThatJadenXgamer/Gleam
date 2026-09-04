package net.thatmaidenjaden.gleam.client.patcher;

import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.platform.VeilEventPlatform;
import io.github.ocelot.glslprocessor.api.GlslParser;
import io.github.ocelot.glslprocessor.api.GlslSyntaxException;
import io.github.ocelot.glslprocessor.api.grammar.GlslVersionStatement;
import io.github.ocelot.glslprocessor.api.node.GlslNode;
import io.github.ocelot.glslprocessor.api.node.GlslTree;
import io.github.ocelot.glslprocessor.api.node.function.GlslFunctionNode;
import net.minecraft.resources.ResourceLocation;
import net.thatmaidenjaden.gleam.Gleam;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class GleamVeilPreProcessor implements ShaderPreProcessor {

    private static final Set<String> TARGET_SHADERS = Set.of(
            "rendertype_solid",
            "rendertype_cutout",
            "rendertype_cutout_mipped",
            "rendertype_translucent"
    );

    public static void initializePatch() {
        VeilEventPlatform.INSTANCE.onVeilAddShaderProcessors((provider, registry) -> registry.addPreprocessor(new GleamVeilPreProcessor()));
        Gleam.LOGGER.info("Veil has been detected, applied integration shader pre-processor");
    }

    @Override
    public void modify(Context ctx, GlslTree tree) throws IOException, GlslSyntaxException {
        ResourceLocation name = ctx.name();
        if (name == null) return;

        String path = name.getPath();
        String baseName = path.substring(path.lastIndexOf('/') + 1);
        int extIdx = baseName.lastIndexOf('.');
        if (extIdx != -1) baseName = baseName.substring(0, extIdx);

        if (!TARGET_SHADERS.contains(baseName)) return;

        boolean isVertex = ctx.isVertex();
        boolean isFragment = ctx.isFragment();
        boolean isSodium = ctx instanceof SodiumContext;

        GlslVersionStatement version = tree.getVersionStatement();
        if (version.getVersion() < 430) {
            version.setVersion(430);
            version.setCore(true);
        }

        if (isVertex) {
            if (isSodium) {
                injectSodiumVertex(tree);
            } else {
                injectVanillaVertex(tree);
            }
        } else if (isFragment) {
            if (isSodium) {
                injectSodiumFragment(tree);
            } else {
                injectVanillaFragment(tree);
            }
        }
    }

    private void injectVanillaVertex(GlslTree tree) throws IOException, GlslSyntaxException {
        String declSource = "#version 430\n" + GleamShaderSources.COMMON_VERTEX_DECLARATIONS + "\n" + GleamShaderSources.VANILLA_VERTEX_FUNCTION;
        GlslTree declTree = GlslParser.parse(declSource);
        tree.getBody().addAll(0, declTree.getBody());

        GlslFunctionNode main = tree.mainFunction().orElseThrow(() -> new IOException("Vertex shader missing main()"));
        List<GlslNode> injectNodes = GlslParser.parseExpressionList(GleamShaderSources.VANILLA_VERTEX_MAIN_CALL);
        if (main.getBody() != null) main.getBody().addAll(0, injectNodes);
    }

    private void injectVanillaFragment(GlslTree tree) throws IOException, GlslSyntaxException {
        String declSource = "#version 430\n" + GleamShaderSources.VANILLA_FRAGMENT_FUNCTION;
        GlslTree declTree = GlslParser.parse(declSource);
        tree.getBody().addAll(0, declTree.getBody());

        GlslFunctionNode main = tree.mainFunction().orElseThrow(() -> new IOException("Fragment shader missing main()"));
        List<GlslNode> injectNodes = GlslParser.parseExpressionList(GleamShaderSources.COMMON_FRAGMENT_MAIN_CALL);
        if (main.getBody() != null) main.getBody().addAll(0, injectNodes);
    }

    private void injectSodiumVertex(GlslTree tree) throws IOException, GlslSyntaxException {
        String declSource = "#version 430\n" + GleamShaderSources.COMMON_VERTEX_DECLARATIONS + "\n" + GleamShaderSources.SODIUM_VERTEX_FUNCTION;
        GlslTree declTree = GlslParser.parse(declSource);
        tree.getBody().addAll(0, declTree.getBody());

        GlslFunctionNode main = tree.mainFunction().orElseThrow(() -> new IOException("Vertex shader missing main()"));
        List<GlslNode> injectNodes = GlslParser.parseExpressionList(GleamShaderSources.VEIL_SODIUM_VERTEX_MAIN_CALL);
        if (main.getBody() != null) main.getBody().addAll(0, injectNodes);
    }

    private void injectSodiumFragment(GlslTree tree) throws IOException, GlslSyntaxException {
        String declSource = "#version 430\n" + GleamShaderSources.SODIUM_FRAGMENT_FUNCTION;
        GlslTree declTree = GlslParser.parse(declSource);
        tree.getBody().addAll(0, declTree.getBody());

        GlslFunctionNode main = tree.mainFunction().orElseThrow(() -> new IOException("Fragment shader missing main()"));
        List<GlslNode> injectNodes = GlslParser.parseExpressionList(GleamShaderSources.COMMON_FRAGMENT_MAIN_CALL);
        if (main.getBody() != null) main.getBody().addAll(0, injectNodes);
    }
}