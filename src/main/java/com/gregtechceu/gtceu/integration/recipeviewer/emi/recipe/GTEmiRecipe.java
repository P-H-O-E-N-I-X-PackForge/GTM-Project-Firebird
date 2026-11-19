package com.gregtechceu.gtceu.integration.recipeviewer.emi.recipe;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.gui.GTRecipeViewerWidget;
import com.gregtechceu.gtceu.api.recipe.gui.ViewerEntry;

import brachy.modularui.integration.emi.EmiStackConverter;
import brachy.modularui.integration.recipeviewer.entry.EntryList;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class GTEmiRecipe extends GTModularEmiRecipe {

    final EmiRecipeCategory category;
    final GTRecipe recipe;

    public GTEmiRecipe(GTRecipe recipe, EmiRecipeCategory category) {
        super(recipe.getId(), () -> new GTRecipeViewerWidget(recipe));
        this.category = category;
        this.recipe = recipe;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return category;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return inputsOf(recipe.inputs);
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputsOf(recipe.outputs);
    }

    // *** Contents ***

    /**
     * Every capability is asked for what to show, so a capability with its own stack type reaches EMI's usage lookups
     * and recipe tree without a special case here.
     */
    public static List<EmiIngredient> inputsOf(Map<RecipeCapability<?>, List<Content>> contents) {
        List<EmiIngredient> ingredients = new ObjectArrayList<>();
        forEachEntry(contents, entry -> {
            EmiIngredient ingredient = toEmi(entry);
            if (!ingredient.isEmpty()) ingredients.add(ingredient);
        });
        return ingredients;
    }

    public static List<EmiStack> outputsOf(Map<RecipeCapability<?>, List<Content>> contents) {
        List<EmiStack> outputs = new ObjectArrayList<>();
        forEachEntry(contents, entry -> {
            var stacks = toEmi(entry).getEmiStacks();
            if (stacks.isEmpty()) return;
            outputs.add(stacks.get(0).copy().setChance(entry.chance()));
        });
        return outputs;
    }

    private static void forEachEntry(Map<RecipeCapability<?>, List<Content>> contents, Consumer<ViewerEntry> action) {
        contents.keySet().stream().sorted(RecipeCapability.COMPARATOR).forEach(cap -> {
            for (Content content : contents.get(cap)) {
                cap.toViewerEntries(content).forEach(action);
            }
        });
    }

    /** One viewer entry as EMI sees it, whatever its stack type, as long as a converter is registered for it. */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static EmiIngredient toEmi(ViewerEntry entry) {
        return EmiStackConverter.convertToEmiEntry((EntryList) entry.entries(), entry.chance(),
                UnaryOperator.identity());
    }
}
