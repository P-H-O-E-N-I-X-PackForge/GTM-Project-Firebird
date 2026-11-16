package com.gregtechceu.gtceu.integration.kjs.viewer;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.recipe.gui.ViewerEntry;
import com.gregtechceu.gtceu.api.recipe.gui.viewer.GTViewerCategories;
import com.gregtechceu.gtceu.api.recipe.gui.viewer.GTViewerCategory;
import com.gregtechceu.gtceu.api.recipe.gui.viewer.GTViewerParts;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.integration.recipeviewer.RecipeSlotRole;
import brachy.modularui.integration.recipeviewer.RecipeViewerSlotWidget;
import brachy.modularui.widgets.layout.Flow;
import dev.latvian.mods.rhino.Wrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lets scripts add a page type to the recipe viewers (EMI and JEI) without handling ModularUI. Bound to scripts as
 * {@code GTViewerPages}.
 * <p>
 * A page is described by data: a function gives the entries to show, and another gives each entry's layout as a plain
 * object, which this turns into the page. The inputs and outputs in the layout are also what the viewers' "uses" and
 * "recipes" lookups find the page by.
 *
 * <pre>{@code
 * GTViewerPages.create('kubejs:heat_sources')
 *     .icon('minecraft:magma_block')
 *     .title('Heat Sources')
 *     .entries(() => [{ block: 'minecraft:magma_block', heat: 300 }, { block: 'minecraft:lava_bucket', heat: 1200 }])
 *     .id(e => 'kubejs:heat/' + e.block.replace(':', '_'))
 *     .layout(e => ({
 *         inputs: [e.block],
 *         lines: ['Heat: ' + e.heat]
 *     }))
 *     .workstation('minecraft:furnace')
 *     .register()
 * }</pre>
 *
 * The layout object may have: {@code title} (text), {@code inputs} and {@code outputs} (lists of items, as an item
 * stack
 * or text such as {@code '3x minecraft:iron_ingot'}, or a fluid stack), {@code lines} (lists of text shown under the
 * slots).
 */
public final class GTViewerPages {

    private GTViewerPages() {}

    public static Builder create(String id) {
        return new Builder(ResourceLocation.parse(id));
    }

    public static final class Builder {

        private final ResourceLocation id;
        private ItemStack icon = ItemStack.EMPTY;
        private Component title;
        private Supplier<Object> entries = () -> List.of();
        private Function<Object, Object> entryId;
        private Function<Object, Object> layout;
        private final List<ItemStack> workstations = new ArrayList<>();
        private int maxWidth = 250;
        private int maxHeight = 250;
        private boolean recipeTree = true;
        private boolean hideCraftable = false;
        private boolean listFirst = false;
        private BooleanSupplier enabled = () -> true;

        private Builder(ResourceLocation id) {
            this.id = id;
            this.title = Component.literal(id.getPath());
        }

        /** The icon of the page's tab: an item stack, or an item as text. */
        public Builder icon(Object icon) {
            this.icon = item(icon);
            return this;
        }

        public Builder title(String title) {
            this.title = Component.literal(title);
            return this;
        }

        /** The entries to show a page for. Each can be anything; it is handed to the other functions as it is. */
        public Builder entries(Supplier<Object> entries) {
            this.entries = entries;
            return this;
        }

        /** The id of an entry's page, as text such as {@code 'kubejs:heat/magma'}. Needs to be unique. */
        public Builder id(Function<Object, Object> entryId) {
            this.entryId = entryId;
            return this;
        }

        /** The layout of an entry's page, as a plain object; see the class description. */
        public Builder layout(Function<Object, Object> layout) {
            this.layout = layout;
            return this;
        }

        /** An item that opens this page in the viewers: an item stack, or an item as text. */
        public Builder workstation(Object stack) {
            this.workstations.add(item(stack));
            return this;
        }

        public Builder maxSize(int width, int height) {
            this.maxWidth = width;
            this.maxHeight = height;
            return this;
        }

        /** Leave the page out of EMI's recipe tree, for pages that are not about crafting. */
        public Builder noRecipeTree() {
            this.recipeTree = false;
            return this;
        }

        public Builder hideCraftable() {
            this.hideCraftable = true;
            return this;
        }

        /** List this page's tab before the GT recipe categories. */
        public Builder listFirst() {
            this.listFirst = true;
            return this;
        }

        /** Only add the page while this is true. */
        public Builder enabledIf(BooleanSupplier enabled) {
            this.enabled = enabled;
            return this;
        }

        /** Adds the page to the viewers. Call it from a startup script. */
        public void register() {
            if (entryId == null) throw new IllegalStateException("page " + id + " needs id(...)");
            if (layout == null) throw new IllegalStateException("page " + id + " needs layout(...)");
            var category = GTViewerCategory.builder(id, Object.class)
                    .icon(() -> icon)
                    .title(title)
                    .enabledIf(enabled)
                    .entries(() -> asList(entries.get()))
                    .id(entry -> entryLocation(entry))
                    .ui(entry -> buildWidget(entry))
                    .inputs(entry -> lookup(entry, "inputs"))
                    .outputs(entry -> lookup(entry, "outputs"))
                    .maxSize(maxWidth, maxHeight)
                    .recipeTree(recipeTree)
                    .hideCraftable(hideCraftable)
                    .listFirst(listFirst);
            for (ItemStack stack : workstations) category.workstation(() -> stack);
            GTViewerCategories.register(category.build());
        }

