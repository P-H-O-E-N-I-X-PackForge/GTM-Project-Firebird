package com.gregtechceu.gtceu.api.recipe;

import com.gregtechceu.gtceu.GTCEu;

import java.util.ArrayDeque;

/**
 * Makes the real handling of a recipe's contents all or nothing. While one is open, the handlers that change anything
 * record how to take the change back, and if the handling does not succeed in the end, closing the transaction without
 * committing it runs those in reverse. Without it, a recipe that took its inputs and then found no room for its
 * outputs, as an ME output can fail to have, kept what it took and voided it.
 * <p>
 * Transactions nest, and are per thread. A transaction that is committed hands what it recorded to the one around it.
 */
public final class RecipeTransaction implements AutoCloseable {

    private static final ThreadLocal<RecipeTransaction> CURRENT = new ThreadLocal<>();

    private final RecipeTransaction parent;
    private final ArrayDeque<Runnable> undo = new ArrayDeque<>();
    private boolean finished;

    private RecipeTransaction() {
        this.parent = CURRENT.get();
        CURRENT.set(this);
    }

    public static RecipeTransaction begin() {
        return new RecipeTransaction();
    }

    /** Whether the code running is inside a transaction: whether what it changes should be recorded. */
    public static boolean isActive() {
        var current = CURRENT.get();
        return current != null && !current.finished;
    }

    /** Records how to take back a change that was just made. Does nothing when there is no transaction. */
    public static void record(Runnable undoAction) {
        var current = CURRENT.get();
        if (current == null || current.finished) return;
        current.undo.push(undoAction);
    }

    /** Keeps the changes, which the transaction around this one, if there is one, can still take back. */
    public void commit() {
        if (finished) return;
        finished = true;
        if (parent != null) {
            // the oldest change is at the end; the parent takes them back in the order that ends up the newest first
            for (var it = undo.descendingIterator(); it.hasNext();) {
                parent.undo.push(it.next());
            }
        }
        undo.clear();
    }

    /** Takes the changes back, the newest first. */
    public void rollback() {
        finished = true;
        Runnable action;
        while ((action = undo.poll()) != null) {
            try {
                action.run();
            } catch (RuntimeException e) {
                GTCEu.LOGGER.error("Recipe rollback failed", e);
            }
        }
    }

    @Override
    public void close() {
        if (!finished) rollback();
        if (parent == null) CURRENT.remove();
        else CURRENT.set(parent);
    }
}
