package com.gregtechceu.gtceu.integration.recipeviewer.jei.recipe;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.category.GTRecipeCategory;
import com.gregtechceu.gtceu.api.recipe.gui.GTRecipeViewerWidget;
import com.gregtechceu.gtceu.api.recipe.gui.viewer.GTViewerVisibility;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.integration.recipeviewer.jei.GTJeiIngredients;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;

import brachy.modularui.integration.jei.recipe.ModularUIJeiCategory;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class GTRecipeJEICategory extends ModularUIJeiCategory<GTRecipe> {

    public static final Function<GTRecipeCategory, RecipeType<GTRecipe>> TYPES = Util
            .memoize(c -> new RecipeType<>(c.registryKey, GTRecipe.class));

    private final GTRecipeCategory category;

    public GTRecipeJEICategory(IJeiHelpers helpers, GTRecipeCategory category) {
        super(GTRecipeViewerWidget::new, GTRecipe::getId);
        this.category = category;
    }

    @Override
    public RecipeType<GTRecipe> getRecipeType() {
        return TYPES.apply(category);
    }

    @Override
    public Component getTitle() {
        return Component.translatable(category.getLanguageKey());
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return (IDrawable) category.getIcon().get();
    }

    @Override
    public int getMaxWidth() {
        return 250;
    }

    @Override
    public int getMaxHeight() {
        return 250;
    }

    @Override
    public void setupRecipeIngredients(IRecipeLayoutBuilder builder, GTRecipe recipe, IFocusGroup focuses) {
        addContents(builder, RecipeIngredientRole.INPUT, recipe.inputs);
        addContents(builder, RecipeIngredientRole.OUTPUT, recipe.outputs);
    }

    private static void addContents(IRecipeLayoutBuilder builder, RecipeIngredientRole role,
                                    java.util.Map<com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability<?>, List<com.gregtechceu.gtceu.api.recipe.content.Content>> contents) {
        contents.keySet().stream().sorted(com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability.COMPARATOR)
                .forEach(cap -> {
                    for (var content : contents.get(cap)) {
                        for (var entry : cap.toViewerEntries(content)) {
                            GTJeiIngredients.addInvisible(builder, role, entry);
                        }
                    }
                });
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<GTRecipeCategory> subCategories = new ArrayList<>();
        // run main categories first
        for (GTRecipeCategory category : GTRegistries.RECIPE_CATEGORIES) {
            if (!category.shouldRegisterDisplays()) continue;
            var type = category.getRecipeType();
            if (category == type.getCategory()) {
                type.buildRepresentativeRecipes();
            } else {
                subCategories.add(category);
                continue;
            }
            var wrapped = type.getRecipesInCategory(category).stream()
                    .filter(r -> GTViewerVisibility.isVisible(category.registryKey, r.id, r)).toList();
            registration.addRecipes(TYPES.apply(category), wrapped);
        }
        // run subcategories
        for (GTRecipeCategory subCategory : subCategories) {
            if (!subCategory.shouldRegisterDisplays()) continue;
            var type = subCategory.getRecipeType();
            var wrapped = type.getRecipesInCategory(subCategory).stream()
                    .filter(r -> GTViewerVisibility.isVisible(subCategory.registryKey, r.id, r)).toList();
            registration.addRecipes(TYPES.apply(subCategory), wrapped);
        }
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (MachineDefinition machine : GTRegistries.MACHINES) {
            for (GTRecipeType type : machine.getRecipeTypes()) {
                for (GTRecipeCategory category : type.getCategories()) {
                    if (!category.isXEIVisible() && !GTCEu.isDev()) continue;
                    registration.addRecipeCatalyst(machine.asStack(), machineType(category));
                }
            }
        }
    }

    public static RecipeType<?> machineType(GTRecipeCategory category) {
        if (category == GTRecipeTypes.FURNACE_RECIPES.getCategory()) return RecipeTypes.SMELTING;
        return TYPES.apply(category);
    }
}
