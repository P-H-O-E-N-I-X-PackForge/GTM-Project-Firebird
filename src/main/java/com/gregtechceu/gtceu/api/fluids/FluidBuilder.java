package com.gregtechceu.gtceu.api.fluids;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconType;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.fluids.attribute.FluidAttribute;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKey;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.api.registry.registrate.IGTFluidBuilder;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.gregtechceu.gtceu.api.fluids.FluidConstants.*;

@Accessors(fluent = true, chain = true)
public class FluidBuilder {

    /**
     * The still and flowing textures of a fluid icon set, and whether the fluid's colour tints them. Textures can be
     * any in the block atlas, from this mod or another, even vanilla's.
     */
    public record IconSetTextures(ResourceLocation still, ResourceLocation flowing, boolean tinted) {}

    private static final Map<ResourceLocation, IconSetTextures> ICONSET_TEXTURES = new HashMap<>();

    /**
     * Gives an icon set textures for the fluids that use it, the way a material icon set gives textures to a material's
     * items. Fluids of a set that has none, and no parent that does, keep using the material icon set paths.
     * <p>
     * Resource packs can still replace the textures themselves.
     */
    public static void registerIconSetTextures(ResourceLocation iconSetId, ResourceLocation still,
                                               ResourceLocation flowing, boolean tinted) {
        ICONSET_TEXTURES.put(iconSetId, new IconSetTextures(still, flowing, tinted));
    }

    public static void registerIconSetTextures(MaterialIconSet iconSet, ResourceLocation still,
                                               ResourceLocation flowing, boolean tinted) {
        registerIconSetTextures(iconSet.id, still, flowing, tinted);
    }

    private static final int INFER_TEMPERATURE = -1;
    private static final int INFER_COLOR = 0xFFFFFFFF;
    private static final int INFER_DENSITY = -1;
    private static final int INFER_LUMINOSITY = -1;
    private static final int INFER_VISCOSITY = -1;

    @Setter
    private String name = null;
    @Setter
    private String translation = null;

    private final Collection<FluidAttribute> attributes = new ArrayList<>();

    @Setter
    private FluidState state = FluidState.LIQUID;
    private int temperature = INFER_TEMPERATURE;
    private int color = INFER_COLOR;
    private boolean isColorEnabled = true;
    /** Whether a colour was asked for: an icon set that comes with its own colouring leaves such a fluid tinted. */
    private boolean colorChosen = false;
    @Nullable
    private MaterialIconSet iconSet = null;
    @Nullable
    private IconSetTextures explicitTextures = null;
    @Setter
    private int density = INFER_DENSITY;
    private int luminosity = INFER_LUMINOSITY;
    private int viscosity = INFER_VISCOSITY;
    @Setter
    private int burnTime = -1;

    @Getter
    @Setter(onMethod_ = @ApiStatus.Internal)
    private ResourceLocation still = null;
    @Getter
    @Setter(onMethod_ = @ApiStatus.Internal)
    private ResourceLocation flowing = null;
    private boolean hasCustomStill = false;
    private boolean hasCustomFlowing = false;

    @Getter
    private boolean hasFluidBlock = false;
    private boolean hasBucket = true;

    public FluidBuilder() {}

    /**
     * @param temperature the temperature of the fluid in Kelvin
     * @return this;
     */
    public @NotNull FluidBuilder temperature(int temperature) {
        Preconditions.checkArgument(temperature >= 0, "temperature must be >= 0");
        this.temperature = temperature;
        return this;
    }

    /**
     * The color may be in either {@code RGB} or {@code ARGB} format.
     * RGB format will assume an alpha of {@code 0xFF}.
     *
     * @param color the color
     * @return this
     */
    public @NotNull FluidBuilder color(int color) {
        this.colorChosen = true;
        this.color = GTUtil.convertRGBtoARGB(color);
        if (this.color == INFER_COLOR) {
            return disableColor();
        }
        return this;
    }