        private ResourceLocation entryLocation(Object entry) {
            Object result = unwrap(entryId.apply(entry));
            return ResourceLocation.parse(String.valueOf(result));
        }

        private Map<?, ?> layoutOf(Object entry) {
            Object result = unwrap(layout.apply(entry));
            if (result instanceof Map<?, ?> map) return map;
            throw new IllegalArgumentException("layout(...) of page " + id + " must return an object, not " + result);
        }

        private List<ViewerEntry> lookup(Object entry, String key) {
            List<ViewerEntry> entries = new ArrayList<>();
            try {
                for (Object value : asList(layoutOf(entry).get(key))) {
                    Object stack = unwrap(value);
                    if (stack instanceof FluidStack fluid) entries.add(ViewerEntry.of(fluid));
                    else {
                        ItemStack item = item(stack);
                        if (!item.isEmpty()) entries.add(ViewerEntry.of(item));
                    }
                }
            } catch (Throwable t) {
                GTCEu.LOGGER.warn("Could not read the {} of a {} page: {}", key, id, t.toString());
            }
            return entries;
        }

        private IWidget buildWidget(Object entry) {
            Flow page = Flow.col().coverChildren(100, 30).padding(4).childPadding(3);
            try {
                Map<?, ?> map = layoutOf(entry);
                Object title = unwrap(map.get("title"));
                if (title != null) page.child(Text.str(String.valueOf(title)).style(ChatFormatting.BOLD).asWidget());

                List<Object> inputs = asList(map.get("inputs"));
                List<Object> outputs = asList(map.get("outputs"));
                if (!inputs.isEmpty() || !outputs.isEmpty()) {
                    Flow row = Flow.row().coverChildren().childPadding(6);
                    if (!inputs.isEmpty()) row.child(slots(inputs, RecipeSlotRole.INPUT));
                    if (!inputs.isEmpty() && !outputs.isEmpty()) row.child(Text.str("→").asWidget());
                    if (!outputs.isEmpty()) row.child(slots(outputs, RecipeSlotRole.OUTPUT));
                    page.child(row);
                }
                for (Object line : asList(map.get("lines"))) {
                    page.child(GTViewerParts.unique(Text.str(String.valueOf(unwrap(line))).asWidget()));
                }
            } catch (Throwable t) {
                GTCEu.LOGGER.error("Could not build a {} page", id, t);
                page.child(Text.str("This page could not be built, see the log").style(ChatFormatting.RED).asWidget());
            }
            return page;
        }

        private static IWidget slots(List<Object> values, RecipeSlotRole role) {
            int perRow = values.size() > 6 ? 6 : values.size();
            Flow column = Flow.col().coverChildren().childPadding(1);
            Flow row = null;
            int inRow = 0;
            for (Object value : values) {
                if (row == null || inRow >= perRow) {
                    row = Flow.row().coverChildren().childPadding(1);
                    column.child(row);
                    inRow = 0;
                }
                Object stack = unwrap(value);
                if (stack instanceof FluidStack fluid) {
                    row.child(GTViewerParts.unique(
                            RecipeViewerSlotWidget.create(FluidStack.class).recipeSlotRole(role).value(fluid)));
                } else {
                    row.child(GTViewerParts.unique(
                            RecipeViewerSlotWidget.create(ItemStack.class).recipeSlotRole(role).value(item(stack))));
                }
                inRow++;
            }
            return column;
        }
    }

    private static final Pattern COUNTED = Pattern.compile("^(\\d+)\\s*x\\s+(.+)$");

    /**
     * An item stack from an item stack, or from text: {@code 'minecraft:iron_ingot'} or
     * {@code '3x minecraft:iron_ingot'}.
     */
    static ItemStack item(Object value) {
        Object v = unwrap(value);
        if (v instanceof ItemStack stack) return stack.copy();
        if (v == null) return ItemStack.EMPTY;
        String text = String.valueOf(v).trim();
        int count = 1;
        Matcher matcher = COUNTED.matcher(text);
        if (matcher.matches()) {
            count = Integer.parseInt(matcher.group(1));
            text = matcher.group(2).trim();
        }
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(text));
        return new ItemStack(item, count);
    }

    /** Whatever a script gives, as a Java object: scripts hand over their own wrappers for Java values. */
    static Object unwrap(Object value) {
        return value instanceof Wrapper wrapper ? wrapper.unwrap() : value;
    }

    /** A list from what a script gives: a script array, a list, a collection or a single value. */
    @SuppressWarnings("unchecked")
    static List<Object> asList(Object value) {
        Object v = unwrap(value);
        if (v == null) return List.of();
        if (v instanceof Collection<?> collection) return new ArrayList<>((Collection<Object>) collection);
        if (v instanceof Object[] array) return new ArrayList<>(Arrays.asList(array));
        return List.of(v);
    }
}
