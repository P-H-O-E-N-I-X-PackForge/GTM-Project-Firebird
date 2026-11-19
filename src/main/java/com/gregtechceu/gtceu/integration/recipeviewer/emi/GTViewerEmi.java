package com.gregtechceu.gtceu.integration.recipeviewer.emi;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.recipe.gui.viewer.GTViewerCategories;
import com.gregtechceu.gtceu.api.recipe.gui.viewer.GTViewerCategory;
import com.gregtechceu.gtceu.api.recipe.gui.viewer.GTViewerVisibility;
import com.gregtechceu.gtceu.integration.recipeviewer.GTViewerBuiltins;
import com.gregtechceu.gtceu.integration.recipeviewer.emi.recipe.GTEmiRecipe;
import com.gregtechceu.gtceu.integration.recipeviewer.emi.recipe.GTModularEmiRecipe;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiReloadManager;

import java.util.List;

/**
 * Puts every {@link GTViewerCategory} into EMI.
 */
public final class GTViewerEmi {

    private static boolean listening = false;

    private GTViewerEmi() {}

    /**
     * @param first true for the pages listed before the GT recipe categories, false for the rest
     */
    public static void register(EmiRegistry registry, boolean first) {
        GTViewerBuiltins.ensureRegistered();
        if (!listening) {
            listening = true;
            // EMI cannot hide a recipe once its recipes are built, so a change in what is visible rebuilds them
            GTViewerVisibility.onRefresh(
                    () -> Minecraft.getInstance().execute(() -> EmiReloadManager.reloadRecipes()));
        }
        for (GTViewerCategory<?> category : GTViewerCategories.all()) {
            if (category.listFirst() != first) continue;
            // one broken page type must not stop the rest of GT's recipes from loading
            try {
                registerCategory(registry, category);
            } catch (Throwable t) {
                GTCEu.LOGGER.error("Could not register the {} recipe viewer category", category.id(), t);
            }
        }
    }

    private static <T> void registerCategory(EmiRegistry registry, GTViewerCategory<T> category) {
        var emiCategory = new ViewerCategory(category);
        registry.addCategory(emiCategory);
        for (T entry : category.visibleEntries()) {
            try {
                registry.addRecipe(new ViewerRecipe<>(category, emiCategory, entry));
            } catch (Throwable t) {
                GTCEu.LOGGER.error("Could not add {} to the {} recipe viewer category", category.entryId(entry),
                        category.id(), t);
            }
        }
        for (var stack : category.workstations()) {
            registry.addWorkstation(emiCategory, EmiStack.of(stack));
        }
    }

    private static final class ViewerCategory extends EmiRecipeCategory {

        private final GTViewerCategory<?> category;

        ViewerCategory(GTViewerCategory<?> category) {
            super(category.id(), EmiStack.of(category.icon()));
            this.category = category;
        }

        @Override
        public Component getName() {
            return category.title();
        }
    }

    private static final class ViewerRecipe<T> extends GTModularEmiRecipe {

        private final GTViewerCategory<T> category;
        private final EmiRecipeCategory emiCategory;
        private final T entry;

        ViewerRecipe(GTViewerCategory<T> category, EmiRecipeCategory emiCategory, T entry) {
            super(category.entryId(entry), () -> category.ui(entry));
            this.category = category;
            this.emiCategory = emiCategory;
            this.entry = entry;
        }

        @Override
        public EmiRecipeCategory getCategory() {
            return emiCategory;
        }

        @Override
        public List<EmiIngredient> getInputs() {
            return category.inputs(entry).stream().map(GTEmiRecipe::toEmi).filter(i -> !i.isEmpty()).toList();
        }

        @Override
        public List<EmiStack> getOutputs() {
            return category.outputs(entry).stream().map(GTEmiRecipe::toEmi)
                    .flatMap(i -> i.getEmiStacks().stream().limit(1)).map(EmiStack::copy).toList();
        }

        @Override
        public boolean supportsRecipeTree() {
            return category.recipeTree();
        }

        @Override
        public boolean hideCraftable() {
            return category.hideCraftable();
        }
    }
}
