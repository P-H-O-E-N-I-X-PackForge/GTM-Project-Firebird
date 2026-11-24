package com.gregtechceu.gtceu.integration.recipeviewer.widgets;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.mui.MultiblockSchemaInfo;
import com.gregtechceu.gtceu.api.multiblock.MultiPredicate;
import com.gregtechceu.gtceu.api.multiblock.PredicateContext;
import com.gregtechceu.gtceu.api.multiblock.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.multiblock.pattern.ExpandablePattern;
import com.gregtechceu.gtceu.api.multiblock.pattern.IBlockPattern;
import com.gregtechceu.gtceu.api.multiblock.predicates.BasePredicate;
import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.client.renderer.PatternPreviewRenderer;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTDimensionMarkers;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.recipeviewer.widgets.MultiblockPreviewExtensions.Slot;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import brachy.modularui.api.IPanelHandler;
import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.api.drawable.IIcon;
import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IGuiAction;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.drawable.*;
import brachy.modularui.drawable.schema.BlockHighlight;
import brachy.modularui.integration.recipeviewer.RecipeSlotRole;
import brachy.modularui.integration.recipeviewer.RecipeViewerSlotWidget;
import brachy.modularui.screen.viewport.ModularGuiContext;
import brachy.modularui.utils.Alignment;
import brachy.modularui.utils.Color;
import brachy.modularui.value.BoolValue;
import brachy.modularui.value.IntValue;
import brachy.modularui.value.StringValue;
import brachy.modularui.value.sync.DynamicSyncHandler;
import brachy.modularui.widget.EmptyWidget;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.*;
import brachy.modularui.widgets.dynamic.DynamicHandler;
import brachy.modularui.widgets.dynamic.DynamicWidget;
import brachy.modularui.widgets.layout.Flow;
import brachy.modularui.widgets.menu.AbstractMenuButton;
import brachy.modularui.widgets.menu.ContextMenuButton;
import brachy.modularui.widgets.menu.Menu;
import brachy.modularui.widgets.menu.MenuPanel;
import brachy.modularui.widgets.textfield.TextFieldWidget;
import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

@Accessors(chain = true)
public class MultiblockPreviewWidget extends ParentWidget<MultiblockPreviewWidget> {

    @Getter
    private final MultiblockMachineDefinition multiblockDefinition;

    // schema stuff
    private DynamicSyncHandler partsViewWidget;
    // private final SchemaRenderer renderer;
    private final DynamicHandler partsHandler = new DynamicHandler();
    private final DynamicHandler selectedBlockHandler = new DynamicHandler();
    private final DynamicHandler structureErrorHandler = new DynamicHandler();
    private final DynamicHandler patternsHandler = new DynamicHandler();
    private final DynamicHandler layersHandler = new DynamicHandler();
    private final DynamicHandler keysHandler = new DynamicHandler();
    private int shownLayerCount = -1;
    private final Reference2IntMap<Block> blockCounts = new Reference2IntOpenHashMap<>();

    @Getter
    @Setter
    private MultiblockSchemaInfo multiblockSchemaInfo;

    @Getter
    @Setter
    private SchemaRenderer schemaRenderer;

    @Getter
    @Setter
    private boolean isFlipped = false;
    @Setter
    private Direction frontFacing;
    @Setter
    private Direction upFacing;
    @Setter
    private @Nullable BlockPos controllerPos;
    private SelectionInfo selectionInfo = SelectionInfo.empty();
    private @Nullable PreviewSchemaRenderer previewRenderer;
    /** The blocks hidden by variants that are switched off; replaced whole, since the render thread reads it. */
    private volatile Set<BlockPos> hiddenPositions = Set.of();
    private List<MultiblockPreviewExtensions.Variant> variants = List.of();
    private final Map<String, Boolean> variantShown = new LinkedHashMap<>();
    @Getter
    private boolean terminal;

    /** An item's tooltip with every line ended, so whatever is added after it starts on a line of its own. */
    private static void addItemTooltip(brachy.modularui.screen.RichTooltip text, ItemStack stack) {
        var lines = net.minecraft.client.gui.screens.Screen.getTooltipFromItem(
                net.minecraft.client.Minecraft.getInstance(), stack);
        if (lines.isEmpty()) return;
        text.addLine(lines.get(0));
        if (lines.size() > 1) text.spaceLine();
        for (int i = 1; i < lines.size(); i++) text.addLine(lines.get(i));
    }

    // what the player chose last stays for the next preview that is opened
    private static com.gregtechceu.gtceu.api.mui.PreviewMode defaultMode = com.gregtechceu.gtceu.api.mui.PreviewMode.FORMED;
    private static boolean defaultGrid = false;
    private static int defaultGraduation = 0;
    private static boolean defaultTurntable = false;
    private static final int[] GRADUATION_STEPS = { 0, 3, 5, 10 };

    private com.gregtechceu.gtceu.api.mui.PreviewMode mode = defaultMode;
    private boolean showGrid = defaultGrid;
    private int graduationIndex = defaultGraduation;
    private boolean turntable = defaultTurntable;

    private int yLevel = Integer.MAX_VALUE;

    @Setter
    private @Nullable Runnable onSchemaRefresh;
    @Getter
    private boolean clearMulti = false;
    @Getter
    private boolean clearPreferences = false;

