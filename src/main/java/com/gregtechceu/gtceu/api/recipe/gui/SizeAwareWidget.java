package com.gregtechceu.gtceu.api.recipe.gui;

import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * A recipe viewer panel that reports when its size changes after it was first laid out. The viewer integrations use
 * this to measure again and lay the page out again.
 */
public interface SizeAwareWidget {

    void setSizeListener(@Nullable BiConsumer<Integer, Integer> listener);
}
