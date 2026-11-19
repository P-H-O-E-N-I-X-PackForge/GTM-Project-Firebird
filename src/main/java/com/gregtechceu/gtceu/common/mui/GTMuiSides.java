package com.gregtechceu.gtceu.common.mui;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.IMuiCover;
import com.gregtechceu.gtceu.api.item.IComponentItem;
import com.gregtechceu.gtceu.api.item.component.IItemComponent;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.mui.MachineUIPanel;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.client.mui.schema.MutableSchema;
import com.gregtechceu.gtceu.common.item.behavior.CoverPlaceBehavior;
import com.gregtechceu.gtceu.common.machine.trait.AutoOutputTrait;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IGuiAction;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.drawable.DynamicDrawable;
import brachy.modularui.drawable.SchemaRenderer;
import brachy.modularui.drawable.UITexture;
import brachy.modularui.drawable.schema.BlockHighlight;
import brachy.modularui.drawable.schema.ISchema;
import brachy.modularui.utils.Color;
import brachy.modularui.value.BoolValue;
import brachy.modularui.value.sync.BooleanSyncValue;
import brachy.modularui.value.sync.IntSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.value.sync.SyncHandlers;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.ButtonWidget;
import brachy.modularui.widgets.ToggleButton;
import brachy.modularui.widgets.layout.Flow;
import brachy.modularui.widgets.slot.ItemSlot;
import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import org.jetbrains.annotations.Nullable;

/**
 * The machine's sides page, as the machine UI of GregTech 7.5.3 had it as a tab: the machine itself, to click a face
 * of,
 * with what can be done to the selected face around it. At the bottom left the output buttons: for items and for
 * fluids,
 * a button that sets the face as the output, then turns auto output on and off, and a toggle that lets the machine take
 * input from its output side. At the bottom right a slot for the face's cover, and a button for the cover's settings:
 * a cover put in the slot is placed on the face, and taking it out takes it off. Clicking the selected face again does
 * what the output button does.
 */
public final class GTMuiSides {

    private static final String ICONS = "textures/gui/icon/io_config/";
    private static final int ORANGE = 0xFFFF6E0F;
    private static final int ORANGE_DIM = 0x8FFF6E0F;

    private GTMuiSides() {}

    /** Whether the machine has anything to do on its sides: covers can go on it, or it outputs. */
    public static boolean applies(MetaMachine machine) {
        return machine.getCoverContainer() != null || machine.getTrait(AutoOutputTrait.class) != null;
    }