    public MultiblockPreviewWidget(MultiblockMachineDefinition definition, MultiblockSchemaInfo schemaInfo, int width,
                                   int height, boolean terminal) {
        this.multiblockDefinition = definition;
        this.terminal = terminal;
        if (!GTCEu.isClientThread()) return;
        this.frontFacing = definition.getRotationState().defaultDirection;
        this.upFacing = switch (definition.getRotationState()) {
            case Y_AXIS -> Direction.NORTH;
            case ALL, NON_Y_AXIS, NONE -> Direction.UP;
        };
        this.multiblockSchemaInfo = schemaInfo == null ? new MultiblockSchemaInfo(definition) : schemaInfo;
        this.multiblockSchemaInfo.setPreviewMode(mode);
        if (!terminal) restoreStructureChoice();
        refreshSchema();

        PreviewSchemaRenderer renderer = new PreviewSchemaRenderer(this.multiblockSchemaInfo.getMapSchema());
        renderer.highlightRenderer(new BlockHighlight(Color.withAlpha(Color.GREEN.brighter(1), 0.9f), 1 / 32f));
        this.previewRenderer = renderer;
        renderer.setGrid(showGrid, GRADUATION_STEPS[graduationIndex]);
        schemaRenderer = renderer;
        this.variants = MultiblockPreviewExtensions.variantsFor(this);
        for (var variant : variants) variantShown.put(variant.id(), variant.shownByDefault());
        resolveHiddenBlocks();

        IGuiAction.MouseReleased setBlockOnClick = (ctx, m) -> {
            if (m == InputConstants.MOUSE_BUTTON_RIGHT) {
                BlockHitResult rayTrace = schemaRenderer.lastRayTrace();
                if (rayTrace != null && rayTrace.getType() == HitResult.Type.BLOCK) {
                    var level = this.multiblockSchemaInfo.getMapSchema().getLevel();
                    BlockState state = level.getBlockState(rayTrace.getBlockPos());
                    ItemStack picked = state.getCloneItemStack(rayTrace, level, rayTrace.getBlockPos(),
                            this.getContext().getMC().player);
                    return com.gregtechceu.gtceu.integration.recipeviewer.GTViewerLookup.show(picked,
                            net.minecraft.client.gui.screens.Screen.hasShiftDown());
                }
                return false;
            }
            if (m == InputConstants.MOUSE_BUTTON_LEFT) {
                BlockHitResult rayTrace = schemaRenderer.lastRayTrace();
                if (rayTrace != null && rayTrace.getType() == HitResult.Type.BLOCK) {
                    BlockState state = this.multiblockSchemaInfo.getMapSchema().getLevel()
                            .getBlockState(rayTrace.getBlockPos());
                    this.selectionInfo = SelectionInfo.of(rayTrace, state);
                    if (previewRenderer != null) {
                        previewRenderer.setLayer("selection", List.of(rayTrace.getBlockPos()), 0xFFFFD34D);
                    }
                    this.selectedBlockHandler.notifyUpdate();
                    MultiblockPreviewExtensions.fireSelectionChanged(this);
                    return true;
                }
            }
            return false;
        };

        this.partsHandler.widgetProvider(() -> Flow.col()
                .name("wrapping_parts_col")
                // NOTE wrapped flows require a fixed size in their axis, relative/coverChildren does not work
                .wrap()
                .coverChildrenWidth(26)
                .childPadding(1)
                .height(height)
                .children(this.multiblockSchemaInfo.getBlockCounts().reference2IntEntrySet(), e -> {
                    ItemStack stack = new ItemStack(e.getKey(), e.getIntValue());
                    return RecipeViewerSlotWidget.create(ItemStack.class)
                            .recipeSlotRole(RecipeSlotRole.OUTPUT)
                            .value(stack)
                            .background(IDrawable.EMPTY)
                            .size(16)
                            // .margin(1)
                            .tooltip(r -> r.addFromItem(stack));
                })
                .horizontalCenter());

        this.selectedBlockHandler.widgetProvider(() -> {
            ItemStack selected = this.selectionInfo.stack();
            if (selected.isEmpty()) return null;

            IBlockPattern pattern = this.multiblockSchemaInfo.getPattern();
            if (pattern instanceof BlockPattern blockPattern) {
                try {
                    MultiPredicate predicate = this.multiblockSchemaInfo.getStructureHelper().getPredicateFromPos(
                            blockPattern, this.selectionInfo.pos(), frontFacing, upFacing, isFlipped);
                    return createSelectedBlockMenu(predicate);
                } catch (RuntimeException e) {
                    return null;
                }
            } else if (pattern instanceof ExpandablePattern expandablePattern) {
                try {
                    MultiPredicate predicate = this.multiblockSchemaInfo.getStructureHelper().getPredicateFromPos(
                            expandablePattern, this.selectionInfo.pos(), frontFacing, upFacing, isFlipped);
                    return createSelectedBlockMenu(predicate);
                } catch (RuntimeException e) {
                    return null;
                }
            }
            return null;
        });

        this.structureErrorHandler.widgetProvider(() -> {
            var schema = this.multiblockSchemaInfo.getMapSchema();
            var controllerPos = schema.getControllerPos();
            if (schema.getLevel().getBlockEntity(controllerPos) instanceof MultiblockControllerMachine controller) {
                var structureState = controller.getPatternState(this.multiblockSchemaInfo.getStructure());
                if (structureState == null || structureState.isFormed()) {
                    return new EmptyWidget();
                }
                return new Icon(GTGuiTextures.INFO).asWidget()
                        .tooltip(r -> {
                            var patternState = controller
                                    .getPatternState(this.multiblockSchemaInfo.getStructure());
                            if (!patternState.hasErrors()) return;
                            Flow col = Flow.col();
                            for (var error : patternState.getErrors()) {
                                error.getPatternErrorUIModifier().apply(col);
                            }
                            col.getChildren().forEach(child -> {
                                if (child instanceof IDrawable drawableChild) r.addDrawableLine(drawableChild);
                                else if (child instanceof TextWidget<?> textWidget) r.addLine(textWidget.getKey());
                            });
                            // r.addLine(Component.literal("Structure is unformed"));
                        });
            }
            return new EmptyWidget();
        });

        this.layersHandler.widgetProvider(() -> createLayerSlider(width));

        this.patternsHandler.widgetProvider(() -> {
            Flow patternColumn = Flow.col().name("structure_pattern").coverChildren();
            IBlockPattern shown = this.multiblockSchemaInfo.getPattern();
            if (shown instanceof BlockPattern blockPattern) {
                createSliceSliders(patternColumn, blockPattern);
            } else if (shown instanceof ExpandablePattern expandablePattern) {
                createConstraintSliders(patternColumn, expandablePattern);
            }
            return patternColumn;
        });

        // the keys of the structure, each a menu of the blocks it can be, in a column at the left of the view
        this.keysHandler.widgetProvider(() -> {
            Flow keys = Flow.col().name("keys").coverChildren().childPadding(2);
            if (this.multiblockSchemaInfo.getPattern() instanceof BlockPattern blockPattern) {
                createPredicateMenus(keys, blockPattern);
            }
            return keys;
        });

        schemaRenderer.camera().setPosAndLookAt(0, 0, 0,
                this.multiblockSchemaInfo.getMapSchema().getCenter());
        PredicateContext context = new PredicateContext(null);
        // the turntable turns the view a little each frame
        SchemaWidget schema = new SchemaWidget(schemaRenderer) {

            {
                // the middle of the blocks, not of their corners, and far enough away to have all of it in view
                offset(0.5f, 0.5f, 0.5f);
                scale(fitDistance());
            }

            private long lastFrame;

            @Override
            public void draw(ModularGuiContext context, brachy.modularui.theme.WidgetThemeEntry<?> widgetTheme) {
                if (turntable) {
                    long now = System.nanoTime();
                    if (lastFrame != 0) yaw(getYaw() + (now - lastFrame) / 1_000_000_000f * 0.6f);
                    lastFrame = now;
                } else {
                    lastFrame = 0;
                }
                super.draw(context, widgetTheme);
            }
        }

                .listenGuiAction(setBlockOnClick)
                .tooltipDynamic(text -> {
                    BlockHitResult hit = schemaRenderer.lastRayTrace();
                    if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                        BlockState state = this.getMultiblockSchemaInfo().getMapSchema().getLevel()
                                .getBlockState(hit.getBlockPos());
                        ItemStack pickedItem = state.getCloneItemStack(hit,
                                this.getMultiblockSchemaInfo().getMapSchema().getLevel(), hit.getBlockPos(),
                                this.getContext().getMC().player);
                        addItemTooltip(text, pickedItem);
                        IBlockPattern value = this.multiblockSchemaInfo.getPattern();
                        context.updateLevel(this.multiblockSchemaInfo.getMapSchema().getLevel());
                        context.updatePos(hit.getBlockPos());
                        try {
                            MultiPredicate root = Objects.requireNonNull(this.multiblockSchemaInfo.getStructureHelper())
                                    .getPredicateFromPos(value, hit.getBlockPos(), frontFacing, upFacing, isFlipped);
                            root.resetLogic();
                            BasePredicate predicate = root.getPredicateAtPos(context).match();
                            if (predicate != null) {
                                var extra = predicate.getRecipeViewerTooltips(root);
                                if (!extra.isEmpty()) text.spaceLine();
                                extra.forEach(text::addLine);
                            }
                            text.addLine(Component.translatable("gtceu.multiblock.preview.right_click_recipes")
                                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
                        } catch (RuntimeException ignored) {
                            // a block of an overlaid structure
                        }
                    }
                }).tooltipAutoUpdate(true)
                .size(width, height);

        schema.getSchemaRenderer().updateRenderFilter((pos, state) -> {
            if (hiddenPositions.contains(pos)) return false;
            if (yLevel == Integer.MAX_VALUE) {
                return true;
            }
            return pos.getY() <= yLevel;
        });

        // the buttons of the view, after the others
        final int viewButtons = 45 + (definition.getStructurePatterns().size() > 1 ? 19 : 0) +
                (variants.isEmpty() ? 0 : 19);
        this.coverChildren()
                .padding(7)
                .childIf(terminal, () -> new ButtonWidget<>()
                        .overlay(new ItemDrawable(GTDimensionMarkers.OVERWORLD.value().getIcon()).asIcon().size(16))
                        .tooltip(r -> {
                            r.addLine(Component.translatable("gtceu.multiblock.preview.in_world"));
                            r.addLine(Component.translatable("gtceu.multiblock.preview.in_world.hint"));
                        })
                        .rightRel(1.0f)
                        .onMousePressed((c, b) -> {
                            if (controllerPos != null &&
                                    !this.getMultiblockSchemaInfo().getStructureBlocks().isEmpty()) {
                                BlockPos origin = controllerPos.offset(
                                        this.getMultiblockSchemaInfo().getMapSchema().getControllerPos().multiply(-1));
                                PatternPreviewRenderer.INSTANCE.showPreview(origin,
                                        this.getMultiblockSchemaInfo().getMapSchema(),
                                        schema.getSchemaRenderer().renderFilter(),
                                        ConfigHolder.INSTANCE.client.inWorldPreviewDuration * 20);
                            }
                            return true;
                        }))
                // todo serialize these values as part of the schema nbt
                .child(new ToggleButton()
                        .overlay(GTGuiTextures.PROGRESS_MIXER[1].asIcon().size(16))
                        .tooltipDynamic(r -> {
                            r.addLine(Component.translatable("gtceu.multiblock.preview.flip"));
                            r.addLine(Component.translatable(isFlipped ? "gtceu.multiblock.preview.flip.on" :
                                    "gtceu.multiblock.preview.flip.off"));
                        })
                        .tooltipAutoUpdate(true)
                        .left(26)
                        .value(new BoolValue.Dynamic(() -> isFlipped, v -> {
                            setFlipped(!isFlipped);
                            refreshSchema();
                            notifyWidgetHandlers();
                        })))
                .childIf(definition.getStructurePatterns().size() > 1, this::createStructuresButton)
                .childIf(!variants.isEmpty(), this::createVariantsMenu)
                .child(createModeButton().left(viewButtons))
                .childIf(
                        !terminal && definition.getRotationState() != com.gregtechceu.gtceu.api.data.RotationState.NONE,
                        () -> createRotateButton().left(viewButtons + 19))
                .child(createGridButton().left(viewButtons + (terminal ? 19 : 38)))
                .child(createTurntableButton().left(viewButtons + (terminal ? 38 : 57)))
                .childIf(MultiblockPreviewExtensions.has(Slot.TOOLBAR, definition), () -> Flow.row()
                        .name("toolbar_extensions")
                        .left(viewButtons + (terminal ? 57 : 76))
                        .coverChildren()
                        .childPadding(1)
                        .children(MultiblockPreviewExtensions.build(Slot.TOOLBAR, this), w -> w))
                .child(Flow.col()
                        .name("main")
                        .coverChildren()
                        // on top, under the toolbar: the selected block, and the sizes of the repeating parts
                        .child(Flow.row()
                                .name("top_row")
                                .marginTop(22)
                                .coverChildren()
                                .childPadding(4)
                                .crossAxisAlignment(Alignment.CrossAxis.START)
                                .child(new DynamicWidget<>()
                                        .name("selected_block")
                                        .coverChildren(20)
                                        .clientOnlyHandler(this.selectedBlockHandler))
                                .child(new DynamicWidget<>()
                                        .name("structure_patterns")
                                        .coverChildren(20)
                                        .clientOnlyHandler(this.patternsHandler)))
                        .child(Flow.row()
                                .name("schema_widgets")
                                .crossAxisAlignment(Alignment.CrossAxis.START)
                                .coverChildren()
                                .childPadding(2)
                                .child(new DynamicWidget<>()
                                        .name("keys")
                                        .coverChildren()
                                        .clientOnlyHandler(this.keysHandler))
                                .childIf(MultiblockPreviewExtensions.has(Slot.LEFT, definition),
                                        () -> extensionColumn(Slot.LEFT))
                                .child(new ParentWidget<>()
                                        .coverChildren()
                                        .background(GTGuiTextures.BACKGROUND_INVERSE)
                                        .padding(3)
                                        .child(schema)
                                        .childIf(MultiblockPreviewExtensions.has(Slot.OVERLAY, definition),
                                                () -> extensionOverlay()))
                                .child(new DynamicWidget<>()
                                        .background(GTGuiTextures.BACKGROUND)
                                        .coverChildrenWidth()
                                        .heightRel(1f)
                                        .name("parts_view")
                                        .clientOnlyHandler(partsHandler))
                                .childIf(MultiblockPreviewExtensions.has(Slot.RIGHT, definition),
                                        () -> extensionColumn(Slot.RIGHT)))
                        .child(new DynamicWidget<>()
                                .name("layers")
                                .widthRel(1f)
                                .coverChildrenHeight()
                                .clientOnlyHandler(this.layersHandler))
                        .childIf(MultiblockPreviewExtensions.has(Slot.BELOW, definition),
                                () -> extensionColumn(Slot.BELOW)))
                .childIf(terminal, () -> new ButtonWidget<>()
                        .right(5)
                        .overlay(new DrawableStack(new ItemDrawable(GTMultiMachines.ELECTRIC_BLAST_FURNACE.asStack()),
                                new ItemDrawable(Items.BARRIER)))
                        .onMousePressed((guiContext, button) -> {
                            clearMulti = true;
                            ((ModularGuiContext) guiContext).getScreen().getMainPanel().closeIfOpen();
                            return true;
                        })
                        .tooltip(r -> r.addLine(Component.translatable("gtceu.terminal.clear_multi"))))
                .childIf(terminal, () -> new ButtonWidget<>()
                        .right(24)
                        .overlay(new DrawableStack(new ItemDrawable(GTBlocks.COIL_CUPRONICKEL),
                                new ItemDrawable(Items.BARRIER)))
                        .onMousePressed((guiContext, button) -> {
                            clearPreferences = true;
                            ((ModularGuiContext) guiContext).getScreen().getMainPanel().closeIfOpen();
                            return true;
                        })
                        .tooltip(r -> r.addLine(Component.translatable("gtceu.terminal.clear_preference"))))
                .childIf(terminal, () -> new ToggleButton()
                        .right(43)
                        .overlay(new DrawableStack(new ItemDrawable(GTMachines.ITEM_EXPORT_BUS[0].asStack()),
                                new ItemDrawable(Items.BARRIER)))
                        .value(new BoolValue.Dynamic(() -> this.multiblockSchemaInfo.isIgnorePreviewCount(), value -> {
                            this.multiblockSchemaInfo.setIgnorePreviewCount(value);
                            refreshSchema();
                            notifyWidgetHandlers();
                        }))
                        .tooltip(r -> r.addLine(Component.translatable("gtceu.terminal.ignore_preview_count"))))
                .child(new DynamicWidget<>()
                        .right(-20)
                        .clientOnlyHandler(structureErrorHandler));
    }

