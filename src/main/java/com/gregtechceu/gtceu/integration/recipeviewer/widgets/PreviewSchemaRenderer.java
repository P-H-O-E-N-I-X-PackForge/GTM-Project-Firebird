package com.gregtechceu.gtceu.integration.recipeviewer.widgets;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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
    private boolean gridVisible;
    private int gridStep;

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

    /** The grid under the structure: a line for each block, with the ones every {@code step} blocks stronger. */
    void setGrid(boolean visible, int step) {
        this.gridVisible = visible;
        this.gridStep = step;
    }

    @Override
    public void renderWorld(MultiBufferSource.BufferSource bufferSource, float partialTick) {
        super.renderWorld(bufferSource, partialTick);
        if (gridVisible) renderGrid(bufferSource);
        if (layers.isEmpty()) return;
        var pose = createWorldRenderPose();
        for (Layer layer : layers.values()) {
            for (BlockPos pos : layer.positions()) {
                layer.highlight().renderHighlight(pose, pos, null, camera().pos());
            }
        }
    }

    private void renderGrid(MultiBufferSource.BufferSource bufferSource) {
        if (!(schema() instanceof com.gregtechceu.gtceu.client.mui.schema.MutableSchema mutable) ||
                mutable.getBounds() == null) {
            return;
        }
        BlockPos min = mutable.getBounds().getFirst();
        BlockPos max = mutable.getBounds().getSecond();
        // under the structure, a block wider than it all round
        int margin = 1;
        int x0 = min.getX() - margin, x1 = max.getX() + 1 + margin;
        int z0 = min.getZ() - margin, z1 = max.getZ() + 1 + margin;
        float y = min.getY();

        var pose = createWorldRenderPose();
        var last = pose.last();
        var consumer = bufferSource.getBuffer(RenderType.lines());
        for (int x = x0; x <= x1; x++) {
            int color = lineColor(x);
            consumer.addVertex(last, x, y, z0).setColor(color).setNormal(last, 0, 0, 1);
            consumer.addVertex(last, x, y, z1).setColor(color).setNormal(last, 0, 0, 1);
        }
        for (int z = z0; z <= z1; z++) {
            int color = lineColor(z);
            consumer.addVertex(last, x0, y, z).setColor(color).setNormal(last, 1, 0, 0);
            consumer.addVertex(last, x1, y, z).setColor(color).setNormal(last, 1, 0, 0);
        }
        bufferSource.endBatch(RenderType.lines());
    }

    /** The lines every {@code gridStep} blocks, counted from the structure's origin, stand out from the others. */
    private int lineColor(int coordinate) {
        boolean strong = gridStep > 0 && Math.floorMod(coordinate, gridStep) == 0;
        return strong ? 0xCCFFFFFF : 0x55AAAAAA;
    }
}
