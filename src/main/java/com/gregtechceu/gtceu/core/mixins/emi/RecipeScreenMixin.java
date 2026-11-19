package com.gregtechceu.gtceu.core.mixins.emi;

import com.gregtechceu.gtceu.integration.recipeviewer.emi.EmiPageScale;

import net.minecraft.client.gui.GuiGraphics;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.screen.RecipeScreen;
import dev.emi.emi.screen.WidgetGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * Makes EMI's recipe screen handle a recipe drawn smaller by {@link EmiPageScale}: the widget is drawn scaled, and
 * every
 * place EMI asks a widget where it is, or tells it where the mouse is, goes through the scale. A recipe that was not
 * scaled has no entry, and these all do exactly what EMI does.
 */
@Mixin(value = RecipeScreen.class, remap = false)
public abstract class RecipeScreenMixin {

    private static final String RENDER = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V";

    // *** Drawing ***

    @Redirect(method = RENDER,
              at = @At(value = "INVOKE",
                       target = "Ldev/emi/emi/api/widget/Widget;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"),
              require = 0)
    private void gtceu$renderScaled(Widget widget, GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        var info = EmiPageScale.of(widget);
        if (info == null) {
            widget.render(graphics, mouseX, mouseY, delta);
            return;
        }
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(info.offsetX(), 0, 0);
        pose.scale(info.scale(), info.scale(), 1f);
        try {
            widget.render(graphics, EmiPageScale.localX(widget, mouseX), EmiPageScale.localY(widget, mouseY), delta);
        } finally {
            pose.popPose();
        }
    }

    // *** Where widgets are ***

    @Redirect(method = { RENDER, "mouseClicked(DDI)Z", "keyPressed(III)Z" },
              at = @At(value = "INVOKE",
                       target = "Ldev/emi/emi/api/widget/Widget;getBounds()Ldev/emi/emi/api/widget/Bounds;"),
              require = 0)
    private Bounds gtceu$widgetBounds(Widget widget) {
        return EmiPageScale.bounds(widget, widget.getBounds());
    }

    @Redirect(method = { "mouseReleased(DDI)Z", "mouseDragged(DDIDD)Z" },
              at = @At(value = "INVOKE",
                       target = "Ldev/emi/emi/api/widget/SlotWidget;getBounds()Ldev/emi/emi/api/widget/Bounds;"),
              require = 0)
    private Bounds gtceu$slotBounds(SlotWidget widget) {
        return EmiPageScale.bounds(widget, widget.getBounds());
    }

    @Redirect(method = RENDER,
              at = @At(value = "INVOKE", target = "Ldev/emi/emi/api/widget/Widget;getTooltip(II)Ljava/util/List;"),
              require = 0)
    private List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> gtceu$tooltip(Widget widget,
                                                                                                          int mouseX,
                                                                                                          int mouseY) {
        return widget.getTooltip(EmiPageScale.localX(widget, mouseX), EmiPageScale.localY(widget, mouseY));
    }

    @Redirect(method = "mouseClicked(DDI)Z",
              at = @At(value = "INVOKE", target = "Ldev/emi/emi/api/widget/Widget;mouseClicked(III)Z"),
              require = 0)
    private boolean gtceu$click(Widget widget, int mouseX, int mouseY, int button) {
        return widget.mouseClicked(EmiPageScale.localX(widget, mouseX), EmiPageScale.localY(widget, mouseY), button);
    }

    @Redirect(method = "mouseReleased(DDI)Z",
              at = @At(value = "INVOKE", target = "Ldev/emi/emi/api/widget/SlotWidget;mouseClicked(III)Z"),
              require = 0)
    private boolean gtceu$release(SlotWidget widget, int mouseX, int mouseY, int button) {
        return widget.mouseClicked(EmiPageScale.localX(widget, mouseX), EmiPageScale.localY(widget, mouseY), button);
    }

    // *** Whether the mouse is over a recipe at all, for the recipe's own keys ***

    @Redirect(method = { "mouseClicked(DDI)Z", "keyPressed(III)Z" },
              at = @At(value = "INVOKE", target = "Ldev/emi/emi/screen/WidgetGroup;getWidth()I"),
              require = 0)
    private int gtceu$groupWidth(WidgetGroup group) {
        return EmiPageScale.groupWidth(group);
    }
}