    private ContextMenuButton<?> createSelectedBlockMenu(MultiPredicate predicate) {
        // TODO this can throw invalid state exception when
        // opening after clicking on a block twice
        return new ContextMenuButton<>(this.selectionInfo.pos().toString())
                .size(20)
                .overlay(new ItemDrawable(this.selectionInfo.stack()).asIcon().center())
                .tooltip(text -> text.addFromItem(this.selectionInfo.stack()))
                .requiresClick()
                .menuList(l -> l
                        .maxSize(80)
                        .coverChildrenWidth()
                        .collapseDisabledChildren()
                        .childSeparator(Icon.EMPTY_2PX)
                        .children(predicate.expand(), basePredicate -> {
                            List<BlockInfo> candidates = basePredicate.getCandidates();
                            if (candidates.isEmpty())
                                return new EmptyWidget();
                            if (candidates.size() > 1) {
                                return new ContextMenuButton<>(basePredicate.getTypeName())
                                        .size(16)
                                        .tooltip(r -> r.add(basePredicate.getTypeName()))
                                        .overlay(new ItemDrawable(
                                                candidates.get(0).getItemStackForm()))
                                        .requiresClick()
                                        .openRightDown()
                                        .menu(candidateGrid(candidates, blockInfo -> setUserDefinedBlockInfo(
                                                this.selectionInfo.pos(), blockInfo)));
                            } else {
                                return new ToggleButton()
                                        .value(new BoolValue.Dynamic(() -> false,
                                                (b) -> setUserDefinedBlockInfo(this.selectionInfo.pos(),
                                                        candidates.get(0))))
                                        .size(16)
                                        .tooltip(r -> r.add(
                                                basePredicate.getCandidates().get(0).getItemStackForm().getHoverName()))
                                        .overlay(new ItemDrawable(
                                                candidates.get(0).getItemStackForm()));
                            }
                        }));
    }

