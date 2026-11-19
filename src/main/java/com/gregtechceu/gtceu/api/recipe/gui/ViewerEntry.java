package com.gregtechceu.gtceu.api.recipe.gui;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.fluids.FluidStack;

import brachy.modularui.integration.recipeviewer.entry.EntryList;
import brachy.modularui.integration.recipeviewer.entry.fluid.FluidStackList;
import brachy.modularui.integration.recipeviewer.entry.item.ItemStackList;

import java.util.List;

/**
 * One piece of recipe content as the recipe viewers see it: the possible stacks, and the chance it is produced or
 * consumed. EMI and JEI both build their ingredients from this, so a capability describes itself once.
 */
public record ViewerEntry(EntryList<?> entries, float chance) {

    public ViewerEntry(EntryList<?> entries) {
        this(entries, 1f);
    }

    public static ViewerEntry of(ItemStack stack) {
        return new ViewerEntry(ItemStackList.of(List.of(stack)));
    }

    public static ViewerEntry of(ItemLike item) {
        return of(new ItemStack(item));
    }

    public static ViewerEntry of(FluidStack stack) {
        return new ViewerEntry(FluidStackList.of(List.of(stack)));
    }
}
