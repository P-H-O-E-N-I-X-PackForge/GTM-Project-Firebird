package com.gregtechceu.gtceu.api.recipe;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.condition.RecipeConditionType;
import com.gregtechceu.gtceu.api.recipe.gui.RecipeUIModifier;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;

import brachy.modularui.api.drawable.Text;
import com.google.gson.JsonObject;
import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

@Accessors(chain = true)
public abstract class RecipeCondition<T extends RecipeCondition<T>> {

    public static final Codec<RecipeCondition<?>> CODEC = GTRegistries.RECIPE_CONDITIONS.codec()
            .dispatch(RecipeCondition::getType, RecipeConditionType::getCodec);

    // spotless:off
    public static <RC extends RecipeCondition<RC>> Products.P1<RecordCodecBuilder.Mu<RC>, Boolean> isReverse(RecordCodecBuilder.Instance<RC> instance) {
        return instance.group(Codec.BOOL.optionalFieldOf("reverse", false).forGetter(val -> val.isReverse));
    }
    // spotless:on

    public static <RC extends RecipeCondition<RC>> Codec<RC> simpleCodec(Function<Boolean, RC> function) {
        return RecordCodecBuilder.create(instance -> isReverse(instance).apply(instance, function));
    }

    @Getter
    @Setter
    protected boolean isReverse;

    public RecipeCondition() {
        this(false);
    }

    public RecipeCondition(boolean isReverse) {
        this.isReverse = isReverse;
    }

    public abstract RecipeConditionType<T> getType();

    public boolean isOr() {
        return false;
    }

    public abstract Component getTooltips();

    /**
     * A short label for the condition on recipe pages, shown as a tinted pill under the inputs instead of a line of
     * text, where the viewer can show one. Its tooltip is the longer text.
     *
     * @param text    the label, for example "sculk"
     * @param color   the tint, as ARGB
     * @param tooltip shown when the pill is hovered
     */
    public record ViewerPill(String text, int color, java.util.List<Component> tooltip) {}

    /** The pill this condition shows on recipe pages, or null to show {@link #getTooltips()} as a line of text. */
    public @org.jetbrains.annotations.Nullable ViewerPill viewerPill() {
        return null;
    }

    /**
     * Whether the recipe viewers leave the recipe out, for conditions that decide who a recipe is for. Asked on the
     * client whenever the viewers list recipes, so it may read the client's own state; tell the viewers with
     * {@code GTViewerVisibility.refresh()} when that state changes.
     */
    public boolean hiddenInViewer(@NotNull GTRecipe recipe) {
        return false;
    }

    public RecipeUIModifier modifyUI() {
        var textLine = RecipeUIModifier.textLine(Text.of(getTooltips()));
        ViewerPill pill = viewerPill();
        if (pill == null) return textLine;
        return (recipe, widget) -> {
            if (com.gregtechceu.gtceu.GTCEu.Mods.isEMILoaded()) {
                com.gregtechceu.gtceu.integration.recipeviewer.emi.GTEmiPills.add(widget, pill);
            } else {
                textLine.buildRecipeUI(recipe, widget);
            }
        };
    }

    public boolean check(@NotNull GTRecipe recipe, @NotNull RecipeLogic recipeLogic) {
        boolean test = testCondition(recipe, recipeLogic);
        return test != isReverse;
    }

    protected abstract boolean testCondition(@NotNull GTRecipe recipe, @NotNull RecipeLogic recipeLogic);

    public abstract T createTemplate();

    @NotNull
    public final JsonObject serialize() {
        var ops = RegistryOps.create(JsonOps.INSTANCE, GTRegistries.builtinRegistry());
        return CODEC.encodeStart(ops, this).getOrThrow(false, GTCEu.LOGGER::error).getAsJsonObject();
    }

    public static RecipeCondition<?> deserialize(@NotNull JsonObject config) {
        var ops = RegistryOps.create(JsonOps.INSTANCE, GTRegistries.builtinRegistry());
        return CODEC.parse(ops, config).getOrThrow(false, GTCEu.LOGGER::error);
    }

    @SuppressWarnings("deprecation")
    public final void toNetwork(FriendlyByteBuf buf) {
        var ops = RegistryOps.create(NbtOps.INSTANCE, GTRegistries.builtinRegistry());
        buf.writeWithCodec(ops, CODEC, this);
    }

    @SuppressWarnings("deprecation")
    public static RecipeCondition<?> fromNetwork(FriendlyByteBuf buf) {
        var ops = RegistryOps.create(NbtOps.INSTANCE, GTRegistries.builtinRegistry());
        return buf.readWithCodec(ops, CODEC);
    }
}
