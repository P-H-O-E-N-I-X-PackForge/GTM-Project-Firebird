package com.gregtechceu.gtceu.integration.recipeviewer.jei;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.recipe.category.GTRecipeCategory;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTFluids;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;
import com.gregtechceu.gtceu.common.fluid.potion.PotionFluid;
import com.gregtechceu.gtceu.integration.recipeviewer.jei.recipe.GTRecipeJEICategory;
import com.gregtechceu.gtceu.integration.recipeviewer.jei.subtype.CircuitSubtypeInterpreter;
import com.gregtechceu.gtceu.integration.recipeviewer.jei.subtype.MaterialSubtypeInterpreter;
import com.gregtechceu.gtceu.integration.recipeviewer.jei.subtype.PotionFluidSubtypeInterpreter;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

import lombok.Getter;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.helpers.IPlatformFluidHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

@JeiPlugin
public class GTJEIPlugin implements IModPlugin {

    @Getter
    private static IJeiRuntime runtime = null;

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return GTCEu.id("jei_plugin");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void registerCategories(@NotNull IRecipeCategoryRegistration registry) {
        if (!GTCEu.Mods.isJEILoaded()) return;

        IJeiHelpers jeiHelpers = registry.getJeiHelpers();
        GTViewerJei.registerCategories(registry, true);
        for (GTRecipeCategory category : GTRegistries.RECIPE_CATEGORIES) {
            if (category.shouldRegisterDisplays()) {
                registry.addRecipeCategories(new GTRecipeJEICategory(jeiHelpers, category));
            }
        }
        GTViewerJei.registerCategories(registry, false);
    }

    @Override
    public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
        if (!GTCEu.Mods.isJEILoaded()) return;

        GTRecipeJEICategory.registerRecipeCatalysts(registration);
        registration.addRecipeCatalyst(GTMultiMachines.LARGE_CHEMICAL_REACTOR.asStack(),
                GTRecipeJEICategory.TYPES.apply(GTRecipeTypes.CHEMICAL_RECIPES.getCategory()));
        GTViewerJei.registerCatalysts(registration);
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        if (GTCEu.Mods.isEMILoaded()) return;
        GTCEu.LOGGER.info("JEI register");
        GTRecipeJEICategory.registerRecipes(registration);
        GTViewerJei.registerRecipes(registration);
    }

    @Override
    public void registerItemSubtypes(@NotNull ISubtypeRegistration registration) {
        if (!GTCEu.Mods.isJEILoaded()) return;

        registration.registerSubtypeInterpreter(GTItems.PROGRAMMED_CIRCUIT.asItem(),
                CircuitSubtypeInterpreter.INSTANCE);
        registration.registerSubtypeInterpreter(GTItems.TURBINE_ROTOR.asItem(), MaterialSubtypeInterpreter.INSTANCE);
    }

    @Override
    public <T> void registerFluidSubtypes(@NotNull ISubtypeRegistration registration,
                                          @NotNull IPlatformFluidHelper<T> platformFluidHelper) {
        if (!GTCEu.Mods.isJEILoaded()) return;

        PotionFluidSubtypeInterpreter interpreter = PotionFluidSubtypeInterpreter.INSTANCE;
        PotionFluid potionFluid = GTFluids.POTION.get();
        registration.registerSubtypeInterpreter(NeoForgeTypes.FLUID_STACK, potionFluid.getSource(), interpreter);
        registration.registerSubtypeInterpreter(NeoForgeTypes.FLUID_STACK, potionFluid.getFlowing(), interpreter);
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        if (!GTCEu.Mods.isJEILoaded()) return;

        Collection<FluidStack> potionFluids = new ArrayList<>(BuiltInRegistries.POTION.size());
        BuiltInRegistries.POTION.holders().forEach(potion -> {
            FluidStack potionFluid = PotionFluid.of(1000, potion);
            potionFluids.add(potionFluid);
        });
        registration.addExtraIngredients(NeoForgeTypes.FLUID_STACK, potionFluids);
    }
}
