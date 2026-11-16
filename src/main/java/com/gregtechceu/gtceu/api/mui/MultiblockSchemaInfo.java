package com.gregtechceu.gtceu.api.mui;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.multiblock.MultiPredicate;
import com.gregtechceu.gtceu.api.multiblock.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.multiblock.pattern.ExpandablePattern;
import com.gregtechceu.gtceu.api.multiblock.pattern.IBlockPattern;
import com.gregtechceu.gtceu.api.multiblock.predicates.BasePredicate;
import com.gregtechceu.gtceu.api.multiblock.util.AbstractStructureHelper;
import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;
import com.gregtechceu.gtceu.api.sync_system.data_transformers.SyncSystemCodecs;
import com.gregtechceu.gtceu.client.mui.schema.MutableSchema;
import com.gregtechceu.gtceu.utils.codec.GTCodecUtils;
import com.gregtechceu.gtceu.utils.codec.GTStreamCodecUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.google.common.collect.HashBasedTable;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine.DEFAULT_STRUCTURE;

public class MultiblockSchemaInfo {

    // spotless:off
    public static final Codec<MultiblockSchemaInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MultiblockMachineDefinition.CODEC.fieldOf("definition").forGetter(MultiblockSchemaInfo::getDefinition),
            GTCodecUtils.map(Codec.INT, Codec.INT).fieldOf("userSliceRepeats").forGetter(MultiblockSchemaInfo::getUserSliceRepeats),
            Codec.INT.listOf().fieldOf("userDimensions").forGetter(MultiblockSchemaInfo::getUserDimensions),
            GTCodecUtils.map(BlockPos.CODEC, BlockInfo.CODEC).fieldOf("userGlobalBlockPreferences").forGetter(MultiblockSchemaInfo::getUserGlobalBlockPreferences),
            GTCodecUtils.map(SyncSystemCodecs.CHAR, BlockInfo.CODEC).fieldOf("blockPreferences").forGetter(MultiblockSchemaInfo::getBlockPreferences),
            Codec.BOOL.fieldOf("flipped").forGetter(MultiblockSchemaInfo::isFlipped),
            Codec.BOOL.fieldOf("ignorePreviewCount").forGetter(MultiblockSchemaInfo::isIgnorePreviewCount),
            Codec.STRING.optionalFieldOf("structure", DEFAULT_STRUCTURE).forGetter(MultiblockSchemaInfo::getStructure),
            Codec.STRING.listOf().optionalFieldOf("overlays", List.of()).forGetter(MultiblockSchemaInfo::getOverlays)
    ).apply(instance, MultiblockSchemaInfo::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, MultiblockSchemaInfo> BASE_STREAM_CODEC = GTStreamCodecUtils.composite(
            MultiblockMachineDefinition.STREAM_CODEC, MultiblockSchemaInfo::getDefinition,
            ByteBufCodecs.map(Int2IntArrayMap::new, ByteBufCodecs.VAR_INT, ByteBufCodecs.VAR_INT), MultiblockSchemaInfo::getUserSliceRepeats,
            ByteBufCodecs.collection(IntArrayList::new, ByteBufCodecs.VAR_INT), MultiblockSchemaInfo::getUserDimensions,
            ByteBufCodecs.map(Object2ObjectOpenHashMap::new, BlockPos.STREAM_CODEC, BlockInfo.STREAM_CODEC), MultiblockSchemaInfo::getUserGlobalBlockPreferences,
            ByteBufCodecs.map(Char2ObjectArrayMap::new, SyncSystemCodecs.CHAR_STREAM, BlockInfo.STREAM_CODEC), MultiblockSchemaInfo::getBlockPreferences,
            ByteBufCodecs.BOOL, MultiblockSchemaInfo::isFlipped,
            ByteBufCodecs.BOOL, MultiblockSchemaInfo::isIgnorePreviewCount,
            ByteBufCodecs.STRING_UTF8, MultiblockSchemaInfo::getStructure,
            MultiblockSchemaInfo::new);
    //spotless:on

    public static final StreamCodec<RegistryFriendlyByteBuf, MultiblockSchemaInfo> STREAM_CODEC = StreamCodec.of(
            (buf, info) -> {
                BASE_STREAM_CODEC.encode(buf, info);
                buf.writeCollection(info.overlays, (b, name) -> b.writeUtf(name));
            }, buf -> {
                MultiblockSchemaInfo info = BASE_STREAM_CODEC.decode(buf);
                for (String name : buf.readList(b -> b.readUtf())) info.setOverlay(name, true);
                return info;
            });

    @Getter
    private final MultiblockMachineDefinition definition;
    /** The name of the structure of the multiblock this describes, see {@code MultiblockMachineBuilder#pattern}. */
    @Getter
    private String structure = DEFAULT_STRUCTURE;
    /** Other structures shown and built together with the selected one, each placed around the same controller. */
    private final List<String> overlays = new ArrayList<>();
    @Getter
    @Setter
    private MutableSchema mapSchema;
    @Getter
    @Setter
    private Reference2IntMap<Block> blockCounts = new Reference2IntOpenHashMap<>();
    @Getter
    private final Object2ObjectMap<BlockPos, BlockInfo> userGlobalBlockPreferences;
    @Getter
    @Setter
    private boolean flipped;
    @Getter
    @Setter
    private boolean ignorePreviewCount;
    @Getter
    protected final Char2ObjectMap<BlockInfo> blockPreferences = new Char2ObjectArrayMap<>();
    @Getter
    protected final HashBasedTable<MultiPredicate, BasePredicate, IntIntPair> minMaxPreferences = HashBasedTable
            .create();

    @Getter
    private final Int2IntMap userSliceRepeats;
    @Getter
    private final IntList userDimensions;
    @Getter
    private final Map<BlockPos, BlockInfo> structureBlocks = new HashMap<>();

    @Getter
    private @Nullable AbstractStructureHelper structureHelper;

    public MultiblockSchemaInfo(MultiblockMachineDefinition definition) {
        this.definition = definition;
        this.userSliceRepeats = new Int2IntArrayMap();
        this.userDimensions = new IntArrayList();
        this.userGlobalBlockPreferences = new Object2ObjectOpenHashMap<>();
    }

    public MultiblockSchemaInfo(MultiblockMachineDefinition definition, Map<Integer, Integer> userSliceRepeats,
                                List<Integer> userDimensions,
                                Map<BlockPos, BlockInfo> userGlobalBlockPreferences,
                                Map<Character, BlockInfo> blockPreferenceMap,
                                boolean flipped,
                                boolean ignorePreviewCount,
                                String structure) {
        this(definition, userSliceRepeats, userDimensions, userGlobalBlockPreferences, blockPreferenceMap, flipped,
                ignorePreviewCount, structure, List.of());
    }

    public MultiblockSchemaInfo(MultiblockMachineDefinition definition, Map<Integer, Integer> userSliceRepeats,
                                List<Integer> userDimensions,
                                Map<BlockPos, BlockInfo> userGlobalBlockPreferences,
                                Map<Character, BlockInfo> blockPreferenceMap,
                                boolean flipped,
                                boolean ignorePreviewCount,
                                String structure, List<String> overlays) {
        this(definition, userSliceRepeats, userDimensions, userGlobalBlockPreferences, blockPreferenceMap, flipped,
                ignorePreviewCount);
        this.structure = definition.getStructurePatterns().containsKey(structure) ? structure : DEFAULT_STRUCTURE;
        for (String name : overlays) {
            if (!name.equals(this.structure) && definition.getStructurePatterns().containsKey(name)) {
                this.overlays.add(name);
            }
        }
    }

    public MultiblockSchemaInfo(MultiblockMachineDefinition definition, Map<Integer, Integer> userSliceRepeats,
                                List<Integer> userDimensions,
                                Map<BlockPos, BlockInfo> userGlobalBlockPreferences,
                                Map<Character, BlockInfo> blockPreferenceMap,
                                boolean flipped,
                                boolean ignorePreviewCount) {
        this.definition = definition;
        this.flipped = flipped;
        this.ignorePreviewCount = ignorePreviewCount;
        this.userSliceRepeats = new Int2IntArrayMap(userSliceRepeats);
        this.userDimensions = new IntArrayList(userDimensions);
        this.userGlobalBlockPreferences = new Object2ObjectOpenHashMap<>(userGlobalBlockPreferences);
        this.blockPreferences.putAll(blockPreferenceMap);
    }

    /**
     * Switches to another of the multiblock's structures. What was set up for the old one, its sizes and block
     * choices, does not apply to the new one and is dropped.
     */
    public void selectStructure(String name) {
        if (structure.equals(name) || !definition.getStructurePatterns().containsKey(name)) return;
        structure = name;
        overlays.remove(name);
        userSliceRepeats.clear();
        userDimensions.clear();
        userGlobalBlockPreferences.clear();
        blockPreferences.clear();
        structureHelper = null;
    }

    /** The other structures built together with the selected one. */
    public List<String> getOverlays() {
        return List.copyOf(overlays);
    }

    public boolean isOverlay(String name) {
        return overlays.contains(name);
    }

    /** Adds or removes a structure that is shown and built together with the selected one. */
    public void setOverlay(String name, boolean on) {
        if (name.equals(structure) || !definition.getStructurePatterns().containsKey(name)) return;
        if (on && !overlays.contains(name)) overlays.add(name);
        else if (!on) overlays.remove(name);
    }

    /** The pattern of the selected structure. */
    public IBlockPattern getPattern() {
        return definition.getStructurePatterns().get(structure).get();
    }

    @ApiStatus.Internal
    public void refreshSchema(MultiblockMachineDefinition multiblockDefinition, Direction frontFacing,
                              Direction upFacing, boolean isFlipped, @Nullable Runnable onSchemaRefresh) {
        Map<BlockPos, BlockInfo> resultStructure = new HashMap<>();
        IBlockPattern pattern = getPattern();

        if (this.structureHelper == null) {
            if (pattern instanceof BlockPattern blockPattern) {
                if (this.userSliceRepeats.isEmpty()) {
                    for (int i = 0; i < blockPattern.getSlices().length; i++) {
                        this.userSliceRepeats.put(i, blockPattern.getSlices()[i].getMinRepeats());
                    }
                }
                // reinterpret slider values as slice repeats?
                this.structureHelper = AbstractStructureHelper.blockPattern(this.userSliceRepeats);

            } else if (pattern instanceof ExpandablePattern expandablePattern) {
                if (this.userDimensions.isEmpty()) {
                    expandablePattern.getBoundsConstraints().apply().stream()
                            .mapToInt(Pair::left)
                            .forEach(this.userDimensions::add);
                }
                // reinterpret slider values as bounds?
                this.structureHelper = AbstractStructureHelper.expandable(this.userDimensions);

            } else {
                // throw? log?
                return;
            }
        }

        this.structureHelper.populate(this, resultStructure, pattern, this.userGlobalBlockPreferences,
                frontFacing, upFacing, isFlipped);

        addOverlays(resultStructure, frontFacing, upFacing, isFlipped);

        Long2ReferenceMap<BlockState> schemaMap = new Long2ReferenceOpenHashMap<>();
        this.blockCounts.clear();
        for (var entry : resultStructure.entrySet()) {
            BlockState state = entry.getValue().getBlockState();
            schemaMap.put(entry.getKey().asLong(), state);
            if (state.isAir()) continue;
            this.blockCounts.merge(state.getBlock(), 1, Integer::sum);
        }
        if (this.mapSchema == null) {
            this.mapSchema = new MutableSchema();
            this.mapSchema.setStructureName(structure);
            this.mapSchema.setBlocks(schemaMap);
        } else {
            this.mapSchema.setStructureName(structure);
            this.mapSchema.setBlocks(schemaMap);
        }
        this.structureBlocks.clear();
        this.structureBlocks.putAll(resultStructure);

        if (onSchemaRefresh != null) {
            onSchemaRefresh.run();
        }
    }

    private void addOverlays(Map<BlockPos, BlockInfo> resultStructure, Direction frontFacing, Direction upFacing,
                             boolean isFlipped) {
        if (overlays.isEmpty()) return;
        var controllerBlock = definition.getBlock();
        BlockPos baseController = controllerPosition(resultStructure, controllerBlock);
        for (String name : overlays) {
            var supplier = definition.getStructurePatterns().get(name);
            if (supplier == null) continue;
            IBlockPattern overlay = supplier.get();
            AbstractStructureHelper helper;
            if (overlay instanceof BlockPattern blockPattern) {
                Int2IntMap repeats = new Int2IntArrayMap();
                for (int i = 0; i < blockPattern.getSlices().length; i++) {
                    repeats.put(i, blockPattern.getSlices()[i].getMinRepeats());
                }
                helper = AbstractStructureHelper.blockPattern(repeats);
            } else if (overlay instanceof ExpandablePattern expandable) {
                IntList dims = new IntArrayList();
                if (expandable.getBoundsConstraints() != null) {
                    expandable.getBoundsConstraints().apply().stream().mapToInt(Pair::left).forEach(dims::add);
                }
                helper = AbstractStructureHelper.expandable(dims);
            } else continue;

            Map<BlockPos, BlockInfo> extra = new HashMap<>();
            helper.populate(this, extra, overlay, null, frontFacing, upFacing, isFlipped);
            BlockPos shift = baseController.subtract(controllerPosition(extra, controllerBlock));
            for (var entry : extra.entrySet()) {
                if (entry.getValue().getBlockState().isAir()) continue;
                resultStructure.putIfAbsent(entry.getKey().offset(shift), entry.getValue());
            }
        }
    }

    private static BlockPos controllerPosition(Map<BlockPos, BlockInfo> structure, net.minecraft.world.level.block.Block controller) {
        for (var entry : structure.entrySet()) {
            if (entry.getValue().getBlockState().is(controller)) return entry.getKey();
        }
        return BlockPos.ZERO;
    }

    @Override
    public int hashCode() {
        return Objects.hash(definition.getId(), blockCounts, userGlobalBlockPreferences,
                blockPreferences, minMaxPreferences, userSliceRepeats, userDimensions, flipped, ignorePreviewCount, structure, overlays);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MultiblockSchemaInfo other)) return false;
        return definition.getId().equals(other.definition.getId()) && blockCounts.equals(other.blockCounts) &&
                userGlobalBlockPreferences.equals(other.userGlobalBlockPreferences) &&
                blockPreferences.equals(other.blockPreferences) && minMaxPreferences.equals(other.minMaxPreferences) &&
                userSliceRepeats.equals(other.userSliceRepeats) &&
                userDimensions.equals(other.userDimensions) && flipped == other.flipped &&
                ignorePreviewCount == other.ignorePreviewCount && structure.equals(other.structure) &&
                overlays.equals(other.overlays);
    }
}
