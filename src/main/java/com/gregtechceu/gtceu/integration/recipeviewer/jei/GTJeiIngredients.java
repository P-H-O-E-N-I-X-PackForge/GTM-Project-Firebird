package com.gregtechceu.gtceu.integration.recipeviewer.jei;

import com.gregtechceu.gtceu.api.recipe.gui.ViewerEntry;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import brachy.modularui.integration.recipeviewer.entry.EntryList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;

import java.util.Map;
import java.util.function.Predicate;

/**
 * Turns {@link ViewerEntry}s into JEI ingredients. Item and fluid are built in; a capability with its own stack type
 * registers it here, next to its {@code EmiStackConverter} converter.
 */
public final class GTJeiIngredients {

    private record Mapping<T>(IIngredientType<T> type, Predicate<T> empty) {}

    private static final Map<Class<?>, Mapping<?>> MAPPINGS = new Reference2ReferenceOpenHashMap<>();

    static {
        register(ItemStack.class, VanillaTypes.ITEM_STACK, ItemStack::isEmpty);
        register(FluidStack.class, ForgeTypes.FLUID_STACK, FluidStack::isEmpty);
    }

    private GTJeiIngredients() {}

    public static <T> void register(Class<T> stackClass, IIngredientType<T> type, Predicate<T> isEmpty) {
        MAPPINGS.put(stackClass, new Mapping<>(type, isEmpty));
    }

    /** Adds every stack of the entry as an invisible JEI ingredient, so lookups find the recipe. */
    public static void addInvisible(IRecipeLayoutBuilder builder, RecipeIngredientRole role, ViewerEntry entry) {
        addAll(builder, role, entry.entries());
    }

    @SuppressWarnings("unchecked")
    private static <T> void addAll(IRecipeLayoutBuilder builder, RecipeIngredientRole role, EntryList<T> list) {
        var mapping = (Mapping<T>) MAPPINGS.get(list.getType());
        if (mapping == null) return;
        for (T stack : list.getStacks()) {
            if (mapping.empty().test(stack)) continue;
            builder.addInvisibleIngredients(role).addIngredient(mapping.type(), stack);
        }
    }
}