    /**
     * Picks the textures for this fluid from an icon set of its own, instead of the one its material uses for its
     * items. An icon set with registered fluid textures (see {@link #registerIconSetTextures}), such as
     * {@link MaterialIconSet#LAVA} and {@link MaterialIconSet#WATER}, gives the fluid those; any other uses the usual
     * material icon set paths. A colour set with {@link #color(int)} still tints the fluid, and so does
     * {@link #customStill()} replace the icon set's texture.
     */
    public @NotNull FluidBuilder iconSet(MaterialIconSet iconSet) {
        this.iconSet = iconSet;
        return this;
    }

    /**
     * Same, looking the icon set up by its id, for scripts and for sets that are registered by name.
     *
     * @throws IllegalArgumentException if there is no such icon set
     */
    public @NotNull FluidBuilder iconSetId(ResourceLocation iconSetId) {
        MaterialIconSet set = com.gregtechceu.gtceu.api.registry.GTRegistries.MATERIAL_ICON_SETS.get(iconSetId);
        if (set == null) throw new IllegalArgumentException("No material icon set " + iconSetId);
        return iconSet(set);
    }

    /**
     * Gives this one fluid textures of its own, from any mod. Unlike {@link #customStill()}, which looks for files
     * named after the fluid in its material's mod, these can be anywhere, so an addon can retexture a fluid it did not
     * make. They take priority over {@link #iconSet}.
     *
     * @param flowing the flowing texture, or null to use the still one
     * @param tinted  whether the fluid's colour tints them
     */
    public @NotNull FluidBuilder textures(ResourceLocation still, @Nullable ResourceLocation flowing, boolean tinted) {
        this.explicitTextures = new IconSetTextures(still, flowing == null ? still : flowing, tinted);
        return this;
    }

    /**
     * Disables coloring the fluid. A color should still be specified.
     *
     * @return this
     */
    public @NotNull FluidBuilder disableColor() {
        this.isColorEnabled = false;
        return this;
    }

    /**
     * @param density the density in g/cm^3
     * @return this
     */
    @Tolerate
    public @NotNull FluidBuilder density(double density) {
        return density(convertToMCDensity(density));
    }

    /**
     * Converts a density value in g/cm^3 to an MC fluid density by comparison to air's density.
     * 
     * @param density the density to convert
     * @return the MC integer density
     */
    private static int convertToMCDensity(double density) {
        // conversion formula from GT6
        if (density > 0.001225) {
            return (int) (1000 * density);
        } else if (density < 0.001225) {
            return (int) (-0.1 / density);
        }
        return 0;
    }

    /**
     * @param luminosity of the fluid from [0, 16)
     * @return this
     */
    public @NotNull FluidBuilder luminosity(int luminosity) {
        Preconditions.checkArgument(luminosity >= 0 && luminosity < 16, "luminosity must be >= 0 and < 16");
        this.luminosity = luminosity;
        return this;
    }

    /**
     * @param mcViscosity the MC viscosity of the fluid
     * @return this
     */
    public @NotNull FluidBuilder viscosity(int mcViscosity) {
        Preconditions.checkArgument(mcViscosity >= 0, "viscosity must be >= 0");
        this.viscosity = mcViscosity;
        return this;
    }

    /**
     * @param viscosity the viscosity of the fluid in Poise
     * @return this
     */
    public @NotNull FluidBuilder viscosity(double viscosity) {
        return viscosity(convertViscosity(viscosity));
    }

    /**
     * Converts viscosity in Poise to MC viscosity
     * 
     * @param viscosity the viscosity to convert
     * @return the converted value
     */
    private static int convertViscosity(double viscosity) {
        return (int) (viscosity * 10000);
    }

    /**
     * @param attribute the attribute to add
     * @return this
     */
    public @NotNull FluidBuilder attribute(@NotNull FluidAttribute attribute) {
        this.attributes.add(attribute);
        return this;
    }