    private void createPredicateMenus(Flow predicatesRow, BlockPattern blockPattern) {
        for (var entry : blockPattern.getPredicates().char2ObjectEntrySet()) {
            MultiPredicate predicate = entry.getValue();
            char key = entry.getCharKey();
            // todo figure out sliders needed for predicate min/max depending on base predicates in the
            // main predicate
            if (predicate.isAny() || predicate.isAir()) {
                continue;
            }
            IDrawable overlay;
            if (predicate.getCandidates().size() == 1 && predicate.getCandidates().get(0).size() == 1) {
                continue;
            } else {
                overlay = new DynamicDrawable(() -> {
                    BlockInfo info = multiblockSchemaInfo.getBlockPreferences().get(entry.getCharKey());
                    return new ItemDrawable(info == null ?
                            entry.getValue().getCandidates().get(0).get(0).getItemStackForm() :
                            info.getItemStackForm())
                            .asIcon()
                            .size(16);
                });
                // overlay = Text.str(String.valueOf(entry.getCharKey())).asIcon().size(8).center();
            }

            var menu = new ContextMenuButton<>(String.valueOf(entry.getCharKey()))
                    .overlay(overlay)
                    .tooltip(text -> {
                        if (overlay instanceof IIcon icon && icon.getWrappedDrawable() instanceof ItemDrawable item) {
                            ItemStack stack = item.getItemList().get(0);
                            text.addFromItem(stack);
                        }
                        text.addLine(Text.str("Multiblock Key: %s", String.valueOf(entry.getCharKey())));
                    })
                    .size(20)
                    .requiresClick()
                    .menuList(l -> l
                            .maxSize(80)
                            .coverChildrenWidth()
                            .collapseDisabledChildren()
                            .childSeparator(Icon.EMPTY_2PX)
                            .children(predicate.expand(), basePredicate -> {
                                List<BlockInfo> candidates = basePredicate.getCandidates();
                                if (candidates.isEmpty()) {
                                    return new EmptyWidget();
                                } else if (candidates.size() > 1) {
                                    return createInnerPredicateMenu(key, predicate, basePredicate, candidates);
                                } else {
                                    return new ToggleButton()
                                            .value(new BoolValue.Dynamic(() -> false,
                                                    (b) -> setPredicateDefaultBlock(key,
                                                            candidates.get(0))))
                                            .size(16)
                                            .tooltip(r -> r.add(
                                                    basePredicate.getCandidates().get(0).getItemStackForm()
                                                            .getHoverName()))
                                            .overlay(new ItemDrawable(
                                                    candidates.get(0).getItemStackForm()));
                                }
                            }));
            predicatesRow.child(menu);
        }
    }

    private ContextMenuButton<?> createInnerPredicateMenu(char predicateKey, MultiPredicate predicate,
                                                          BasePredicate basePredicate,
                                                          List<BlockInfo> candidates) {
        return new ContextMenuButton<>(basePredicate.toString())
                .size(16)
                .tooltip(r -> r.add(basePredicate.getTypeName()))
                .overlay(new ItemDrawable(
                        candidates.get(0).getItemStackForm()))
                .requiresClick()
                .openRightDown()
                .menu(candidateGrid(candidates, blockInfo -> setPredicateDefaultBlock(predicateKey, blockInfo)));
    }

