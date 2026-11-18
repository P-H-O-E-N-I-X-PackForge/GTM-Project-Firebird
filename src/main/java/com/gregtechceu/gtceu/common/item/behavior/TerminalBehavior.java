package com.gregtechceu.gtceu.common.item.behavior;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.mui.IItemUIHolder;
import com.gregtechceu.gtceu.api.mui.MultiblockSchemaInfo;
import com.gregtechceu.gtceu.api.multiblock.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.multiblock.pattern.ExpandablePattern;
import com.gregtechceu.gtceu.api.multiblock.pattern.IBlockPattern;
import com.gregtechceu.gtceu.api.multiblock.pattern.PatternState;
import com.gregtechceu.gtceu.api.multiblock.util.AbstractStructureHelper;
import com.gregtechceu.gtceu.api.multiblock.util.AutobuildHelper;
import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;
import com.gregtechceu.gtceu.common.data.item.GTDataComponents;
import com.gregtechceu.gtceu.common.network.packets.CPacketTerminalSettings;
import com.gregtechceu.gtceu.integration.recipeviewer.widgets.MultiblockPreviewWidget;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

import appeng.api.ids.AEComponents;
import brachy.modularui.factory.ClientGUI;
import brachy.modularui.factory.PlayerInventoryGuiData;
import brachy.modularui.factory.inventory.InventoryTypes;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.*;
import org.jspecify.annotations.Nullable;

import java.util.*;

import static com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine.DEFAULT_STRUCTURE;

public class TerminalBehavior implements IInteractionItem, IItemUIHolder, IAddInformation {

    // todo somewhere client panel warning if the structure to be built is invalid
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (!(MetaMachine.getMachine(level, pos) instanceof MultiblockControllerMachine controller)) {
            return InteractionResult.PASS;
        }

        // Sneak-clicking a controller builds it. If the terminal already holds this multiblock's information, from a
        // right click on the controller, its block choices and flip are used. Otherwise the multiblock is built as it
        // is, with the default blocks, and the terminal is left as it was.
        var stored = stack.get(GTDataComponents.TERMINAL_CONTROLLER_INFO);
        boolean useStored = stored != null && controller.getDefinition() == stored.definition();
        var info = useStored ? stored : new ControllerInfo(controller);

        MultiblockSchemaInfo chosen = useStored ?
                stack.getOrDefault(GTDataComponents.MULTI_SCHEMA_INFO, defaultSchemaInfo(info)) :
                defaultSchemaInfo(info);
        String structure = chosen.getStructure();
        PatternState state = controller.getPatternState(structure);
        if (state == null) {
            return InteractionResult.PASS;
        }
        // nothing to build when the structure and everything layered on it are formed
        boolean everythingFormed = state.isFormed();
        for (String overlay : chosen.getOverlays()) {
            everythingFormed &= controller.isStructureFormed(overlay);
        }
        if (everythingFormed) {
            return InteractionResult.PASS;
        }

        Direction frontFacing = controller.getFrontFacing();
        Direction upFacing = controller.getUpwardsFacing();

