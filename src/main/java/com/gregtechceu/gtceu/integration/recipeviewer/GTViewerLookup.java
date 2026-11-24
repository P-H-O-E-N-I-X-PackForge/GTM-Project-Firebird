package com.gregtechceu.gtceu.integration.recipeviewer;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.world.item.ItemStack;

/**
 * Opens the recipe viewer on an item, for the places that show blocks outside of the viewer's own slots, like the
 * multiblock preview.
 */
public final class GTViewerLookup {

    private GTViewerLookup() {}

    /** Shows the recipes that make the item (or its uses, with {@code uses}). Returns false if no viewer took it. */
    public static boolean show(ItemStack stack, boolean uses) {
        if (stack.isEmpty()) return false;
        if (GTCEu.Mods.isEMILoaded()) return Emi.show(stack, uses);
        if (GTCEu.Mods.isJEILoaded()) return Jei.show(stack, uses);
        return false;
    }

    private static final class Emi {

        static boolean show(ItemStack stack, boolean uses) {
            var emi = dev.emi.emi.api.stack.EmiStack.of(stack);
            if (uses) dev.emi.emi.api.EmiApi.displayUses(emi);
            else dev.emi.emi.api.EmiApi.displayRecipes(emi);
            return true;
        }
    }

    private static final class Jei {

        static boolean show(ItemStack stack, boolean uses) {
            var runtime = com.gregtechceu.gtceu.integration.recipeviewer.jei.GTJEIPlugin.getRuntime();
            if (runtime == null) return false;
            var focus = runtime.getJeiHelpers().getFocusFactory().createFocus(
                    uses ? mezz.jei.api.recipe.RecipeIngredientRole.INPUT : mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT,
                    mezz.jei.api.constants.VanillaTypes.ITEM_STACK, stack);
            runtime.getRecipesGui().show(focus);
            return true;
        }
    }
}