    /** The blocks a part can be, in rows as wide as they need to be, which beats one long column of them. */
    private static Menu<?> candidateGrid(List<BlockInfo> candidates, Consumer<BlockInfo> choose) {
        int columns = Mth.clamp((int) Math.ceil(Math.sqrt(candidates.size() * 1.5)), 3, 8);
        int width = columns * 18 + 6;
        return sizedMenu(width, list -> {
            list.coverChildrenHeight().childSeparator(Icon.EMPTY_2PX);
            for (int i = 0; i < candidates.size(); i += columns) {
                Flow row = Flow.row().coverChildren().childPadding(2);
                for (int j = i; j < Math.min(candidates.size(), i + columns); j++) {
                    BlockInfo blockInfo = candidates.get(j);
                    Component stackName = blockInfo.getItemStackForm().getHoverName();
                    row.child(new ToggleButton()
                            .value(new BoolValue.Dynamic(() -> false, b -> choose.accept(blockInfo)))
                            .size(16)
                            .tooltip(r -> r.add(stackName))
                            .overlay(new ItemDrawable(blockInfo.getItemStackForm())));
                }
                list.child(row);
            }
        });
    }

    /// ==== Schema setup ====
    @ApiStatus.Internal
    public void refreshSchema() {
        this.multiblockSchemaInfo.refreshSchema(multiblockDefinition, frontFacing, upFacing, isFlipped,
                onSchemaRefresh);
        resolveHiddenBlocks();
        MultiblockPreviewExtensions.fireSchemaChanged(this);
    }

    // *** Structures ***

    private record StructureChoice(String structure, List<String> overlays) {}

    private static final Map<net.minecraft.resources.ResourceLocation, StructureChoice> CHOICES = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * The recipe viewers build the page again when it is scrolled or its tab is changed, so the structure picked in a
     * viewer page is kept per multiblock, for the rest of the session. The terminal keeps its choice on the item.
     */
    private void rememberStructureChoice() {
        if (terminal) return;
        CHOICES.put(multiblockDefinition.getId(),
                new StructureChoice(multiblockSchemaInfo.getStructure(), multiblockSchemaInfo.getOverlays()));
    }

    private void restoreStructureChoice() {
        StructureChoice choice = CHOICES.get(multiblockDefinition.getId());
        if (choice == null) return;
        multiblockSchemaInfo.selectStructure(choice.structure());
        for (String name : choice.overlays()) multiblockSchemaInfo.setOverlay(name, true);
    }

    /** The name of the structure being shown, see {@code MultiblockMachineBuilder#pattern}. */
    public String getStructure() {
        return multiblockSchemaInfo.getStructure();
    }

    /** Shows another of the multiblock's structures. Its sizes and block choices start over. */
    public void setStructure(String name) {
        if (name.equals(getStructure())) return;
        multiblockSchemaInfo.selectStructure(name);
        rememberStructureChoice();
        selectionInfo = SelectionInfo.empty();
        if (previewRenderer != null) previewRenderer.clearLayer("selection");
        yLevel = Integer.MAX_VALUE;
        selectedBlockHandler.notifyUpdate();
        patternsHandler.notifyUpdate();
        keysHandler.notifyUpdate();
        applyFilters();
        if (schemaRenderer != null) {
            schemaRenderer.camera().setPosAndLookAt(0, 0, 0, multiblockSchemaInfo.getMapSchema().getCenter());
        }
        MultiblockPreviewExtensions.fireSelectionChanged(this);
    }

    /** Shows another of the multiblock's structures on top of the selected one, or stops showing it. */
    public void setOverlay(String name, boolean on) {
        if (multiblockSchemaInfo.isOverlay(name) == on) return;
        multiblockSchemaInfo.setOverlay(name, on);
        rememberStructureChoice();
        applyFilters();
    }

    private @Nullable IPanelHandler structuresPanel;

    /**
     * The toolbar's S button, which opens a small panel to choose the structure from. The panel is made the way the
     * auto-output side menu is, which is known to take clicks, and not with {@code ContextMenuButton}, whose list
     * menu is as wide as its button.
     */
    private ButtonWidget<?> createStructuresButton() {
        ButtonWidget<?> button = new ButtonWidget<>();
        button.size(16)
                .left(45)
                .overlay(Text.str("S").asIcon())
                .tooltip(r -> r.addLine(Component.translatable("gtceu.preview.structures")))
                .onMousePressed((context, mouse) -> {
                    if (structuresPanel == null) {
                        structuresPanel = IPanelHandler.simple(button.getPanel(), (parent, player) -> {
                            Menu<?> menu = buildStructureMenu();
                            menu.resizer().relative(button);
                            AbstractMenuButton.Direction.RIGHT_DOWN.apply(menu);
                            return new MenuPanel("structures", menu);
                        }, true);
                    }
                    if (structuresPanel.isPanelOpen()) structuresPanel.closePanel();
                    else structuresPanel.openPanel();
                    return true;
                });
        return button;
    }

    /**
     * The structures to choose from, one to a line: the name shows that structure, the plus beside it shows it on top
     * of the selected one.
     */
    private Menu<?> buildStructureMenu() {
        var font = Minecraft.getInstance().font;
        int nameWidth = 30;
        for (String name : multiblockDefinition.getStructurePatterns().keySet()) {
            nameWidth = Math.max(nameWidth, font.width(name) + 8);
        }
        Flow column = Flow.col().coverChildren().childPadding(2);
        for (String name : multiblockDefinition.getStructurePatterns().keySet()) {
            column.child(Flow.row().coverChildren().childPadding(2)
                    .child(com.gregtechceu.gtceu.api.recipe.gui.viewer.GTViewerParts.unique(new ToggleButton()
                            .size(nameWidth, 14)
                            .value(new BoolValue.Dynamic(() -> name.equals(getStructure()), shown -> {
                                if (shown) setStructure(name);
                            }))
                            .tooltip(r -> r.addLine(Component.translatable("gtceu.preview.structure_show")))
                            .overlay(Text.str(name).asIcon().size(font.width(name) + 2, 8))))
                    .child(com.gregtechceu.gtceu.api.recipe.gui.viewer.GTViewerParts.unique(new ToggleButton()
                            .size(14, 14)
                            .value(new BoolValue.Dynamic(() -> multiblockSchemaInfo.isOverlay(name),
                                    on -> setOverlay(name, on)))
                            .tooltip(r -> r.addLine(Component.translatable("gtceu.preview.structure_overlay")))
                            .overlay(Text.str("+").asIcon().size(font.width("+") + 2, 8)))));
        }
        return new Menu<>().coverChildren().padding(2).child(column);
    }

    /**
     * A menu of a fixed width. The menu of {@code ContextMenuButton#menuList} is as wide as its button, which is too
     * narrow for anything with text in it.
     */
    private static Menu<?> sizedMenu(int width, Consumer<ListWidget<IWidget, ?>> content) {
        ListWidget<IWidget, ?> list = new ListWidget<>().widthRel(1f);
        content.accept(list);
        return new Menu<>().width(width).coverChildrenHeight().child(list);
    }

    // *** Variants ***

    /** The parts of this multiblock that can be shown or hidden. */
    public List<MultiblockPreviewExtensions.Variant> getVariants() {
        return variants;
    }