    /**
     * @param attributes the attributes to add
     * @return this
     */
    public @NotNull FluidBuilder attributes(@NotNull FluidAttribute @NotNull... attributes) {
        Collections.addAll(this.attributes, attributes);
        return this;
    }

    /**
     * Mark this fluid as having a custom still texture
     * 
     * @return this
     */
    public @NotNull FluidBuilder customStill() {
        return textures(true);
    }

    /**
     * @param hasCustomStill if the fluid has a custom still texture
     * @return this
     */
    public @NotNull FluidBuilder textures(boolean hasCustomStill) {
        this.hasCustomStill = hasCustomStill;
        this.isColorEnabled = false;
        return this;
    }

    /**
     * @param hasCustomStill   if the fluid has a custom still texture
     * @param hasCustomFlowing if the fluid has a custom flowing texture
     * @return this
     */
    public @NotNull FluidBuilder textures(boolean hasCustomStill, boolean hasCustomFlowing) {
        this.hasCustomStill = hasCustomStill;
        this.hasCustomFlowing = hasCustomFlowing;
        this.isColorEnabled = false;
        return this;
    }

    /**
     * Generate a fluid block for the fluid
     *
     * @return this
     */
    public @NotNull FluidBuilder block() {
        this.hasFluidBlock = true;
        return this;
    }

    /**
     * Disables the auto-generated fluid bucket for the fluid
     *
     * @return this
     */
    public @NotNull FluidBuilder disableBucket() {
        this.hasBucket = false;
        return this;
    }

    public @NotNull Supplier<? extends Fluid> build(Material material, @NotNull FluidStorageKey key,
                                                    GTRegistrate registrate) {
        determineName(material, key);
        determineTextures(material, key, material.getModid());

        if (name == null) {
            throw new IllegalStateException("Could not determine fluid name");
        }

        if (state == null) {
            if (key != null && key.getDefaultFluidState() != null) {
                state = key.getDefaultFluidState();
            } else {
                state = FluidState.LIQUID; // default fallback
            }
        }

        determineTemperature(material);
        determineColor(material);
        determineDensity();
        determineLuminosity(material);
        determineViscosity(material);
        IGTFluidBuilder builder = registrate
                .createFluid(name, this.translation != null ? this.translation : key.getTranslationKeyFor(material),
                        material, this.still, this.flowing)
                .temperature(this.temperature)
                .density(this.density)
                .luminance(this.luminosity)
                .viscosity(this.viscosity)
                .hasBlock(this.hasFluidBlock)
                .hasBucket(this.hasBucket)
                .burnTime(this.burnTime)
                .state(this.state);

        if (isColorEnabled) {
            builder.color(this.color);
        }

        builder.onFluidRegister(fluid -> {
            if (fluid instanceof FlowingFluid flowingFluid) {
                if (flowingFluid.getSource() instanceof GTFluid gtSource) attributes.forEach(gtSource::addAttribute);
                if (flowingFluid.getFlowing() instanceof GTFluid gtFlowing) attributes.forEach(gtFlowing::addAttribute);
            }
        });

        return builder.registerFluid();
    }

    private void determineName(@NotNull Material material, @Nullable FluidStorageKey key) {
        if (name != null) return;
        if (key == null) throw new IllegalArgumentException("Fluid must have a name");
        name = key.getRegistryNameFor(material);
    }

