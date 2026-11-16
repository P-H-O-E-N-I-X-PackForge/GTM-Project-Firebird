package com.gregtechceu.gtceu.integration.recipeviewer.widgets;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;

import brachy.modularui.drawable.SchemaRenderer;
import brachy.modularui.drawable.schema.BlockHighlight;
import brachy.modularui.drawable.schema.ISchema;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The multiblock preview's 3D view, which can also keep outlines around blocks of its own choosing, in layers that
 * can be set and cleared separately. ModularUI's renderer only outlines the block under the mouse.
 */
class PreviewSchemaRenderer extends SchemaRenderer {

    private record Layer(Set<BlockPos> positions, BlockHighlight highlight) {}

    private final Map<String, Layer> layers = new LinkedHashMap<>();

    PreviewSchemaRenderer(ISchema schema) {
        super(schema);
    }

    void setLayer(String id, Collection<BlockPos> positions, int argb) {
        layers.put(id, new Layer(Set.copyOf(positions), new BlockHighlight(argb, 1 / 32f)));
    }

    void clearLayer(String id) {
        layers.remove(id);
    }

    void clearLayers() {
        layers.clear();
    }

    @Override
    public void renderWorld(MultiBufferSource.BufferSource bufferSource, float partialTick) {
        super.renderWorld(bufferSource, partialTick);
        if (layers.isEmpty()) return;
        var pose = createWorldRenderPose();
        for (Layer layer : layers.values()) {
            for (BlockPos pos : layer.positions()) {
                layer.highlight().renderHighlight(pose, pos, null, camera().pos());
            }
        }
    }
}
