package com.gregtechceu.gtceu.integration.recipeviewer;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidDefinition;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockore.BedrockOreDefinition;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.gui.ViewerEntry;
import com.gregtechceu.gtceu.api.recipe.gui.viewer.GTViewerCategories;
import com.gregtechceu.gtceu.api.recipe.gui.viewer.GTViewerCategory;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;
import com.gregtechceu.gtceu.common.item.behavior.IntCircuitBehaviour;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.recipeviewer.widgets.GTOreByProduct;
import com.gregtechceu.gtceu.integration.recipeviewer.widgets.MultiblockPreviewWidget;
import com.gregtechceu.gtceu.integration.recipeviewer.widgets.OreProcessingRecipeWidget;
import com.gregtechceu.gtceu.integration.recipeviewer.widgets.OreVeinRecipeWidget;
import com.gregtechceu.gtceu.integration.recipeviewer.widgets.ProgrammedCircuitRecipeWidget;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.*;

/**
 * GT's own extra recipe viewer pages, described with {@link GTViewerCategory} so EMI and JEI get them from one
 * definition. Addons register theirs the same way.
 */
public final class GTViewerBuiltins {

    private static boolean registered = false;

    private static final Map<MultiblockMachineDefinition, List<ViewerEntry>> MULTIBLOCK_PARTS = new ConcurrentHashMap<>();

    private GTViewerBuiltins() {}

    /** Registers the pages once, whichever viewer loads first. */
    public static synchronized void ensureRegistered() {
        if (registered) return;
        registered = true;

        // the info pages are listed before the GT recipe categories
        registerMultiblockInfo();
        registerOreProcessing();
        registerOreVein();
        registerBedrockFluid();
        registerBedrockOre();
        // and the circuit page after them
        registerProgrammedCircuit();
    }

    // *** Multiblock info ***

    private static void registerMultiblockInfo() {
        GTViewerCategories.register(GTViewerCategory
                .builder(GTCEu.id("multiblock_info"), MultiblockMachineDefinition.class)
                .listFirst(true)
                .icon(() -> GTMultiMachines.ELECTRIC_BLAST_FURNACE.asStack())
                .title(Component.translatable("gtceu.jei.multiblock_info"))
                .entries(() -> GTRegistries.MACHINES.stream()
                        .filter(MultiblockMachineDefinition.class::isInstance)
                        .map(MultiblockMachineDefinition.class::cast)
                        .filter(MultiblockMachineDefinition::isRenderXEIPreview).toList())
                .id(def -> def.getId().withPrefix("/multi_info/"))
                .ui(def -> new MultiblockPreviewWidget(def, null, 200, 180, false))
                .inputs(GTViewerBuiltins::multiblockParts)
                .outputs(def -> List.of(ViewerEntry.of(new ItemStack(def.getBlock()))))
                .maxSize(200, 180)
                .build());
    }

    /** The blocks a multiblock is made of, worked out once without building its preview. */
    private static List<ViewerEntry> multiblockParts(MultiblockMachineDefinition definition) {
        return MULTIBLOCK_PARTS.computeIfAbsent(definition, def -> {
            List<ViewerEntry> parts = new ArrayList<>();
            try {
                Reference2IntMap<net.minecraft.world.level.block.Block> counts = MultiblockPreviewWidget
                        .previewBlockCounts(def);
                counts.forEach((block, count) -> parts.add(ViewerEntry.of(new ItemStack(block.asItem(), count))));
            } catch (Throwable t) {
                // building the structure creates real controllers, which an addon's machine may not like; the page
                // still works without its parts listed
                GTCEu.LOGGER.warn("Could not list the parts of multiblock {} for the recipe viewers: {}", def.getId(),
                        t.toString());
            }
            return parts;
        });
    }

    // *** Ore processing ***