        if (!level.isClientSide) {
            MultiblockSchemaInfo schemaInfo = chosen;
            boolean flipped = schemaInfo.isFlipped();

            ServerPlayer serverPlayer = (ServerPlayer) player;
            // Partially copy pasted from MultiblockControllerMachine#onUse.
            // TODO: Probably extract into helper function
            Map<BlockPos, BlockInfo> resultStructure = new HashMap<>();
            AbstractStructureHelper structureHelper = null;
            IBlockPattern pattern = schemaInfo.getPattern();
            if (pattern instanceof BlockPattern blockPattern) {
                Int2IntMap slices = new Int2IntArrayMap();
                for (int i = 0; i < blockPattern.getSlices().length; i++) {
                    slices.put(i, blockPattern.getSlices()[i].getMinRepeats());
                }
                slices.putAll(schemaInfo.getUserSliceRepeats());

                structureHelper = AbstractStructureHelper.blockPattern(slices);
            } else if (pattern instanceof ExpandablePattern expandablePattern) {
                IntList dims = new IntArrayList();
                if (expandablePattern.getBoundsConstraints() != null) {
                    IntList userDims = schemaInfo.getUserDimensions();
                    List<IntIntPair> constraints = expandablePattern.getBoundsConstraints().apply();
                    for (int i = 0; i < constraints.size(); i++) {
                        IntIntPair bounds = constraints.get(i);
                        int dim = i < userDims.size() ? userDims.getInt(i) : bounds.leftInt();
                        dims.add(Mth.clamp(dim, bounds.leftInt(), bounds.rightInt()));
                    }
                }
                structureHelper = AbstractStructureHelper.expandable(dims);
            }

            if (structureHelper != null) {
                structureHelper.populate(schemaInfo, resultStructure, pattern,
                        schemaInfo.getUserGlobalBlockPreferences(), frontFacing,
                        upFacing,
                        flipped);
                // the structures layered on the selected one are built with it
                schemaInfo.addOverlays(resultStructure, frontFacing, upFacing, flipped);
            }

            // Extract controller block offset
            Block controllerBlock = controller.getDefinition().getBlock();
            BlockPos schemaControllerPos = BlockPos.ZERO;
            for (var entry : resultStructure.entrySet()) {
                if (entry.getValue().getBlockState().is(controllerBlock)) {
                    schemaControllerPos = entry.getKey();
                    break;
                }
            }

            BlockPos controllerOffset = controller.getBlockPos().subtract(schemaControllerPos);
            if (player.isCreative()) {
                for (var entry : resultStructure.entrySet()) {
                    level.setBlockAndUpdate(entry.getKey().offset(controllerOffset), entry.getValue().getBlockState());
                }
            } else if (structureHelper != null) {
                AutobuildHelper.autobuild(serverPlayer, context.getItemInHand(), controller.getDefinition(), controller,
                        resultStructure, structureHelper, flipped, structure);
            }

            // needed to force the multiblock to do a clean check, kinda sus
            if (structure.equals(DEFAULT_STRUCTURE)) {
                controller.getDefaultPatternState().getCache().clear();
                controller.checkAndFormStructure();
            } else {
                state.getCache().clear();
                if (controller.checkStructurePattern(structure).getState().isValid()) {
                    controller.formStructure(structure);
                }
            }
            for (String overlay : schemaInfo.getOverlays()) {
                var overlayState = controller.getPatternState(overlay);
                if (overlayState == null) continue;
                overlayState.getCache().clear();
                if (controller.checkStructurePattern(overlay).getState().isValid()) {
                    controller.formStructure(overlay);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack itemStack, UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();

        if (!(MetaMachine.getMachine(level, blockPos) instanceof MultiblockControllerMachine controller)) {
            return InteractionResult.PASS;
        }

        if (player == null || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            player.displayClientMessage(Component.literal("Loaded controller information"), false);
        } else {
            itemStack.set(GTDataComponents.TERMINAL_CONTROLLER_INFO, new ControllerInfo(controller));
            itemStack.remove(GTDataComponents.MULTI_SCHEMA_INFO);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public boolean shouldOpenUI(ItemStack item) {
        return item.has(GTDataComponents.TERMINAL_CONTROLLER_INFO);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(ItemStack item, Level level, Player player,
                                                  InteractionHand usedHand) {
        if (!shouldOpenUI(player.getItemInHand(usedHand))) {
            if (level.isClientSide)
                player.displayClientMessage(Component.literal("No controller information loaded"), false);
            return InteractionResultHolder.pass(player.getItemInHand(usedHand));
        }

        if (level.isClientSide) {
            PlayerInventoryGuiData<?> guiData = PlayerInventoryGuiData.of(player, InventoryTypes.PLAYER, null,
                    usedHand == InteractionHand.OFF_HAND ? Inventory.SLOT_OFFHAND : player.getInventory().selected);
            Optional<ModularPanel<?>> clientPanel = clientPanel(player.getItemInHand(usedHand), usedHand);
            if (clientPanel.isEmpty()) {
                return InteractionResultHolder.sidedSuccess(player.getItemInHand(usedHand), true);
            }
            ClientGUI.open(createScreen(guiData, clientPanel.get()));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(usedHand), level.isClientSide);
    }

    private Optional<ModularPanel<?>> clientPanel(ItemStack item, InteractionHand hand) {
        var controllerInfo = item.get(GTDataComponents.TERMINAL_CONTROLLER_INFO);
        if (controllerInfo == null) return Optional.empty();

        MultiblockSchemaInfo schemaInfo = item.getOrDefault(GTDataComponents.MULTI_SCHEMA_INFO,
                defaultSchemaInfo(controllerInfo));

        MultiblockPreviewWidget previewWidget = new MultiblockPreviewWidget(controllerInfo.definition(), schemaInfo,
                200, 200, true)
                .setControllerPos(controllerInfo.pos())
                .setFrontFacing(controllerInfo.facing()).setUpFacing(controllerInfo.upFace())
                .setFlipped(schemaInfo.isFlipped());
        previewWidget.refreshSchema();

        return Optional.of(ModularPanel.defaultPanel("terminal")
                .coverChildren()
                .child(previewWidget)
                .onCloseAction(w -> writeMultiblockInfo(controllerInfo.definition(), hand, previewWidget)));
    }

    private void writeMultiblockInfo(MultiblockMachineDefinition definition, InteractionHand hand,
                                     MultiblockPreviewWidget previewWidget) {
        MultiblockSchemaInfo schemaInfo = previewWidget.getMultiblockSchemaInfo();

        PacketDistributor.sendToServer(new CPacketTerminalSettings(hand, definition, schemaInfo.getUserSliceRepeats(),
                schemaInfo.getUserDimensions(), schemaInfo.getUserGlobalBlockPreferences(),
                schemaInfo.getBlockPreferences(), previewWidget.isFlipped(), previewWidget.isClearMulti(),
                previewWidget.isClearPreferences(), schemaInfo.isIgnorePreviewCount())
                .structure(schemaInfo.getStructure(), schemaInfo.getOverlays()));
    }

    public static void applyUserPreferences(ItemStack item, MultiblockSchemaInfo schemaInfo,
                                            boolean isClearMulti,
                                            boolean isClearPreferences) {
        if (isClearMulti) {
            item.remove(GTDataComponents.TERMINAL_CONTROLLER_INFO);
        } else if (isClearPreferences) {
            var controllerInfo = item.get(GTDataComponents.TERMINAL_CONTROLLER_INFO);
            if (controllerInfo == null) return;
            item.set(GTDataComponents.MULTI_SCHEMA_INFO, defaultSchemaInfo(controllerInfo));
        } else {
            item.set(GTDataComponents.MULTI_SCHEMA_INFO, schemaInfo);
        }
    }

    private static MultiblockSchemaInfo defaultSchemaInfo(ControllerInfo controllerInfo) {
        MultiblockSchemaInfo schemaInfo = new MultiblockSchemaInfo(controllerInfo.definition());
        schemaInfo.setFlipped(controllerInfo.flipped());
        return schemaInfo;
    }

    @Override
    public ModularPanel<?> buildUI(PlayerInventoryGuiData<?> data, PanelSyncManager syncManager, UISettings settings) {
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        var info = stack.get(GTDataComponents.TERMINAL_CONTROLLER_INFO);
        if (info != null) {
            tooltipComponents.add(Component.translatable("gtceu.terminal.controller_bound",
                    info.pos().getX(), info.pos().getY(), info.pos().getZ())
                    .withStyle(ChatFormatting.GOLD));
            tooltipComponents.add(info.definition().getBlock().getName());
            addSettingsTooltip(stack, info, tooltipComponents);
        }

        GlobalPos aeBinding = null;
        if (GTCEu.Mods.isAE2Loaded()) {
            aeBinding = AE2CallWrapper.getAEBinding(stack);
        }

        if (aeBinding != null) {
            tooltipComponents.add(Component.translatable("gtceu.terminal.network_bound", aeBinding.dimension(),
                    aeBinding.pos().getX(), aeBinding.pos().getY(), aeBinding.pos().getZ())
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    /** What the terminal will do: how it shows and builds the multiblock, as the player has set it in the preview. */
    private static void addSettingsTooltip(ItemStack stack, ControllerInfo info, List<Component> tooltip) {
        MultiblockSchemaInfo schema = stack.get(GTDataComponents.MULTI_SCHEMA_INFO);
        boolean flipped = schema != null ? schema.isFlipped() : info.flipped();

        if (schema != null && !DEFAULT_STRUCTURE.equals(schema.getStructure())) {
            tooltip.add(Component.translatable("gtceu.terminal.tooltip.structure", schema.getStructure())
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("gtceu.terminal.tooltip.flipped", stateName(flipped))
                .withStyle(ChatFormatting.GRAY));

        // the layers: how often each repeatable section is built, the minimum where it was not set
        Component layers;
        if (schema == null || schema.getUserSliceRepeats().isEmpty()) {
            layers = Component.translatable("gtceu.terminal.tooltip.layers.minimum");
        } else {
            StringJoiner joined = new StringJoiner(", ");
            for (int count : schema.getUserSliceRepeats().values()) joined.add(String.valueOf(count));
            layers = Component.literal(joined.toString());
        }
        tooltip.add(Component.translatable("gtceu.terminal.tooltip.layers", layers).withStyle(ChatFormatting.GRAY));

        // the variant: the blocks the player picked in place of the multiblock's own
        Component variant;
        if (schema == null || schema.getUserGlobalBlockPreferences().isEmpty()) {
            variant = Component.translatable("gtceu.terminal.tooltip.variant.default");
        } else {
            Map<Block, Integer> picked = new LinkedHashMap<>();
            for (var preference : schema.getUserGlobalBlockPreferences().values()) {
                picked.merge(preference.getBlockState().getBlock(), 1, Integer::sum);
            }
            var joined = Component.empty();
            boolean first = true;
            for (var entry : picked.entrySet()) {
                if (!first) joined.append(", ");
                joined.append(entry.getKey().getName());
                first = false;
            }
            variant = joined;
        }
        tooltip.add(Component.translatable("gtceu.terminal.tooltip.variant", variant).withStyle(ChatFormatting.GRAY));

        if (GTUtil.isShiftDown()) {
            tooltip.add(Component.translatable("gtceu.terminal.help.load").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("gtceu.terminal.help.preview").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("gtceu.terminal.help.build").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(
                    Component.translatable("gtceu.terminal.tooltip.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static Component stateName(boolean enabled) {
        return Component.translatable(enabled ? "gtceu.terminal.tooltip.yes" : "gtceu.terminal.tooltip.no");
    }

    public static class AE2CallWrapper {

        static @Nullable GlobalPos getAEBinding(ItemStack stack) {
            return stack.get(AEComponents.WIRELESS_LINK_TARGET);
        }
    }

    public record ControllerInfo(MultiblockMachineDefinition definition, BlockPos pos, Direction facing,
                                 Direction upFace, boolean flipped) {

        // spotless:off
        public static final Codec<ControllerInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                MultiblockMachineDefinition.CODEC.fieldOf("definition").forGetter(ControllerInfo::definition),
                BlockPos.CODEC.fieldOf("pos").forGetter(ControllerInfo::pos),
                Direction.CODEC.fieldOf("facing").forGetter(ControllerInfo::facing),
                Direction.CODEC.fieldOf("up").forGetter(ControllerInfo::upFace),
                Codec.BOOL.fieldOf("flipped").forGetter(ControllerInfo::flipped)
        ).apply(instance, ControllerInfo::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ControllerInfo> STREAM_CODEC = StreamCodec.composite(
                MultiblockMachineDefinition.STREAM_CODEC, ControllerInfo::definition,
                BlockPos.STREAM_CODEC, ControllerInfo::pos,
                Direction.STREAM_CODEC, ControllerInfo::facing,
                Direction.STREAM_CODEC, ControllerInfo::upFace,
                ByteBufCodecs.BOOL, ControllerInfo::flipped,
                ControllerInfo::new
        );
        //spotless:on

        public ControllerInfo(MultiblockControllerMachine machine) {
            this(machine.getDefinition(), machine.getBlockPos(), machine.getFrontFacing(), machine.getUpwardsFacing(),
                    machine.isFlipped());
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ControllerInfo other)) return false;
            return definition.getId().equals(other.definition.getId()) && pos.equals(other.pos) &&
                    upFace.equals(other.upFace) && flipped == other.flipped;
        }

        @Override
        public int hashCode() {
            return Objects.hash(definition, pos, facing, upFace, flipped);
        }
    }
}
