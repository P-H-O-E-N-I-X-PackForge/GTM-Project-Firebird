package com.gregtechceu.gtceu.integration.recipeviewer.jei;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.category.GTRecipeCategory;
import com.gregtechceu.gtceu.api.recipe.gui.ViewerEntry;
import com.gregtechceu.gtceu.api.recipe.gui.viewer.GTViewerCategories;
import com.gregtechceu.gtceu.api.recipe.gui.viewer.GTViewerCategory;
import com.gregtechceu.gtceu.api.recipe.gui.viewer.GTViewerVisibility;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.integration.recipeviewer.GTViewerBuiltins;
import com.gregtechceu.gtceu.integration.recipeviewer.jei.recipe.GTRecipeJEICategory;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import brachy.modularui.integration.jei.recipe.ModularUIJeiCategory;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Puts every {@link GTViewerCategory} into JEI, and applies the visibility rules to JEI's recipes.
 */
public final class GTViewerJei {

    private static final Map<RecipeType<?>, Set<Object>> HIDDEN = new HashMap<>();
    private static boolean listening = false;

    private GTViewerJei() {}

    /**
     * @param first true for the pages listed before the GT recipe categories, false for the rest
     */
    public static void registerCategories(IRecipeCategoryRegistration registry, boolean first) {
        GTViewerBuiltins.ensureRegistered();
        listen();
        IJeiHelpers helpers = registry.getJeiHelpers();
        for (GTViewerCategory<?> category : GTViewerCategories.all()) {
            if (category.listFirst() == first) registry.addRecipeCategories(new ViewerCategory<>(helpers, category));
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        for (GTViewerCategory<?> category : GTViewerCategories.all()) {
            addRecipes(registration, category);
        }
    }

    private static <T> void addRecipes(IRecipeRegistration registration, GTViewerCategory<T> category) {
        registration.addRecipes(recipeType(category), category.visibleEntries());
    }

    public static void registerCatalysts(IRecipeCatalystRegistration registration) {
        for (GTViewerCategory<?> category : GTViewerCategories.all()) {
            for (var stack : category.workstations()) {
                registration.addRecipeCatalyst(stack, recipeType(category));
            }
        }
    }

    private static <T> RecipeType<T> recipeType(GTViewerCategory<T> category) {
        return new RecipeType<>(category.id(), category.type());
    }

    // *** Visibility ***

    private static void listen() {
        if (listening) return;
        listening = true;
        GTViewerVisibility.onRefresh(() -> Minecraft.getInstance().execute(GTViewerJei::applyVisibility));
    }

    /** Hides and unhides JEI's recipes so they match the rules, touching only what changed. */
    private static void applyVisibility() {
        if (GTCEu.Mods.isEMILoaded()) return;
        var runtime = GTJEIPlugin.getRuntime();
        if (runtime == null) return;
        for (GTViewerCategory<?> category : GTViewerCategories.all()) {
            syncCategory(runtime, category);
        }
        for (GTRecipeCategory category : GTRegistries.RECIPE_CATEGORIES) {
            if (!category.shouldRegisterDisplays()) continue;
            var type = GTRecipeJEICategory.TYPES.apply(category);
            List<GTRecipe> all = List.copyOf(category.getRecipeType().getRecipesInCategory(category));
            sync(runtime, type, all, r -> GTViewerVisibility.isVisible(category.registryKey, r.id, r));
        }
    }

    private static <T> void syncCategory(mezz.jei.api.runtime.IJeiRuntime runtime, GTViewerCategory<T> category) {
        sync(runtime, recipeType(category), category.allEntries(),
                e -> GTViewerVisibility.isVisible(category.id(), category.entryId(e), e));
    }

    private static <T> void sync(mezz.jei.api.runtime.IJeiRuntime runtime, RecipeType<T> type, List<T> all,
                                 java.util.function.Predicate<T> visible) {
        Set<Object> hidden = HIDDEN.computeIfAbsent(type, t -> new HashSet<>());
        List<T> toHide = new ArrayList<>();
        List<T> toShow = new ArrayList<>();
        for (T recipe : all) {
            boolean shouldShow = visible.test(recipe);
            boolean isHidden = hidden.contains(recipe);
            if (!shouldShow && !isHidden) toHide.add(recipe);
            else if (shouldShow && isHidden) toShow.add(recipe);
        }
        var manager = runtime.getRecipeManager();
        if (!toHide.isEmpty()) {
            manager.hideRecipes(type, toHide);
            hidden.addAll(toHide);
        }
        if (!toShow.isEmpty()) {
            manager.unhideRecipes(type, toShow);
            hidden.removeAll(toShow);
        }
    }

    // *** The JEI page type ***

    private static final class ViewerCategory<T> extends ModularUIJeiCategory<T> {

        private final GTViewerCategory<T> category;
        private final IDrawable icon;

        ViewerCategory(IJeiHelpers helpers, GTViewerCategory<T> category) {
            super(category::ui, category::entryId);
            this.category = category;
            this.icon = helpers.getGuiHelper().createDrawableItemStack(category.icon());
        }

        @Override
        public RecipeType<T> getRecipeType() {
            return recipeType(category);
        }

        @Override
        public Component getTitle() {
            return category.title();
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public int getMaxWidth() {
            return category.maxWidth();
        }

        @Override
        public int getMaxHeight() {
            return category.maxHeight();
        }

        @Override
        public void setupRecipeIngredients(IRecipeLayoutBuilder builder, T entry, IFocusGroup focuses) {
            for (ViewerEntry e : category.inputs(entry)) {
                GTJeiIngredients.addInvisible(builder, RecipeIngredientRole.INPUT, e);
            }
            for (ViewerEntry e : category.outputs(entry)) {
                GTJeiIngredients.addInvisible(builder, RecipeIngredientRole.OUTPUT, e);
            }
        }
    }
}
