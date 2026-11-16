package com.gregtechceu.gtceu.core.mixins.emi;

import com.gregtechceu.gtceu.integration.recipeviewer.emi.EmiPageScale;

import dev.emi.emi.screen.RecipeDisplay;
import dev.emi.emi.screen.WidgetGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * When a recipe is bigger than the room EMI has for it, which happens in a small window, marks it to be drawn smaller.
 * See {@link EmiPageScale}.
 */
@Mixin(value = RecipeDisplay.class, remap = false)
public abstract class RecipeDisplayMixin {

    @Shadow
    public abstract int getWidth();

    @Shadow
    public abstract int getHeight();

    @Shadow
    public abstract int getLeftWidth();

    @Shadow
    public abstract int getRightWidth();

    @Inject(method = "getWidgets(IIII)Ldev/emi/emi/screen/WidgetGroup;", at = @At("RETURN"), require = 0)
    private void gtceu$scaleToFit(int x, int y, int availableWidth, int availableHeight,
                                  CallbackInfoReturnable<WidgetGroup> cir) {
        WidgetGroup group = cir.getReturnValue();
        if (group == null || group.recipe == null) return;
        EmiPageScale.apply(group, getWidth(), getHeight(), availableWidth - getLeftWidth() - getRightWidth(),
                availableHeight);
    }
}