    public boolean isVariantShown(String id) {
        return variantShown.getOrDefault(id, true);
    }

    /** Shows or hides a variant: its blocks leave or join the 3D view and the parts list. */
    public void setVariantShown(String id, boolean shown) {
        if (isVariantShown(id) == shown) return;
        variantShown.put(id, shown);
        applyFilters();
    }

    private void resolveHiddenBlocks() {
        if (variants.isEmpty()) return;
        Set<BlockPos> hidden = new HashSet<>();
        for (var variant : variants) {
            if (!isVariantShown(variant.id())) hidden.addAll(variant.positions().apply(this));
        }
        hiddenPositions = Set.copyOf(hidden);
    }

    /** Rebuilds the 3D view and the parts list after something that hides blocks changed: a layer or a variant. */
    // *** the view's buttons ***

    private ButtonWidget<?> createModeButton() {
        return new ButtonWidget<>()
                .overlay(new DynamicDrawable(() -> Text.dynamic(() -> Component.literal(mode.label)).scale(0.7f)
                        .asIcon()))
                .tooltipDynamic(r -> {
                    r.addLine(Component.translatable("gtceu.multiblock.preview.mode"));
                    r.addLine(Component.translatable(mode.tooltipKey));
                })
                .tooltipAutoUpdate(true)
                .onMousePressed((c, b) -> {
                    setPreviewMode(b == InputConstants.MOUSE_BUTTON_RIGHT ? mode.previous() : mode.next());
                    return true;
                });
    }

    private ButtonWidget<?> createRotateButton() {
        return new ButtonWidget<>()
                .overlay(Text.str("R").asIcon())
                .tooltip(r -> {
                    r.addLine(Component.translatable("gtceu.multiblock.preview.rotate"));
                    r.addLine(Component.translatable("gtceu.multiblock.preview.rotate.hint"));
                })
                .onMousePressed((c, b) -> {
                    rotate(b == InputConstants.MOUSE_BUTTON_RIGHT);
                    return true;
                });
    }

    private ButtonWidget<?> createGridButton() {
        return new ButtonWidget<>()
                .overlay(new DynamicDrawable(() -> Text.dynamic(() -> Component.literal(showGrid ? "G" : "g"))
                        .asIcon()))
                .tooltipDynamic(r -> {
                    r.addLine(Component.translatable(showGrid ? "gtceu.multiblock.preview.grid.on" :
                            "gtceu.multiblock.preview.grid.off"));
                    int step = GRADUATION_STEPS[graduationIndex];
                    r.addLine(step == 0 ? Component.translatable("gtceu.multiblock.preview.grid.no_marks") :
                            Component.translatable("gtceu.multiblock.preview.grid.marks", step));
                    r.addLine(Component.translatable("gtceu.multiblock.preview.grid.hint"));
                })
                .tooltipAutoUpdate(true)
                .onMousePressed((c, b) -> {
                    if (b == InputConstants.MOUSE_BUTTON_RIGHT) {
                        graduationIndex = (graduationIndex + 1) % GRADUATION_STEPS.length;
                        defaultGraduation = graduationIndex;
                        // a marked grid is of no use when it is off
                        if (!showGrid) {
                            showGrid = true;
                            defaultGrid = true;
                        }
                    } else {
                        showGrid = !showGrid;
                        defaultGrid = showGrid;
                    }
                    if (previewRenderer != null) {
                        previewRenderer.setGrid(showGrid, GRADUATION_STEPS[graduationIndex]);
                    }
                    return true;
                });
    }

    private ToggleButton createTurntableButton() {
        return new ToggleButton()
                .overlay(Text.str("T").asIcon())
                .tooltip(r -> r.addLine(Component.translatable("gtceu.multiblock.preview.turntable")))
                .value(new BoolValue.Dynamic(() -> turntable, on -> {
                    turntable = on;
                    defaultTurntable = on;
                }));
    }

    /** How far the view has to be from the structure for all of it to be in it. */
    private float fitDistance() {
        var bounds = multiblockSchemaInfo.getMapSchema().getBounds();
        if (bounds == null) return 10f;
        double dx = bounds.getSecond().getX() - bounds.getFirst().getX() + 1;
        double dy = bounds.getSecond().getY() - bounds.getFirst().getY() + 1;
        double dz = bounds.getSecond().getZ() - bounds.getFirst().getZ() + 1;
        // the view is 60 degrees, so a sphere round the structure fits at twice its radius, which is its diagonal
        return (float) Math.max(3, Math.sqrt(dx * dx + dy * dy + dz * dz) * 1.15);
    }

    private void setPreviewMode(com.gregtechceu.gtceu.api.mui.PreviewMode newMode) {
        this.mode = newMode;
        defaultMode = newMode;
        multiblockSchemaInfo.setPreviewMode(newMode);
        refreshSchema();
        notifyWidgetHandlers();
    }

