package com.gregtechceu.gtceu.api.cover;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Changes how much conveyors, robot arms, pumps and fluid regulators move, for packs and addons that want faster or
 * slower transfers than GT's tiers give, without mixing into the covers. A modifier is given the rate the cover would
 * have and returns the one it should have; they run in the order they were registered, when a cover is created.
 *
 * <pre>{@code
 * // every tiered cover moves four times as much
 * CoverTransferRates.register((kind, tier, rate) -> tier < 1 ? rate : rate * 4);
 * }</pre>
 */
public final class CoverTransferRates {

    public enum Kind {
        /** Items per second, of conveyors and robot arms. */
        ITEM,
        /** Millibuckets per tick, of pumps and fluid regulators. */
        FLUID
    }

    @FunctionalInterface
    public interface Modifier {

        /**
         * @param kind whether the cover moves items or fluids
         * @param tier the cover's voltage tier
         * @param rate the maximum transfer rate it has so far
         * @return the maximum transfer rate it should have
         */
        int modify(Kind kind, int tier, int rate);
    }

    private static final List<Modifier> MODIFIERS = new CopyOnWriteArrayList<>();

    private CoverTransferRates() {}

    /** Adds a modifier. Call it during setup, before covers are created. */
    public static void register(Modifier modifier) {
        MODIFIERS.add(modifier);
    }

    /** The rate after all the modifiers. Called by the covers. */
    public static int apply(Kind kind, int tier, int rate) {
        int result = rate;
        for (Modifier modifier : MODIFIERS) result = Math.max(1, modifier.modify(kind, tier, result));
        return result;
    }
}