    /** The sides page. It replaces the main contents while its tab is on. */
    public static ParentWidget<?> page(MetaMachine machine, brachy.modularui.factory.PosGuiData data,
                                       PanelSyncManager sm,
                                       brachy.modularui.screen.UISettings settings) {
        final ICoverable coverable = machine.getCoverContainer();
        final AutoOutputTrait output = machine.getTrait(AutoOutputTrait.class);
        final int[] side = { -1 };

        // the selected face reaches the server too, which is where covers and outputs are changed
        final boolean[] updatingSlot = { false };
        final CustomItemStackHandler coverHandler = new CustomItemStackHandler(1) {

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
        coverHandler.setOnContentsChanged(() -> {
            if (updatingSlot[0]) return;
            coverItemChanged(sm, coverable, coverHandler, side, coverHandler.getStackInSlot(0));
        });
        coverHandler.setFilter(stack -> stack.isEmpty() || (side[0] >= 0 && coverable != null &&
                canAttachTo(coverable, coverDefinitionOf(stack), Direction.from3DDataValue(side[0]))));

        IntSyncValue selected = new IntSyncValue(() -> side[0], value -> {
            side[0] = value;
            syncCoverSlot(coverable, coverHandler, side, updatingSlot);
        }).allowC2S();
        sm.syncValue("sides_selected", selected);

        sm.registerSyncedAction("sides_click", false, true, buf -> {
            int button = buf.readVarInt();
            if (output != null && side[0] >= 0) clickFace(output, Direction.from3DDataValue(side[0]), button);
        });
        sm.registerSyncedAction("sides_mode", false, true, buf -> {
            boolean fluid = buf.readBoolean();
            if (output != null && side[0] >= 0) pressMode(output, Direction.from3DDataValue(side[0]), fluid);
        });

        ParentWidget<?> page = new ParentWidget<>()
                .size(MachineUIPanel.DEFAULT_CONTENT_WIDTH, MachineUIPanel.DEFAULT_CONTENT_HEIGHT)
                .background(GTGuiTextures.BACKGROUND_INVERSE);

        IWidget scene = scene(machine, data, sm, output, side, selected);
        if (scene != null) page.child(scene);

        // bottom left: the output buttons
        if (output != null) {
            // the "auto" labels: items at the top left in orange, fluids at the top right in blue
            if (output.supportsAutoOutputItems()) {
                page.child(new brachy.modularui.widgets.TextWidget<>(
                        Text.lang("gtceu.gui.auto_output.name").color(ORANGE))
                        .pos(4, 4)
                        .setEnabledIf(w -> output.isAutoOutputItems() && output.getItemOutputDirection() != null));
            }
            if (output.supportsAutoOutputFluids()) {
                page.child(new brachy.modularui.widgets.TextWidget<>(
                        Text.lang("gtceu.gui.auto_output.name").color(0xFF00B4FF))
                        .top(4).right(4)
                        .setEnabledIf(w -> output.isAutoOutputFluids() && output.getFluidOutputDirection() != null));
            }
            // stacked up from the bottom: items at the bottom, fluids over them
            Flow left = Flow.col().coverChildren().childPadding(3).left(6).bottom(6);
            if (output.supportsAutoOutputFluids()) left.child(outputRow(sm, output, true, side));
            if (output.supportsAutoOutputItems()) left.child(outputRow(sm, output, false, side));
            page.child(left);
        }

        // bottom right: the cover slot and the cover's settings
        if (coverable != null) {
            var coverSlot = SyncHandlers.itemSlot(coverHandler, 0).singletonSlotGroup(0)
                    .filter(stack -> stack.isEmpty() || (side[0] >= 0 &&
                            canAttachTo(coverable, coverDefinitionOf(stack), Direction.from3DDataValue(side[0]))));
            Flow right = Flow.row().coverChildren().childPadding(1).right(6).bottom(6);
            right.child(configButton(sm, data, settings, coverable, side));
            right.child(new ItemSlot().slot(coverSlot)
                    .background(GTGuiTextures.SLOT, uiTexture("cover_slot_overlay.png"))
                    .setEnabledIf(w -> side[0] >= 0));
            page.child(right);
        }
        return page;
    }

    // ---- the output buttons

    private static IWidget outputRow(PanelSyncManager sm, AutoOutputTrait output, boolean fluid, int[] side) {
        UITexture modes = uiTexture(fluid ? "output_config_fluid_modes.png" : "output_config_item_modes.png");
        BooleanSyncValue allow = new BooleanSyncValue(
                () -> fluid ? output.allowsFluidInputFromOutputSide() : output.allowsItemInputFromOutputSide(),
                value -> {
                    if (fluid) output.setAllowFluidInputFromOutputSide(value);
                    else output.setAllowItemInputFromOutputSide(value);
                }).allowC2S();
        sm.syncValue(fluid ? "sides_allow_fluid" : "sides_allow_item", allow);

        Flow row = Flow.row().coverChildren().childPadding(1);
        row.child(new ButtonWidget<>().size(18, 18)
                .overlay(new DynamicDrawable(() -> {
                    int mode = modeOf(output, fluid, side[0]);
                    return modes.getSubArea(0, mode / 3f, 1, (mode + 1) / 3f).asIcon().size(16);
                }))
                .tooltipDynamic(r -> {
                    String key = "gtceu.gui." + (fluid ? "fluid" : "item") + "_auto_output.";
                    int mode = modeOf(output, fluid, side[0]);
                    String state = side[0] < 0 ? "unselected" : mode == 2 ? "enabled" : mode == 1 ? "disabled" :
                            "other_direction";
                    // the unselected and other direction ones are several lines: .0, .1, ...
                    if (net.minecraft.client.resources.language.I18n.exists(key + state + ".0")) {
                        for (int i = 0; net.minecraft.client.resources.language.I18n
                                .exists(key + state + "." + i); i++) {
                            r.addLine(Component.translatable(key + state + "." + i));
                        }
                    } else {
                        r.addLine(Component.translatable(key + state));
                    }
                })
                .tooltipAutoUpdate(true)
                .onMousePressed((context, button) -> {
                    if (side[0] < 0) return true;
                    GTMuiSync.call(sm, "sides_mode", buf -> buf.writeBoolean(fluid));
                    return true;
                }));
        row.child(new ToggleButton().size(18, 18)
                .value(new BoolValue.Dynamic(allow::getBoolValue, allow::setBoolValue))
                .overlay((fluid ? GTGuiTextures.BUTTON_FLUID_OUTPUT : GTGuiTextures.BUTTON_ITEM_OUTPUT).asIcon()
                        .size(16))
                .tooltip(r -> r.addLine(Component.translatable(
                        "gtceu.gui." + (fluid ? "fluid" : "item") + "_auto_output.allow_input"))));
        return row;
    }

    /** 0: not the output side, 1: the output side with auto output off, 2: the output side with auto output on. */
    private static int modeOf(AutoOutputTrait output, boolean fluid, int side) {
        if (side < 0) return 0;
        Direction face = Direction.from3DDataValue(side);
        Direction out = fluid ? output.getFluidOutputDirection() : output.getItemOutputDirection();
        if (out != face) return 0;
        return (fluid ? output.isAutoOutputFluids() : output.isAutoOutputItems()) ? 2 : 1;
    }

    private static void pressMode(AutoOutputTrait output, Direction face, boolean fluid) {
        if (fluid) {
            if (output.getFluidOutputDirection() == face) {
                output.setAllowAutoOutputFluids(!output.isAutoOutputFluids());
            } else {
                output.setAllowAutoOutputFluids(false);
                output.setFluidOutputDirection(face);
            }
        } else {
            if (output.getItemOutputDirection() == face) {
                output.setAllowAutoOutputItems(!output.isAutoOutputItems());
            } else {
                output.setAllowAutoOutputItems(false);
                output.setItemOutputDirection(face);
            }
        }
    }

    /**
     * Clicking the selected face again: left click for items, right click for fluids, or for items when there are no
     * fluids.
     */
    private static void clickFace(AutoOutputTrait output, Direction face, int button) {
        boolean hasItems = output.supportsAutoOutputItems();
        boolean hasFluids = output.supportsAutoOutputFluids();
        if (hasItems && (button == 0 || (!hasFluids && button == 1))) {
            if (output.getItemOutputDirection() != face) {
                output.setItemOutputDirection(face);
                output.setAllowAutoOutputItems(false);
            } else {
                output.setAllowAutoOutputItems(!output.isAutoOutputItems());
            }
        } else if (hasFluids && button == 1 || (hasFluids && !hasItems && button == 0)) {
            if (output.getFluidOutputDirection() != face) {
                output.setFluidOutputDirection(face);
                output.setAllowAutoOutputFluids(false);
            } else {
                output.setAllowAutoOutputFluids(!output.isAutoOutputFluids());
            }
        }
    }

    // ---- covers

    private static void syncCoverSlot(@Nullable ICoverable coverable, CustomItemStackHandler handler, int[] side,
                                      boolean[] updatingSlot) {
        if (coverable == null || side[0] < 0) return;
        CoverBehavior cover = coverable.getCoverAtSide(Direction.from3DDataValue(side[0]));
        ItemStack wanted = cover == null ? ItemStack.EMPTY : cover.getAttachItem();
        if (ItemStack.matches(handler.getStackInSlot(0), wanted)) return;
        updatingSlot[0] = true;
        try {
            handler.setStackInSlot(0, wanted.copy());
            handler.onContentsChanged(0);
        } finally {
            updatingSlot[0] = false;
        }
    }

    private static void coverItemChanged(PanelSyncManager sm, ICoverable coverable, CustomItemStackHandler handler,
                                         int[] side, ItemStack item) {
        if (!(sm.getPlayer() instanceof ServerPlayer player) || side[0] < 0) return;
        Direction face = Direction.from3DDataValue(side[0]);
        var existing = coverable.getCoverAtSide(face);
        GTCEu.LOGGER.info("[sides] cover slot on {}: item {}, cover there {}", face, item, existing);
        if (existing != null) {
            // already the cover of this item: the slot only showed it
            if (!item.isEmpty() && ItemStack.isSameItemSameTags(existing.getAttachItem(), item)) return;
            coverable.removeCover(false, face, player);
        }
        var definition = coverDefinitionOf(item);
        if (!item.isEmpty() && definition != null) {
            // a copy: the item stays in the slot, which only shows what the cover on the face is
            boolean placed = coverable.placeCoverOnSide(face, item.copy(), definition, player);
            GTCEu.LOGGER.info("[sides] placed cover on {}: {}", face, placed);
        }
    }

    private static IWidget configButton(PanelSyncManager sm, brachy.modularui.factory.PosGuiData data,
                                        brachy.modularui.screen.UISettings settings, ICoverable coverable,
                                        int[] side) {
        // a panel for each face, as the cover buttons of the machine UI have, made when it is opened
        var panels = new brachy.modularui.api.IPanelHandler[6];
        for (Direction face : Direction.values()) {
            final Direction f = face;
            panels[f.get3DDataValue()] = sm.syncedPanel("sides_cover_" + f.getName(), true, (sm2, sh) -> {
                var cover = coverable.getCoverAtSide(f);
                if (!(cover instanceof IMuiCover muiCover)) {
                    throw new IllegalStateException("no cover with settings on " + f);
                }
                return muiCover.buildUI(new brachy.modularui.factory.SidedPosGuiData(data.getPlayer(),
                        data.getBlockPos(), f), sm2, settings).closeOnOutOfBoundsClick(true);
            });
        }
        return new ButtonWidget<>().size(18, 18)
                .overlay(uiTexture("cover_settings.png").asIcon().size(16))
                .tooltip(r -> r.addLine(Component.translatable("gui.gtceu.cover.settings")))
                .setEnabledIf(w -> side[0] >= 0 &&
                        coverable.getCoverAtSide(Direction.from3DDataValue(side[0])) instanceof IMuiCover)
                .onMousePressed((context, button) -> {
                    if (side[0] < 0) return true;
                    var panel = panels[side[0]];
                    if (panel.isPanelOpen()) panel.closePanel();
                    else panel.openPanel();
                    return true;
                });
    }

    /** Whether the cover can be put on the face, which also needs what it works on to be there. */
    private static boolean canAttachTo(ICoverable coverable,
                                       @Nullable com.gregtechceu.gtceu.api.cover.CoverDefinition definition,
                                       Direction face) {
        return definition != null && coverable.canPlaceCoverOnSide(definition, face) &&
                definition.createCoverBehavior(coverable, face).canAttach();
    }

    private static @Nullable com.gregtechceu.gtceu.api.cover.CoverDefinition coverDefinitionOf(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IComponentItem item)) return null;
        for (IItemComponent component : item.getComponents()) {
            if (component instanceof CoverPlaceBehavior behavior) return behavior.coverDefinition();
        }
        return null;
    }

    private static UITexture uiTexture(String name) {
        return UITexture.fullImage(GTCEu.id(ICONS + name), null);
    }

    // ---- the view of the machine

    /** The machine's block, to click a face of; null where there is no client to draw it. */
    private static @Nullable IWidget scene(MetaMachine machine, brachy.modularui.factory.PosGuiData data,
                                           PanelSyncManager sm, @Nullable AutoOutputTrait output,
                                           int[] side, IntSyncValue selected) {
        if (!machine.isRemote()) return null;
        try {
            var blocks = new Long2ReferenceOpenHashMap<BlockState>();
            blocks.put(BlockPos.ZERO.asLong(), machine.getBlockState());
            var schema = new MutableSchema(blocks);
            var renderer = new SidesRenderer(schema, output, side);
            renderer.highlightRenderer(new BlockHighlight(Color.withAlpha(0xFFFFFFFF, 0.3f), false, 1 / 32f));
            // seen from where the player stands, close enough for the block to fill the view
            var player = data.getPlayer();
            float yaw = (float) Math.toRadians(player.getYRot()) - (float) (Math.PI / 2);
            float pitch = (float) Math.toRadians(Math.max(-60, Math.min(60, player.getXRot())));
            // the widget sets the camera every frame, so the point it looks at is set there: the middle of the block
            return new brachy.modularui.widgets.SchemaWidget(renderer) {

                /** How far the block came out of the middle of the view, in the view's own units. */
                private float biasX, biasY;
                private int calibrated;

                @Override
                public void draw(brachy.modularui.screen.viewport.ModularGuiContext context,
                                 brachy.modularui.theme.WidgetThemeEntry<?> widgetTheme) {
                    var camera = renderer.camera();
                    float tx = 0.5f + getOffset().x, ty = 0.5f + getOffset().y, tz = 0.5f + getOffset().z;
                    camera.setLookAtAndAngle(tx, ty, tz, getScale(), getYaw(), getPitch());
                    // the bias moves the camera across the view, whichever way the user has turned it
                    var look = camera.getLookVec().normalize();
                    var right = look.cross(new org.joml.Vector3f(0, 1, 0), new org.joml.Vector3f()).normalize();
                    var up = right.cross(look, new org.joml.Vector3f());
                    float halfH = getScale() * (float) Math.tan(Math.toRadians(30));
                    float halfW = halfH * getArea().w() / Math.max(1, getArea().h());
                    right.mul(biasX * halfW);
                    up.mul(biasY * halfH);
                    camera.setLookAtAndAngle(tx + right.x + up.x, ty + right.y + up.y, tz + right.z + up.z,
                            getScale(), getYaw(), getPitch());
                    renderer.drawAtZeroPadded(context, getArea(), widgetTheme.theme());

                    // for its first frames the view finds where the middle of the block came out, and keeps that
                    if (calibrated >= 40) return;
                    var ndc = renderer.projection().transformProject(tx, ty, tz, new org.joml.Vector3f());
                    if (!Float.isFinite(ndc.x) || !Float.isFinite(ndc.y)) return;
                    if (Math.abs(ndc.x) < 0.002f && Math.abs(ndc.y) < 0.002f) {
                        calibrated += 10;
                        return;
                    }
                    calibrated++;
                    biasX += ndc.x * 0.6f;
                    biasY += ndc.y * 0.6f;
                }
            }.scale(1.5f).yaw(yaw).pitch(pitch)
                    .left(4).top(4).right(4).bottom(4)
                    .background(new brachy.modularui.drawable.Rectangle().color(0xFF000000))
                    .listenGuiAction((IGuiAction.MouseReleased) (ctx, mouse) -> {
                        var hit = renderer.lastRayTrace();
                        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return false;
                        int face = hit.getDirection().get3DDataValue();
                        if (mouse == InputConstants.MOUSE_BUTTON_LEFT || mouse == InputConstants.MOUSE_BUTTON_RIGHT) {
                            if (side[0] == face) {
                                // the selected face again: what its output button does
                                int button = mouse;
                                if (output != null) GTMuiSync.call(sm, "sides_click", buf -> buf.writeVarInt(button));
                            } else {
                                selected.setIntValue(face);
                            }
                            return true;
                        }
                        return false;
                    });
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** The machine's view, with its selected face and its output faces marked. */
    private static final class SidesRenderer extends SchemaRenderer {

        private final @Nullable AutoOutputTrait output;
        private final int[] side;
        private final BlockHighlight selectedFace = new BlockHighlight(0xB0FF6E0F, false, 1 / 32f);
        private final BlockHighlight itemFace = new BlockHighlight(ORANGE_DIM, false, 1 / 32f);
        private final BlockHighlight fluidFace = new BlockHighlight(0x8F00B4FF, false, 2 / 32f);

        SidesRenderer(ISchema schema, @Nullable AutoOutputTrait output, int[] side) {
            super(schema);
            this.output = output;
            this.side = side;
        }

        @Override
        public void renderWorld(MultiBufferSource.BufferSource bufferSource, float partialTick) {
            super.renderWorld(bufferSource, partialTick);
            var pose = createWorldRenderPose();
            if (output != null) {
                Direction item = output.getItemOutputDirection();
                if (item != null) {
                    itemFace.color(output.isAutoOutputItems() ? ORANGE : ORANGE_DIM);
                    itemFace.renderHighlight(pose, BlockPos.ZERO, item, camera().pos());
                }
                Direction fluid = output.getFluidOutputDirection();
                if (fluid != null) {
                    fluidFace.color(output.isAutoOutputFluids() ? 0xFF00B4FF : 0x8F00B4FF);
                    fluidFace.renderHighlight(pose, BlockPos.ZERO, fluid, camera().pos());
                }
            }
            if (side[0] >= 0) {
                selectedFace.renderHighlight(pose, BlockPos.ZERO, Direction.from3DDataValue(side[0]),
                        camera().pos());
            }
        }
    }
}
