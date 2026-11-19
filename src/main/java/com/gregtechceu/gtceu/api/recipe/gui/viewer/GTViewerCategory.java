package com.gregtechceu.gtceu.api.recipe.gui.viewer;

import com.gregtechceu.gtceu.api.recipe.gui.ViewerEntry;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import brachy.modularui.api.widget.IWidget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A page type in the recipe viewers (EMI and JEI) that is not a GT recipe category: an ore vein diagram, a multiblock
 * preview, a mod's own data. Describe it once here and register it with {@link GTViewerCategories#register}; both
 * viewers get it, with GT's panel sizing, visibility rules and workstations.
 *
 * <pre>{@code
 * GTViewerCategories.register(GTViewerCategory.builder(MyMod.id("heat_sources"), HeatSource.class)
 *         .icon(() -> new ItemStack(Items.MAGMA_BLOCK))
 *         .title(Component.translatable("mymod.viewer.heat_sources"))
 *         .entries(HeatSources::all)
 *         .id(source -> source.id())
 *         .ui(HeatSourceWidget::new)
 *         .outputs(source -> List.of(ViewerEntry.of(source.block())))
 *         .workstation(() -> new ItemStack(MyItems.THERMOMETER))
 *         .build());
 * }</pre>
 */
public final class GTViewerCategory<T> {

    private final ResourceLocation id;
    private final Class<T> type;
    private Supplier<ItemStack> icon = () -> ItemStack.EMPTY;
    private Supplier<Component> title;
    private BooleanSupplier enabled = () -> true;
    private Supplier<? extends Collection<? extends T>> entries = List::of;
    private Function<T, ResourceLocation> entryId;
    private Function<T, IWidget> ui;
    private Function<T, List<ViewerEntry>> inputs = t -> List.of();
    private Function<T, List<ViewerEntry>> outputs = t -> List.of();
    private final List<Supplier<List<ItemStack>>> workstations = new ArrayList<>();
    private boolean listFirst = false;
    private int maxWidth = 250;
    private int maxHeight = 250;
    private boolean recipeTree = true;
    private boolean hideCraftable = false;

    private GTViewerCategory(ResourceLocation id, Class<T> type) {
        this.id = id;
        this.type = type;
        this.title = () -> Component.translatable(id.toLanguageKey("jei"));
    }

    /** {@code type} is the class of the entries: JEI keys its pages by it. */
    public static <T> GTViewerCategory<T> builder(ResourceLocation id, Class<T> type) {
        return new GTViewerCategory<>(id, type);
    }

    public GTViewerCategory<T> icon(Supplier<ItemStack> icon) {
        this.icon = icon;
        return this;
    }

    public GTViewerCategory<T> title(Component title) {
        this.title = () -> title;
        return this;
    }

    /** Only registered while this is true, for config toggles. Checked when the viewers load. */
    public GTViewerCategory<T> enabledIf(BooleanSupplier enabled) {
        this.enabled = enabled;
        return this;
    }

    /** What pages to show. Called when the viewers load, so it may read the world's registries. */
    public GTViewerCategory<T> entries(Supplier<? extends Collection<? extends T>> entries) {
        this.entries = entries;
        return this;
    }

    /** The id the viewers use for an entry's page. */
    public GTViewerCategory<T> id(Function<T, ResourceLocation> entryId) {
        this.entryId = entryId;
        return this;
    }

    /** The page itself. Its size is measured, so it may cover its children. */
    public GTViewerCategory<T> ui(Function<T, IWidget> ui) {
        this.ui = ui;
        return this;
    }

    /** What the page uses, for the viewers' usage lookups. */
    public GTViewerCategory<T> inputs(Function<T, List<ViewerEntry>> inputs) {
        this.inputs = inputs;
        return this;
    }

    /** What the page produces or shows, for the viewers' recipe lookups. */
    public GTViewerCategory<T> outputs(Function<T, List<ViewerEntry>> outputs) {
        this.outputs = outputs;
        return this;
    }

    /** An item that, in the viewers, opens this page: the stack is added as a workstation or catalyst. */
    public GTViewerCategory<T> workstation(Supplier<ItemStack> stack) {
        this.workstations.add(() -> List.of(stack.get()));
        return this;
    }

    /**
     * Like {@link #workstation}, for a set of stacks worked out when the viewers load, such as every machine of a kind.
     */
    public GTViewerCategory<T> workstations(Supplier<List<ItemStack>> stacks) {
        this.workstations.add(stacks);
        return this;
    }

    /**
     * True to list this page's tab before the GT recipe categories in EMI and JEI, as the built-in info pages are. The
     * default puts it after them.
     */
    public GTViewerCategory<T> listFirst(boolean listFirst) {
        this.listFirst = listFirst;
        return this;
    }

    /** JEI lays its pages out with an upper bound on their size. EMI ignores this and uses the measured size. */
    public GTViewerCategory<T> maxSize(int width, int height) {
        this.maxWidth = width;
        this.maxHeight = height;
        return this;
    }

    /** False for pages whose contents are not craftable, so EMI leaves them out of its recipe tree. */
    public GTViewerCategory<T> recipeTree(boolean recipeTree) {
        this.recipeTree = recipeTree;
        return this;
    }

    /** True to stop EMI marking a produced item as craftable because of this page. */
    public GTViewerCategory<T> hideCraftable(boolean hideCraftable) {
        this.hideCraftable = hideCraftable;
        return this;
    }

    /** Checks the category is complete and returns it. */
    public GTViewerCategory<T> build() {
        if (entryId == null) throw new IllegalStateException("viewer category " + id + " needs id(...)");
        if (ui == null) throw new IllegalStateException("viewer category " + id + " needs ui(...)");
        return this;
    }

    public ResourceLocation id() {
        return id;
    }

    public Class<T> type() {
        return type;
    }

    public ItemStack icon() {
        return icon.get();
    }

    public Component title() {
        return title.get();
    }

    public boolean enabled() {
        return enabled.getAsBoolean();
    }

    /** The entries to show, after the visibility rules are applied. */
    @SuppressWarnings("unchecked")
    public List<T> visibleEntries() {
        return ((Collection<T>) entries.get()).stream()
                .filter(e -> GTViewerVisibility.isVisible(id, entryId.apply(e), e)).toList();
    }

    /** All the entries, hidden or not. */
    @SuppressWarnings("unchecked")
    public List<T> allEntries() {
        return List.copyOf((Collection<T>) entries.get());
    }

    public ResourceLocation entryId(T entry) {
        return entryId.apply(entry);
    }

    public IWidget ui(T entry) {
        return ui.apply(entry);
    }

    public List<ViewerEntry> inputs(T entry) {
        return inputs.apply(entry);
    }

    public List<ViewerEntry> outputs(T entry) {
        return outputs.apply(entry);
    }

    public List<ItemStack> workstations() {
        return workstations.stream().flatMap(s -> s.get().stream()).filter(s -> !s.isEmpty()).toList();
    }

    public boolean listFirst() {
        return listFirst;
    }

    public int maxWidth() {
        return maxWidth;
    }

    public int maxHeight() {
        return maxHeight;
    }

    public boolean recipeTree() {
        return recipeTree;
    }

    public boolean hideCraftable() {
        return hideCraftable;
    }
}
