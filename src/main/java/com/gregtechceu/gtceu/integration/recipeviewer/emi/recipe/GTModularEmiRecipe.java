package com.gregtechceu.gtceu.integration.recipeviewer.emi.recipe;

import com.gregtechceu.gtceu.api.recipe.gui.SizeAwareWidget;
import com.gregtechceu.gtceu.integration.recipeviewer.emi.EmiHostWidget;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import brachy.modularui.api.widget.IWidget;
import brachy.modularui.integration.emi.recipe.ModularUIEmiRecipe;
import brachy.modularui.screen.EmbedHandler;
import brachy.modularui.screen.ModularScreen;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.screen.RecipeScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A ModularUI-drawn EMI recipe that follows its panel's real size and can host EMI widgets, see {@link EmiHostWidget}.
 * Every GT recipe viewer page is built on this, so they all get both.
 */
public abstract class GTModularEmiRecipe extends ModularUIEmiRecipe {

    /** Smallest time between two re-layouts of the recipe screen caused by a panel changing size. */
    private static final long RELAYOUT_COOLDOWN_NANOS = 250_000_000L;

    private final Supplier<IWidget> ui;

    /** An {@link EmiHostWidget} in the laid-out panel, with where it sits. */
    private record HostSlot(EmiHostWidget host, int x, int y) {}

    private boolean measured = false;
    private Bounds bounds = new Bounds(0, 0, 0, 0);
    private List<HostSlot> hosts = List.of();
    private long lastRelayout = 0;

    protected GTModularEmiRecipe(ResourceLocation id, Supplier<IWidget> ui) {
        super(id, ui);
        this.ui = ui;
    }

    private static int debugBudget = 80;

    /** A short trace of how recipe pages are sized, for tracking down layout problems; it stops after a while. */
    private static void debug(String message, Object... args) {
        if (debugBudget-- > 0) com.gregtechceu.gtceu.GTCEu.LOGGER.info("[GTViewer size] " + message, args);
    }

    // *** Size ***

    /**
     * The panel's real size, measured from a laid-out copy and kept until a panel reports that it grew or shrank.
     * ModularUI measures only once, which is how recipes with extra rows used to be cut off.
     */
    @Override
    protected void calculateSize() {
        if (measured) return;
        measured = true;
        ModularScreen screen = createScreen(ui.get(), getId().getNamespace(), "emi_recipe_" + getId().getPath());
        bounds = adjustSize(EmbedHandler.getEmbedWidth(screen), EmbedHandler.getEmbedHeight(screen));
        hosts = findHosts(screen.getMainPanel());
        debug("measured {} -> {}x{}, panel at {},{}, {} host(s)", getId(), bounds.width(), bounds.height(),
                screen.getMainPanel().getArea().x, screen.getMainPanel().getArea().y, hosts.size());
    }

    /**
     * Last say on the size of the displayed recipe, given what the layout measured. Override to clamp or pad it.
     */
    protected Bounds adjustSize(int width, int height) {
        return new Bounds(0, 0, width, height);
    }

    @Override
    public Bounds getBounds() {
        calculateSize();
        return bounds;
    }

    @Override
    public int getDisplayWidth() {
        calculateSize();
        return bounds.width();
    }

    @Override
    public int getDisplayHeight() {
        calculateSize();
        return bounds.height();
    }

    /** Forgets the measured size, so the next query measures again. */
    public void invalidateSize() {
        measured = false;
    }

    /**
     * A displayed panel reported its real size, which can be larger than what was measured before anything was drawn:
     * text rows only get their height on their first draw. Takes the reported size, and finds the host widgets again
     * from that laid out panel, then asks EMI to lay the page out again.
     */
    private void onPanelResized(IWidget panelWidget, int width, int height) {
        Bounds reported = adjustSize(width, height);
        var panelArea = panelWidget.getPanel().getArea();
        debug("reported {} -> {}x{} (had {}x{}), panel at {},{}", getId(), reported.width(), reported.height(),
                bounds.width(), bounds.height(), panelArea.x, panelArea.y);
        if (reported.width() == bounds.width() && reported.height() == bounds.height()) return;
        measured = true;
        bounds = reported;
        hosts = findHosts(panelWidget.getPanel());
        long now = System.nanoTime();
        if (now - lastRelayout < RELAYOUT_COOLDOWN_NANOS) return;
        lastRelayout = now;
        Minecraft mc = Minecraft.getInstance();
        // EMI lays recipes out when its screen opens, so it has to be told to do it again
        if (mc.screen instanceof RecipeScreen screen) {
            mc.execute(
                    () -> screen.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight()));
        }
    }

    @Override
    public IWidget transformWidget(IWidget widget) {
        if (widget instanceof SizeAwareWidget aware) {
            aware.setSizeListener((w, h) -> onPanelResized(widget, w, h));
        }
        return super.transformWidget(widget);
    }

    // *** EMI widgets inside the panel ***

    private static List<HostSlot> findHosts(IWidget root) {
        List<HostSlot> found = new ArrayList<>();
        collectHosts(root, 0, 0, found);
        return found.isEmpty() ? List.of() : List.copyOf(found);
    }

    private static void collectHosts(IWidget widget, int originX, int originY, List<HostSlot> out) {
        int x = originX + widget.getArea().rx;
        int y = originY + widget.getArea().ry;
        if (widget instanceof EmiHostWidget host) out.add(new HostSlot(host, x, y));
        for (IWidget child : widget.getChildren()) {
            collectHosts(child, x, y, out);
        }
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        super.addWidgets(widgets);
        calculateSize();
        for (HostSlot slot : hosts) {
            slot.host().emit(widgets, slot.x(), slot.y());
        }
    }
}
