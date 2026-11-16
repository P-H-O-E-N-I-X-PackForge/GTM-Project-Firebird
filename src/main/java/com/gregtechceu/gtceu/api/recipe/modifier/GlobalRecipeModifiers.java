package com.gregtechceu.gtceu.api.recipe.modifier;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Recipe modifiers that apply to machines other than the ones that registered them, such as a modifier from an addon
 * that every machine should run its recipes through. They run after the machine's own modifier.
 */
public final class GlobalRecipeModifiers {

    private record Entry(Predicate<MachineDefinition> appliesTo, RecipeModifier modifier) {}

    private static final List<Entry> ENTRIES = new CopyOnWriteArrayList<>();
    private static volatile int version = 0;

    private GlobalRecipeModifiers() {}

    /** Adds a modifier to every machine. */
    public static void register(RecipeModifier modifier) {
        register(definition -> true, modifier);
    }

    /** Adds a modifier to the machines {@code appliesTo} accepts. Call it during setup. */
    public static synchronized void register(Predicate<MachineDefinition> appliesTo, RecipeModifier modifier) {
        ENTRIES.add(new Entry(appliesTo, modifier));
        version++;
    }

    /** Changes whenever a modifier is registered, so machines know to build their modifier again. */
    public static int version() {
        return version;
    }

    /**
     * The machine's own modifier followed by the global ones that apply to it. Machines that have no modifier of
     * their own, {@link RecipeModifier#NO_MODIFIER}, only get the global ones.
     */
    public static RecipeModifier wrap(MachineDefinition definition, RecipeModifier own) {
        List<RecipeModifier> extra = ENTRIES.stream().filter(e -> e.appliesTo().test(definition))
                .map(Entry::modifier).toList();
        if (extra.isEmpty()) return own;
        List<RecipeModifier> all = new java.util.ArrayList<>();
        if (own != RecipeModifier.NO_MODIFIER) all.add(own);
        all.addAll(extra);
        return new RecipeModifierList(all.toArray(RecipeModifier[]::new));
    }
}
