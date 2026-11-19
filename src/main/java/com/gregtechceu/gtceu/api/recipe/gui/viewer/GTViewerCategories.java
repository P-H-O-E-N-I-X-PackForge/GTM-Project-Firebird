package com.gregtechceu.gtceu.api.recipe.gui.viewer;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The extra pages GT and addons add to the recipe viewers. Register during mod setup: the viewers read this list when
 * they load, which is after that.
 */
public final class GTViewerCategories {

    private static final List<GTViewerCategory<?>> CATEGORIES = new ArrayList<>();

    private GTViewerCategories() {}

    public static synchronized <T> GTViewerCategory<T> register(GTViewerCategory<T> category) {
        for (var existing : CATEGORIES) {
            if (existing.id().equals(category.id())) {
                throw new IllegalStateException("viewer category " + category.id() + " is already registered");
            }
        }
        CATEGORIES.add(category);
        return category;
    }

    /** The enabled categories, in registration order. */
    public static synchronized List<GTViewerCategory<?>> all() {
        return CATEGORIES.stream().filter(GTViewerCategory::enabled).toList();
    }

    public static synchronized @Nullable GTViewerCategory<?> get(ResourceLocation id) {
        for (var category : CATEGORIES) {
            if (category.id().equals(id)) return category;
        }
        return null;
    }
}