    private static void registerOreProcessing() {
        GTViewerCategories.register(GTViewerCategory.builder(GTCEu.id("ore_processing_diagram"), Material.class)
                .listFirst(true)
                .enabledIf(() -> !ConfigHolder.INSTANCE.compat.hideOreProcessingDiagrams)
                .icon(() -> Items.RAW_IRON.getDefaultInstance())
                .title(Component.translatable("gtceu.jei.ore_processing_diagram"))
                .entries(() -> GTRegistries.MATERIALS.stream()
                        .filter(mat -> mat.hasProperty(PropertyKey.ORE) &&
                                !mat.hasFlag(MaterialFlags.NO_ORE_PROCESSING_TAB))
                        .toList())
                .id(mat -> mat.getResourceLocation().withPrefix("/ore_proc/"))
                .ui(OreProcessingRecipeWidget::new)
                .inputs(mat -> {
                    var byProduct = new GTOreByProduct(mat);
                    List<ViewerEntry> inputs = new ArrayList<>();
                    byProduct.getItemInputs().forEach(list -> inputs.add(new ViewerEntry(list)));
                    byProduct.getFluidInputs().forEach(list -> inputs.add(new ViewerEntry(list)));
                    return inputs;
                })
                .outputs(mat -> new GTOreByProduct(mat).getItemOutputs().stream().map(ViewerEntry::of).toList())
                .workstations(GTViewerBuiltins::oreProcessingMachines)
                .recipeTree(false)
                .maxSize(180, 180)
                .build());
    }

    private static List<ItemStack> oreProcessingMachines() {
        GTRecipeType[] valid = { MACERATOR_RECIPES.value(), ORE_WASHER_RECIPES.value(),
                THERMAL_CENTRIFUGE_RECIPES.value(), CENTRIFUGE_RECIPES.value(), CHEMICAL_BATH_RECIPES.value(),
                ELECTROMAGNETIC_SEPARATOR_RECIPES.value(), SIFTER_RECIPES.value() };
        Comparator<MachineDefinition> byTier = Comparator
                .<MachineDefinition, Boolean>comparing(m -> m instanceof MultiblockMachineDefinition)
                .thenComparingInt(MachineDefinition::getTier);
        List<ItemStack> machines = new ArrayList<>();
        for (MachineDefinition machine : GTRegistries.MACHINES.stream().sorted(byTier).toList()) {
            for (GTRecipeType type : machine.getRecipeTypes()) {
                if (Arrays.asList(valid).contains(type)) {
                    machines.add(machine.asStack());
                    break;
                }
            }
        }
        return machines;
    }

    // *** Ore veins and bedrock deposits ***

    private static <T> ResourceLocation keyOf(ResourceKey<Registry<T>> registry, T value, String prefix) {
        return Objects.requireNonNull(Minecraft.getInstance().level.registryAccess().registryOrThrow(registry)
                .getKey(value)).withPrefix(prefix);
    }

    private static List<ViewerEntry> dimensionMarkers(java.util.Set<ResourceKey<net.minecraft.world.level.Level>> filter) {
        return Arrays.stream(OreVeinRecipeWidget.getDimensionMarkers(filter))
                .map(marker -> ViewerEntry.of(marker.getIcon())).toList();
    }

    private static void registerOreVein() {
        GTViewerCategories.register(GTViewerCategory.builder(GTCEu.id("ore_vein_diagram"), GTOreDefinition.class)
                .listFirst(true)
                .icon(() -> Items.RAW_IRON.getDefaultInstance())
                .title(Component.translatable("gtceu.jei.ore_vein_diagram"))
                .entries(() -> Minecraft.getInstance().level.registryAccess()
                        .registryOrThrow(GTRegistries.Keys.ORE_VEIN).stream().toList())
                .id(vein -> keyOf(GTRegistries.Keys.ORE_VEIN, vein, "/ore_vein_diagram/"))
                .ui(OreVeinRecipeWidget::new)
                .inputs(vein -> dimensionMarkers(vein.dimensionFilter()))
                .outputs(vein -> OreVeinRecipeWidget.getContainedOresAndBlocks(vein).stream()
                        .map(ViewerEntry::of).toList())
                .workstation(() -> GTItems.PROSPECTOR_LV.asStack())
                .workstation(() -> GTItems.PROSPECTOR_HV.asStack())
                .workstation(() -> GTItems.PROSPECTOR_LuV.asStack())
                .maxSize(180, 300)
                .build());
    }

