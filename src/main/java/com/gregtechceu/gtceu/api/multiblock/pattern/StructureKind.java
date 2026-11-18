package com.gregtechceu.gtceu.api.multiblock.pattern;

/**
 * How one of a multiblock's structures relates to its default one, {@code "main"}.
 */
public enum StructureKind {

    /**
     * The structure is another shape the multiblock can have, such as a tier. Previews show one at a time, and the
     * terminal builds the one that is selected.
     */
    ALTERNATIVE,
    /**
     * The structure is built on top of the main one, such as an optional extension. It is a pattern of its own,
     * anchored
     * on the controller like the others, that only has to check the blocks it adds. Previews show it with the main
     * structure, and the terminal builds it with it, unless the player switches it off.
     */
    ADDITION
}
