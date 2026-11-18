package com.gregtechceu.gtceu.integration.kjs;

import com.gregtechceu.gtceu.integration.kjs.events.GTBedrockFluidVeinEventJS;
import com.gregtechceu.gtceu.integration.kjs.events.GTBedrockOreVeinEventJS;
import com.gregtechceu.gtceu.integration.kjs.events.GTOreVeinEventJS;
import com.gregtechceu.gtceu.integration.kjs.events.MachineRecipeEventJS;
import com.gregtechceu.gtceu.integration.kjs.events.MultiblockFormEventJS;
import com.gregtechceu.gtceu.integration.kjs.events.MultiblockStructureEventJS;
import com.gregtechceu.gtceu.integration.kjs.events.RegisterCapesEventJS;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

public interface GTCEuServerEvents {

    EventGroup GROUP = EventGroup.of("GTCEuServerEvents");

    EventHandler ORE_VEIN_MODIFICATION = GROUP.server("oreVeins", () -> GTOreVeinEventJS.class);
    EventHandler FLUID_VEIN_MODIFICATION = GROUP.server("fluidVeins", () -> GTBedrockFluidVeinEventJS.class);
    EventHandler BEDROCK_ORE_VEIN_MODIFICATION = GROUP.server("bedrockOreVeins", () -> GTBedrockOreVeinEventJS.class);

    EventHandler REGISTER_CAPES = GROUP.server("registerCapes", () -> RegisterCapesEventJS.class);

    EventHandler MULTIBLOCK_FORM = GROUP.server("multiblockForm", () -> MultiblockFormEventJS.class);
    EventHandler STRUCTURE_FORMED = GROUP.server("structureFormed", () -> MultiblockStructureEventJS.class);
    EventHandler STRUCTURE_INVALIDATED = GROUP.server("structureInvalidated", () -> MultiblockStructureEventJS.class);
    EventHandler RECIPE_STARTED = GROUP.server("recipeStarted", () -> MachineRecipeEventJS.class);
    EventHandler RECIPE_FINISHED = GROUP.server("recipeFinished", () -> MachineRecipeEventJS.class);
    EventHandler RECIPE_STATUS = GROUP.server("recipeStatus", () -> MachineRecipeEventJS.class);
}
