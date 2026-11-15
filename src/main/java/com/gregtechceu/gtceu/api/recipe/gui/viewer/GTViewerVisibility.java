package com.gregtechceu.gtceu.api.recipe.gui.viewer;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Which recipes the recipe viewers show, decided by rules that read whatever state you like (research, unlocks, a
 * config). Rules apply to GT recipe categories and to {@link GTViewerCategory} pages, in EMI and JEI.
 * <p>
 * When the state a rule reads changes, call {@link #refresh()}. JEI hides and unhides the affected recipes right away;
 * EMI has no way to do that, so it rebuilds its recipes, which is slower, so call it when something actually changed
 * and not every tick.
 */
public final class GTViewerVisibility {

    @FunctionalInterface
    public interface Rule {

        /**
         * @param category the page type's id
         * @param recipe   the id of the recipe or entry
         * @param entry    the recipe or entry itself: a {@code GTRecipe} for recipe categories
         * @return false to hide it
         */
        boolean visible(ResourceLocation category, ResourceLocation recipe, Object entry);
    }

    private static final List<Rule> RULES = new CopyOnWriteArrayList<>();
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();

    private GTViewerVisibility() {}

    public static void addRule(Rule rule) {
        RULES.add(rule);
    }

    public static boolean isVisible(ResourceLocation category, ResourceLocation recipe, Object entry) {
        for (Rule rule : RULES) {
            if (!rule.visible(category, recipe, entry)) return false;
        }
        return true;
    }

    /** Asks the viewers to apply the rules again. */
    public static void refresh() {
        LISTENERS.forEach(Runnable::run);
    }

    /** For the viewer integrations: called by {@link #refresh()}. */
    public static void onRefresh(Runnable listener) {
        LISTENERS.add(listener);
    }
}
