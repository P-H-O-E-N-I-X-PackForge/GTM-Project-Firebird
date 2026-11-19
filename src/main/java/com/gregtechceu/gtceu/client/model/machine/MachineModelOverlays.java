package com.gregtechceu.gtceu.client.model.machine;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Lets addons draw extra quads on machines they did not make, such as an overlay on one face for some state of the
 * machine, without mixing into {@link MachineModel}. A provider is asked for each face and render type a machine's
 * block is drawn with, after the machine's own model and its covers, and adds the quads to the list it is given. Quads
 * are cached by the game, so the machine has to tell the client when its look changes
 * ({@code @RerenderOnChanged} on a synced field does).
 */
@OnlyIn(Dist.CLIENT)
public final class MachineModelOverlays {

    @FunctionalInterface
    public interface Provider {

        /**
         * @param machine    the machine being drawn
         * @param side       the face asked for, or null for the quads that belong to no face
         * @param renderType the render type being drawn, or null when every type is asked for at once
         * @param quads      the quads so far; add to it
         */
        void addQuads(MetaMachine machine, @Nullable Direction side, @Nullable RenderType renderType,
                      RandomSource random, ModelData modelData, List<BakedQuad> quads);
    }

    private record Entry(Predicate<MetaMachine> appliesTo, Provider provider) {}

    private static final List<Entry> ENTRIES = new CopyOnWriteArrayList<>();

    private MachineModelOverlays() {}

    /** Adds overlay quads to the machines {@code appliesTo} accepts. Call it during client setup. */
    public static void register(Predicate<MetaMachine> appliesTo, Provider provider) {
        ENTRIES.add(new Entry(appliesTo, provider));
    }

    static void apply(MetaMachine machine, @Nullable Direction side, @Nullable RenderType renderType,
                      RandomSource random, ModelData modelData, List<BakedQuad> quads) {
        for (Entry entry : ENTRIES) {
            if (entry.appliesTo().test(machine)) {
                entry.provider().addQuads(machine, side, renderType, random, modelData, quads);
            }
        }
    }
}