    private void determineTextures(@NotNull Material material, @Nullable FluidStorageKey key, @NotNull String modid) {
        IconSetTextures setTextures = explicitTextures != null ? explicitTextures :
                key != null && state == FluidState.LIQUID && !hasCustomStill ? findIconSetTextures(material) : null;
        if (setTextures != null) {
            still = setTextures.still();
            if (!setTextures.tinted() && !colorChosen) isColorEnabled = false;
        } else if (key != null) {
            if (hasCustomStill) {
                still = ResourceLocation.fromNamespaceAndPath(modid, "block/fluids/fluid." + name);
            } else {
                MaterialIconType icon = key == FluidStorageKeys.LIQUID && material.requiresMetalFreezing() ?
                        MaterialIconType.molten : key.getIconType();
                still = icon.getBlockTexturePath(material.getMaterialIconSet(), true);
            }
        } else {
            still = ResourceLocation.fromNamespaceAndPath(modid, "block/fluids/fluid." + name);
        }

        if (hasCustomFlowing) {
            flowing = ResourceLocation.fromNamespaceAndPath(modid, "block/fluids/fluid." + name + "_flow");
        } else if (setTextures != null) {
            flowing = setTextures.flowing();
        } else {
            flowing = still;
        }
    }

    /** The textures of the nearest icon set, this fluid's own or its material's, going up through the parents. */
    private @Nullable IconSetTextures findIconSetTextures(Material material) {
        MaterialIconSet set = iconSet != null ? iconSet : material.getMaterialIconSet();
        while (set != null) {
            IconSetTextures textures = ICONSET_TEXTURES.get(set.id);
            if (textures != null) return textures;
            set = set.parentIconset;
        }
        return null;
    }

    private void determineTemperature(@NotNull Material material) {
        if (temperature != INFER_TEMPERATURE) return;
        BlastProperty property = material.getProperty(PropertyKey.BLAST);
        if (property == null) {
            temperature = switch (state) {
                case LIQUID -> {
                    if (material.hasProperty(PropertyKey.DUST)) {
                        yield SOLID_LIQUID_TEMPERATURE;
                    }
                    yield ROOM_TEMPERATURE;
                }
                case GAS -> ROOM_TEMPERATURE;
                case PLASMA -> {
                    if (material.hasFluid() && material.getFluidBuilder() != null &&
                            material.getFluidBuilder() != material.getFluidBuilder(FluidStorageKeys.PLASMA)) {
                        yield BASE_PLASMA_TEMPERATURE + material.getFluidBuilder().temperature;
                    }
                    yield BASE_PLASMA_TEMPERATURE;
                }
            };
        } else {
            temperature = property.getBlastTemperature() + switch (state) {
                case LIQUID -> LIQUID_TEMPERATURE_OFFSET;
                case GAS -> GAS_TEMPERATURE_OFFSET;
                case PLASMA -> BASE_PLASMA_TEMPERATURE;
            };
        }
    }

    private void determineColor(@NotNull Material material) {
        if (color != INFER_COLOR) return;
        if (isColorEnabled) {
            color = GTUtil.convertRGBtoARGB(material.getMaterialRGB());
        }
    }

    private void determineDensity() {
        if (density != INFER_DENSITY) return;
        density = switch (state) {
            case LIQUID -> DEFAULT_LIQUID_DENSITY;
            case GAS -> DEFAULT_GAS_DENSITY;
            case PLASMA -> DEFAULT_PLASMA_DENSITY;
        };
    }

    private void determineLuminosity(@NotNull Material material) {
        if (luminosity != INFER_LUMINOSITY) return;
        if (state == FluidState.PLASMA) {
            luminosity = 15;
        } else if (material.hasFlag(MaterialFlags.PHOSPHORESCENT)) {
            luminosity = 15;
        } else if (state == FluidState.LIQUID && material.hasProperty(PropertyKey.DUST)) {
            // liquids only glow if not phosphorescent
            luminosity = 10;
        } else {
            luminosity = 0;
        }
    }

    private void determineViscosity(@NotNull Material material) {
        if (viscosity != INFER_VISCOSITY) return;
        viscosity = switch (state) {
            case LIQUID -> {
                if (material.hasFlag(MaterialFlags.STICKY)) {
                    yield STICKY_LIQUID_VISCOSITY;
                }
                yield DEFAULT_LIQUID_VISCOSITY;
            }
            case GAS -> DEFAULT_GAS_VISCOSITY;
            case PLASMA -> DEFAULT_PLASMA_VISCOSITY;
        };
    }
}