    private static void registerBedrockFluid() {
        GTViewerCategories.register(GTViewerCategory
                .builder(GTCEu.id("bedrock_fluid_diagram"), BedrockFluidDefinition.class)
                .listFirst(true)
                .icon(() -> GTMaterials.Oil.getFluid().getBucket().getDefaultInstance())
                .title(Component.translatable("gtceu.jei.bedrock_fluid_diagram"))
                .entries(() -> Minecraft.getInstance().level.registryAccess()
                        .registryOrThrow(GTRegistries.Keys.BEDROCK_FLUID).stream()
                        .filter(BedrockFluidDefinition::canGenerate).toList())
                .id(fluid -> keyOf(GTRegistries.Keys.BEDROCK_FLUID, fluid, "/bedrock_fluid_diagram/"))
                .ui(OreVeinRecipeWidget::new)
                .inputs(fluid -> dimensionMarkers(fluid.dimensionFilter))
                .outputs(fluid -> List.of(ViewerEntry.of(new FluidStack(fluid.getStoredFluid(), 1000))))
                .workstation(() -> GTItems.PROSPECTOR_HV.asStack())
                .workstation(() -> GTItems.PROSPECTOR_LuV.asStack())
                .maxSize(180, 250)
                .build());
    }

    private static void registerBedrockOre() {
        GTViewerCategories.register(GTViewerCategory
                .builder(GTCEu.id("bedrock_ore_diagram"), BedrockOreDefinition.class)
                .listFirst(true)
                .enabledIf(() -> ConfigHolder.INSTANCE.machines.doBedrockOres)
                .icon(() -> ChemicalHelper.get(TagPrefix.rawOre, GTMaterials.Tungstate))
                .title(Component.translatable("gtceu.jei.bedrock_ore_diagram"))
                .entries(() -> Minecraft.getInstance().level.registryAccess()
                        .registryOrThrow(GTRegistries.Keys.BEDROCK_ORE).stream()
                        .filter(BedrockOreDefinition::canGenerate).toList())
                .id(ore -> keyOf(GTRegistries.Keys.BEDROCK_ORE, ore, "/bedrock_ore_diagram/"))
                .ui(OreVeinRecipeWidget::new)
                .inputs(ore -> dimensionMarkers(ore.dimensionFilter()))
                .outputs(ore -> OreVeinRecipeWidget.getRawMaterialList(ore).stream().map(ViewerEntry::of).toList())
                .workstation(() -> GTItems.PROSPECTOR_HV.asStack())
                .workstation(() -> GTItems.PROSPECTOR_LuV.asStack())
                .maxSize(180, 300)
                .build());
    }

    // *** Programmed circuit ***

    private static void registerProgrammedCircuit() {
        GTViewerCategories.register(GTViewerCategory.builder(GTCEu.id("programmed_circuit"), Object.class)
                .icon(() -> GTItems.PROGRAMMED_CIRCUIT.asStack())
                .title(Component.translatable("gtceu.jei.programmed_circuit"))
                .entries(() -> List.of(new Object()))
                .id(o -> GTCEu.id("/programmed_circuit"))
                .ui(o -> new ProgrammedCircuitRecipeWidget())
                .outputs(o -> IntStream.range(0, 33).mapToObj(IntCircuitBehaviour::stack).map(ViewerEntry::of)
                        .toList())
                .workstation(() -> IntCircuitBehaviour.stack(0))
                .recipeTree(false)
                .hideCraftable(true)
                .build());
    }
}
