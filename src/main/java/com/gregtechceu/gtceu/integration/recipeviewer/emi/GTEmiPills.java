package com.gregtechceu.gtceu.integration.recipeviewer.emi;

import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.api.recipe.gui.GTRecipeViewerWidget;

/** Puts recipe condition pills on EMI pages. Loads EMI classes, so call it only when EMI is present. */
public final class GTEmiPills {

    private GTEmiPills() {}

    public static void add(GTRecipeViewerWidget widget, RecipeCondition.ViewerPill pill) {
        widget.inputColumn.child(EmiHostWidget.pill(pill.text(), pill.color(), pill.tooltip()));
    }
}