    /** Turns the structure a quarter turn about the vertical, which the machine's rotation allows. */
    private void rotate(boolean reverse) {
        var rotation = multiblockDefinition.getRotationState();
        if (rotation == com.gregtechceu.gtceu.api.data.RotationState.NONE) return;
        Direction[] ring = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };
        // a machine that faces up or down is turned by which way its top points
        boolean byUp = rotation == com.gregtechceu.gtceu.api.data.RotationState.Y_AXIS;
        Direction current = byUp ? upFacing : frontFacing;
        int index = Arrays.asList(ring).indexOf(current);
        Direction next = ring[Math.floorMod((index < 0 ? 0 : index) + (reverse ? -1 : 1), ring.length)];
        if (byUp) upFacing = next;
        else frontFacing = next;
        selectionInfo = SelectionInfo.empty();
        if (previewRenderer != null) previewRenderer.clearLayer("selection");
        refreshSchema();
        selectedBlockHandler.notifyUpdate();
        notifyWidgetHandlers();
    }

    private void applyFilters() {
        refreshSchema();
        Reference2IntMap<Block> counts = new Reference2IntOpenHashMap<>();
        for (var entry : this.multiblockSchemaInfo.getStructureBlocks().entrySet()) {
            BlockPos pos = entry.getKey();
            if (hiddenPositions.contains(pos) || pos.getY() > yLevel) continue;
            counts.merge(entry.getValue().getBlockState().getBlock(), 1, Integer::sum);
        }
        this.multiblockSchemaInfo.setBlockCounts(counts);
        notifyWidgetHandlers();
    }

    private int variantsMenuWidth() {
        int width = 40;
        for (var variant : variants) {
            width = Math.max(width, Minecraft.getInstance().font.width(variant.label()) + 12);
        }
        return width + 4;
    }

    private ContextMenuButton<?> createVariantsMenu() {
        return new ContextMenuButton<>("variants")
                .size(16)
                .left(multiblockDefinition.getStructurePatterns().size() > 1 ? 64 : 45)
                .overlay(Text.str("V").asIcon())
                .tooltip(r -> r.addLine(Component.translatable("gtceu.preview.variants")))
                .requiresClick()
                .openRightDown()
                .menu(sizedMenu(variantsMenuWidth(), list -> list
                        .coverChildrenHeight()
                        .childSeparator(Icon.EMPTY_2PX)
                        .children(variants, variant -> new ToggleButton()
                                .size(variantsMenuWidth() - 4, 14)
                                .value(new BoolValue.Dynamic(() -> isVariantShown(variant.id()),
                                        shown -> setVariantShown(variant.id(), shown)))
                                .overlay(Text.of(variant.label()).asIcon()
                                        .size(Minecraft.getInstance().font.width(variant.label()) + 2, 8)))));
    }

    // *** For widgets added with MultiblockPreviewExtensions ***

    /** Whether a block is selected, by clicking it in the 3D view. */
    public boolean hasSelection() {
        return !selectionInfo.stack().isEmpty();
    }

    /** The position of the selected block in the preview's world, see {@link #toPreviewPos}. */
    public @Nullable BlockPos getSelectedPos() {
        return hasSelection() ? selectionInfo.pos() : null;
    }

    /** The selected block's state, or air if none is selected. */
    public BlockState getSelectedState() {
        return hasSelection() ? selectionInfo.state() : Blocks.AIR.defaultBlockState();
    }

    /** The selected block as an item, or an empty stack if none is selected. */
    public ItemStack getSelectedStack() {
        return hasSelection() ? selectionInfo.stack() : ItemStack.EMPTY;
    }

    /**
     * Outlines blocks in the 3D view, in a layer of your own: setting the layer again replaces what it outlined.
     *
     * @param layer     a name for the layer, to change or clear it later; use your mod id in it
     * @param positions positions in the preview's world, see {@link #toPreviewPos}
     * @param argb      the colour, with alpha
     */
    public void highlight(String layer, Collection<BlockPos> positions, int argb) {
        if (previewRenderer != null) previewRenderer.setLayer(layer, positions, argb);
    }

    public void clearHighlight(String layer) {
        if (previewRenderer != null) previewRenderer.clearLayer(layer);
    }

    public void clearHighlights() {
        if (previewRenderer != null) previewRenderer.clearLayers();
    }

    /**
     * A position in the preview's world from one relative to the controller, such as {@code (0, 1, 0)} for the block
     * above it, taken in the controller's own orientation.
     */
    public BlockPos toPreviewPos(BlockPos fromController) {
        return multiblockSchemaInfo.getMapSchema().getControllerPos().offset(fromController);
    }

    private Flow extensionColumn(Slot slot) {
        return Flow.col().name("extensions_" + slot.name().toLowerCase())
                .coverChildren().childPadding(2)
                .crossAxisAlignment(Alignment.CrossAxis.START)
                .children(MultiblockPreviewExtensions.build(slot, this), w -> w);
    }

    private ParentWidget<?> extensionOverlay() {
        // tiny, so it never takes the mouse from the 3D view; the widgets inside place themselves
        ParentWidget<?> overlay = new ParentWidget<>().name("extensions_overlay").size(1, 1);
        MultiblockPreviewExtensions.build(Slot.OVERLAY, this).forEach(overlay::child);
        return overlay;
    }

    /** The blocks a preview of the multiblock would list, without building the preview itself. */
    public static Reference2IntMap<Block> previewBlockCounts(MultiblockMachineDefinition definition) {
        var info = new MultiblockSchemaInfo(definition);
        Direction front = definition.getRotationState().defaultDirection;
        Direction up = switch (definition.getRotationState()) {
            case Y_AXIS -> Direction.NORTH;
            case ALL, NON_Y_AXIS, NONE -> Direction.UP;
        };
        info.refreshSchema(definition, front, up, false, null);
        return info.getBlockCounts();
    }

    /** How many layers the structure has, from its lowest block to its highest. */
    private int layerCount() {
        var bounds = multiblockSchemaInfo.getMapSchema().getBounds();
        if (bounds == null) return 1;
        return bounds.getSecond().getY() - bounds.getFirst().getY() + 1;
    }

    /** The layer shown up to, from 1 for the lowest one; the last is all of them. */
    private int currentLayer() {
        return yLevel == Integer.MAX_VALUE ? layerCount() : yLevel - multiblockSchemaInfo.getMapSchema().getBounds()
                .getFirst().getY() + 1;
    }

    private void setLayer(int layer) {
        int count = layerCount();
        int clamped = Mth.clamp(layer, 1, count);
        int newLevel = clamped >= count ? Integer.MAX_VALUE :
                multiblockSchemaInfo.getMapSchema().getBounds().getFirst().getY() + clamped - 1;
        if (newLevel == yLevel) return;
        yLevel = newLevel;
        applyFilters();
    }

    /** Under the view: a slider for how many layers of the structure are shown, with the layer it is at. */
    private IWidget createLayerSlider(int width) {
        int count = layerCount();
        if (count <= 1) return new EmptyWidget();
        int labelWidth = Minecraft.getInstance().font.width(count + "/" + count) + 4;
        return Flow.row()
                .widthRel(1f)
                .height(14)
                .childPadding(3)
                .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                .child(Text.lang("gtceu.multiblock.preview.layer.label").asWidget().height(10))
                .child(new SliderWidget()
                        .value(new brachy.modularui.value.DoubleValue.Dynamic(() -> currentLayer(),
                                v -> setLayer((int) Math.round(v))))
                        .bounds(1, count)
                        .stopper(1)
                        .width(Math.max(30, width - labelWidth - 52))
                        .height(10)
                        .tooltipDynamic(r -> {
                            r.addLine(Component.translatable("gtceu.multiblock.preview.layer"));
                            r.addLine(yLevel == Integer.MAX_VALUE ?
                                    Component.translatable("gtceu.multiblock.preview.layer.all") :
                                    Component.translatable("gtceu.multiblock.preview.layer.current",
                                            currentLayer()));
                        })
                        .tooltipAutoUpdate(true))
                .child(Text.dynamic(() -> Component.literal(currentLayer() + "/" + count)).asWidget()
                        .width(labelWidth).height(10));
    }

    private void notifyWidgetHandlers() {
        // the slider is built again only when the structure has another number of layers: building it while it is
        // dragged would drop the drag
        if (layersHandler != null && multiblockSchemaInfo != null && multiblockSchemaInfo.getMapSchema() != null &&
                layerCount() != shownLayerCount) {
            shownLayerCount = layerCount();
            layersHandler.notifyUpdate();
        }
        if (partsViewWidget != null) {
            partsViewWidget.notifyUpdate((packet) -> {});
        }
        if (partsHandler != null) {
            partsHandler.notifyUpdate();
        }
        if (structureErrorHandler != null) {
            structureErrorHandler.notifyUpdate();
        }
        if (schemaRenderer != null) {
            schemaRenderer.notifyRecompile();
        }
    }

    /// ==== User Preference UI ======
    private void setPredicateDefaultBlock(char predicate,
                                          BlockInfo blockInfo) {
        this.multiblockSchemaInfo.getBlockPreferences().put(predicate, blockInfo);
        refreshSchema();
        notifyWidgetHandlers();
    }

    private void setUserDefinedBlockInfo(BlockPos pos, BlockInfo blockInfo) {
        // todo validation testing?
        this.multiblockSchemaInfo.getUserGlobalBlockPreferences().put(pos, blockInfo);
        refreshSchema();
        notifyWidgetHandlers();
    }

    private void createConstraintSliders(Flow parent, ExpandablePattern pattern) {
        if (pattern.getBoundsConstraints() == null) {
            return;
        }
        List<IntIntPair> constraints = pattern.getBoundsConstraints().apply();
        Flow topRow = Flow.row()
                .coverChildren();
        Flow bottomRow = Flow.row()
                .coverChildren();

        for (int i = 0; i < constraints.size(); i++) {
            IntIntPair value = constraints.get(i);
            if (value.leftInt() != value.rightInt()) {
                final int index = i;
                IntValue.Dynamic syncValue = new IntValue.Dynamic(
                        () -> this.getMultiblockSchemaInfo().getUserDimensions().getInt(index), v -> {
                            int oldValue = this.getMultiblockSchemaInfo().getUserDimensions().getInt(index);
                            if (oldValue == v) return;
                            this.getMultiblockSchemaInfo().getUserDimensions().set(index, v);
                            refreshSchema();
                            notifyWidgetHandlers();
                        });

                var textField = new TextFieldWidget() {

                    @Override
                    public boolean onMouseScrolled(double scrollX, double scrollY) {
                        int inc = (int) scrollY;
                        int val = Mth.clamp(syncValue.getIntValue() + inc, value.leftInt(),
                                value.rightInt());
                        syncValue.setIntValue(val);
                        return true;
                    }
                };

                textField.tooltip(r -> r.addLine(Component.literal(RelativeDirection.values()[index].name())))
                        .value(new StringValue.Dynamic(() -> String.valueOf(syncValue.getIntValue()), $ -> {}));

                (i < 3 ? topRow : bottomRow).child(textField.width(20));
                // parent.child(textField);

                /*
                 * parent.child(new SliderWidget()
                 * .background(GTGuiTextures.FLUID_SLOT)
                 * .bounds(value.leftInt(), value.rightInt())
                 * .height(16)
                 * .width(value.rightInt() * 12)
                 * .stopper(1.0f)
                 * .value(new IntValue.Dynamic(
                 * () -> this.getMultiblockSchemaInfo().getUserDimensions().getInt(index), v -> {
                 * int oldValue = this.getMultiblockSchemaInfo().getUserDimensions().getInt(index);
                 * if (oldValue == v) return;
                 * this.getMultiblockSchemaInfo().getUserDimensions().set(index, v);
                 * refreshSchema();
                 * refreshViewWidget();
                 * })));
                 */
            }
        }

        parent.child(topRow);
        parent.child(bottomRow);
    }

    private void createSliceSliders(Flow col, BlockPattern blockPattern) {
        int repeatSliceIndex = 0;
        for (var patternSlice : blockPattern.getSlices()) {
            if (patternSlice.getMinRepeats() == 1 && patternSlice.getMaxRepeats() == 1) {
                repeatSliceIndex++;
                continue;
            }
            if (!this.multiblockSchemaInfo.getUserSliceRepeats().containsKey(repeatSliceIndex)) {
                this.multiblockSchemaInfo.getUserSliceRepeats().put(repeatSliceIndex, patternSlice.getMinRepeats());
            }
            if (patternSlice.getMinRepeats() != patternSlice.getMaxRepeats()) {
                final int index = repeatSliceIndex;
                IntValue.Dynamic syncValue = new IntValue.Dynamic(() -> {
                    if (!this.multiblockSchemaInfo.getUserSliceRepeats().containsKey(index)) return 0;
                    return this.multiblockSchemaInfo.getUserSliceRepeats().get(index);
                }, v -> {
                    int oldValue = this.multiblockSchemaInfo.getUserSliceRepeats().getOrDefault(index, 0);
                    if (oldValue == v) return;
                    this.multiblockSchemaInfo.getUserSliceRepeats().put(index, v);
                    refreshSchema();
                    notifyWidgetHandlers();
                });

                var textField = new TextFieldWidget() {

                    @Override
                    public boolean onMouseScrolled(double scrollX, double scrollY) {
                        int inc = (int) scrollY;
                        int val = Mth.clamp(syncValue.getIntValue() + inc, patternSlice.getMinRepeats(),
                                patternSlice.getMaxRepeats());
                        syncValue.setIntValue(val);
                        return true;
                    }
                };

                textField
                        .value(new StringValue.Dynamic(() -> String.valueOf(syncValue.getIntValue()), text -> {
                            try {
                                syncValue.setIntValue(Mth.clamp(Integer.parseInt(text.trim()),
                                        patternSlice.getMinRepeats(), patternSlice.getMaxRepeats()));
                            } catch (NumberFormatException ignored) {}
                        }));

                // - [n] +, as the old preview had
                var minus = new ButtonWidget<>().size(12, 18).overlay(Text.str("-").asIcon())
                        .onMousePressed((c, b) -> {
                            syncValue.setIntValue(Mth.clamp(syncValue.getIntValue() - 1,
                                    patternSlice.getMinRepeats(), patternSlice.getMaxRepeats()));
                            return true;
                        });
                var plus = new ButtonWidget<>().size(12, 18).overlay(Text.str("+").asIcon())
                        .onMousePressed((c, b) -> {
                            syncValue.setIntValue(Mth.clamp(syncValue.getIntValue() + 1,
                                    patternSlice.getMinRepeats(), patternSlice.getMaxRepeats()));
                            return true;
                        });
                col.child(Flow.row().coverChildren().childPadding(1).crossAxisAlignment(Alignment.CrossAxis.CENTER)
                        .child(minus)
                        .child(textField.width(24).setNumbers(patternSlice.getMinRepeats(),
                                patternSlice.getMaxRepeats()))
                        .child(plus));
                /*
                 * col.child(new SliderWidget()
                 * .background(GTGuiTextures.FLUID_SLOT)
                 * .height(16)
                 * .width(patternSlice.getMaxRepeats() * 12)
                 * .stopper(1.0f)
                 * .bounds(patternSlice.getMinRepeats(), patternSlice.getMaxRepeats())
                 * );
                 */
            }
            repeatSliceIndex++;
        }
    }

    protected record SelectionInfo(BlockPos pos, BlockInfo info) {

        protected static SelectionInfo empty() {
            return new SelectionInfo(BlockPos.ZERO, BlockInfo.EMPTY);
        }

        protected static SelectionInfo of(BlockHitResult result, BlockState state) {
            return new SelectionInfo(result.getBlockPos(), BlockInfo.fromBlockState(state));
        }

        public Block block() {
            return state().getBlock();
        }

        public BlockState state() {
            return info().getBlockState();
        }

        public ItemStack stack() {
            return info().getItemStackForm();
        }
    }
}
