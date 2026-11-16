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
import com.gregtechceu.gtceu.api.registry.registrate.GTClientFluidTypeExtensions;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.common.block.MaterialFluidBlock;
import com.gregtechceu.gtceu.common.item.GTBucketItem;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;

import com.google.common.base.Preconditions;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.OneTimeEventReceiver;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
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
import java.util.Objects;
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

    public static void registerIconSetTextures(Holder<MaterialIconSet> iconSet, ResourceLocation still,
                                               ResourceLocation flowing, boolean tinted) {
        registerIconSetTextures(iconSet.value().id, still, flowing, tinted);
    }

    private static final int INFER_TEMPERATURE = -1;
    private static final int INFER_COLOR = 0xFFFFFFFF;
    private static final int INFER_DENSITY = -1;
    private static final int INFER_LUMINOSITY = -1;
    private static final int INFER_VISCOSITY = -1;

    @Setter
    @Nullable
    private String name = null;
    @Setter
    @Nullable
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
    private Holder<MaterialIconSet> iconSet = null;
    @Nullable
    private IconSetTextures explicitTextures = null;
    @Setter
    private int density = INFER_DENSITY;
    private int luminosity = INFER_LUMINOSITY;
    private int viscosity = INFER_VISCOSITY;
    @Setter
    private int burnTime = 0;

    @Getter
    @Setter(onMethod_ = @ApiStatus.Internal)
    @Nullable
    private ResourceLocation still = null;
    @Getter
    @Setter(onMethod_ = @ApiStatus.Internal)
    @Nullable
    private ResourceLocation flowing = null;
    @Getter
    private boolean hasCustomStill = false;
    @Getter
    private boolean hasCustomFlowing = false;

    @Getter
    private boolean hasFluidBlock = false;
    private boolean hasBucket = true;

    public FluidBuilder() {}

    /**
     * @param temperature the temperature of the fluid in Kelvin
     * @return this;
     */
    public FluidBuilder temperature(int temperature) {
        Preconditions.checkArgument(temperature > 0, "temperature must be > 0");
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
    public FluidBuilder color(int color) {
        this.colorChosen = true;
        this.color = GTUtil.convertRGBtoARGB(color);
        if (this.color == INFER_COLOR) {
            return disableColor();
        }
        return this;
    }

    /**
     * Disables coloring the fluid. A color should still be specified.
     *
     * @return this
     */
    /**
     * Picks the textures for this fluid from an icon set of its own, instead of the one its material uses for its
     * items.
     * An icon set with registered fluid textures (see {@link #registerIconSetTextures}), such as
     * {@link MaterialIconSet#LAVA} and {@link MaterialIconSet#WATER}, gives the fluid those; any other uses the usual
     * material icon set paths. A colour set with {@link #color(int)} still tints the fluid, and so does
     * {@link #customStill()} replace the icon set's texture.
     */
    public FluidBuilder iconSet(Holder<MaterialIconSet> iconSet) {
        this.iconSet = iconSet;
        return this;
    }

    /**
     * Same, looking the icon set up by its id, for scripts and for sets that are registered by name. It has a name of
     * its own because scripts cannot tell it from {@link #iconSet(Holder)}, as both take text there. The set has to be
     * registered already, which it is by the time materials are modified.
     *
     * @throws IllegalArgumentException if there is no such icon set
     */
    public FluidBuilder iconSetId(ResourceLocation iconSetId) {
        var holder = com.gregtechceu.gtceu.api.registry.GTRegistries.MATERIAL_ICON_SETS.getHolder(
                net.minecraft.resources.ResourceKey.create(
                        com.gregtechceu.gtceu.api.registry.GTRegistries.Keys.MATERIAL_ICON_SET, iconSetId));
        if (holder.isEmpty()) throw new IllegalArgumentException("No material icon set " + iconSetId);
        return iconSet(holder.get());
    }

    /**
     * Gives this one fluid textures of its own, from any mod. Unlike {@link #customStill()}, which looks for files
     * named
     * after the fluid in its material's mod, these can be anywhere, so an addon can retexture a fluid it did not make.
     * They take priority over {@link #iconSet}.
     *
     * @param flowing the flowing texture, or null to use the still one
     * @param tinted  whether the fluid's colour tints them
     */
    public FluidBuilder textures(ResourceLocation still, @Nullable ResourceLocation flowing, boolean tinted) {
        this.explicitTextures = new IconSetTextures(still, flowing == null ? still : flowing, tinted);
        return this;
    }

    public FluidBuilder disableColor() {
        this.isColorEnabled = false;
        return this;
    }

    /**
     * @param density the density in g/cm^3
     * @return this
     */
    @Tolerate
    public FluidBuilder density(double density) {
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
    public FluidBuilder luminosity(int luminosity) {
        Preconditions.checkArgument(luminosity >= 0 && luminosity < 16, "luminosity must be >= 0 and < 16");
        this.luminosity = luminosity;
        return this;
    }

    /**
     * @param mcViscosity the MC viscosity of the fluid
     * @return this
     */
    public FluidBuilder viscosity(int mcViscosity) {
        Preconditions.checkArgument(mcViscosity >= 0, "viscosity must be >= 0");
        this.viscosity = mcViscosity;
        return this;
    }

    /**
     * @param viscosity the viscosity of the fluid in Poise
     * @return this
     */
    public FluidBuilder viscosity(double viscosity) {
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
    public FluidBuilder attribute(FluidAttribute attribute) {
        this.attributes.add(attribute);
        return this;
    }

    /**
     * @param attributes the attributes to add
     * @return this
     */
    public FluidBuilder attributes(FluidAttribute @NotNull... attributes) {
        Collections.addAll(this.attributes, attributes);
        return this;
    }

    /**
     * Mark this fluid as having a custom still texture
     *
     * @return this
     */
    public FluidBuilder customStill() {
        return textures(true);
    }

    /**
     * @param hasCustomStill if the fluid has a custom still texture
     * @return this
     */
    public FluidBuilder textures(boolean hasCustomStill) {
        this.hasCustomStill = hasCustomStill;
        this.isColorEnabled = false;
        return this;
    }

    /**
     * @param hasCustomStill   if the fluid has a custom still texture
     * @param hasCustomFlowing if the fluid has a custom flowing texture
     * @return this
     */
    public FluidBuilder textures(boolean hasCustomStill, boolean hasCustomFlowing) {
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
    public FluidBuilder block() {
        this.hasFluidBlock = true;
        return this;
    }

    /**
     * Disables the auto-generated fluid bucket for the fluid
     *
     * @return this
     */
    public FluidBuilder disableBucket() {
        this.hasBucket = false;
        return this;
    }

    @SuppressWarnings("UnstableApiUsage")
    public Supplier<? extends Fluid> build(Material material, FluidStorageKey key, GTRegistrate registrate) {
        Objects.requireNonNull(material, "Fluid builder does not have a material");
        determineName(material, key);
        determineTextures(material, key);

        if (name == null) {
            throw new IllegalStateException("Could not determine fluid name");
        }

        // noinspection ConstantValue: in case of mistakes
        if (state == null) {
            if (key.getDefaultFluidState() != null) {
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

        // noinspection DataFlowIssue
        var builder = registrate.fluid(this.name, this.still, this.flowing,
                (p, $1, $2) -> makeFluidType(registrate, p, material, key),
                (p) -> new GTFluid.Flowing(this.state, this.burnTime, p))
                .source((p) -> new GTFluid.Source(this.state, this.burnTime, p))
                .properties(this::setupFluidTypeProperties)
                .fluidProperties(this::setupFluidProperties)
                .setData(ProviderType.LANG, NonNullBiConsumer.noop());
        if (this.hasFluidBlock) {
            builder.block(MaterialFluidBlock::new)
                    .setData(ProviderType.LANG, NonNullBiConsumer.noop())
                    .properties(p -> p.mapColor(GTUtil.determineMapColor(material.getMaterialRGB())))
                    .color(() -> () -> (state, level, pos, index) -> {
                        return IClientFluidTypeExtensions.of(state.getFluidState())
                                .getTintColor(state.getFluidState(), level, pos);
                    })
                    .setData(ProviderType.LANG, NonNullBiConsumer.noop())
                    .register();
        } else {
            builder.noBlock();
        }
        if (this.hasBucket) {
            builder.bucket(GTBucketItem::new)
                    .properties(p -> p.craftRemainder(Items.BUCKET).stacksTo(1))
                    .setData(ProviderType.LANG, NonNullBiConsumer.noop())
                    .setData(ProviderType.ITEM_MODEL, NonNullBiConsumer.noop())
                    .color(() -> () -> GTBucketItem::color)
                    .register();
        } else {
            builder.noBucket();
        }

        builder.onRegister(fluid -> {
            if (fluid.getSource() instanceof GTFluid gtSource) attributes.forEach(gtSource::addAttribute);
            if (fluid.getFlowing() instanceof GTFluid gtFlowing) attributes.forEach(gtFlowing::addAttribute);
        });
        return builder.register()::getSource;
    }

    private void determineName(Material material, @Nullable FluidStorageKey key) {
        if (name != null) return;
        if (key == null) throw new IllegalArgumentException("Fluid must have a name");
        name = key.getRegistryNameFor(material);
    }

    @ApiStatus.Internal
    public void determineTextures(Material material, FluidStorageKey key) {
        IconSetTextures setTextures = explicitTextures != null ? explicitTextures :
                state == FluidState.LIQUID && !hasCustomStill ? findIconSetTextures(material) : null;
        if (setTextures != null) {
            still = setTextures.still();
            if (!setTextures.tinted() && !colorChosen) isColorEnabled = false;
        } else if (hasCustomStill) {
            still = ResourceLocation.fromNamespaceAndPath(material.getModid(), "block/fluids/fluid." + name);
        } else {
            MaterialIconType icon = key == FluidStorageKeys.LIQUID && material.requiresMetalFreezing() ?
                    MaterialIconType.molten : key.getIconType();
            still = icon.getBlockTexturePath(material.getMaterialIconSet(), true);
        }

        if (hasCustomFlowing) {
            flowing = ResourceLocation.fromNamespaceAndPath(material.getModid(),
                    "block/fluids/fluid." + name + "_flow");
        } else if (setTextures != null) {
            flowing = setTextures.flowing();
        } else {
            // FIXME this is actually wrong, flowing fluids should have 32x32 textures (double the size of still ones).
            flowing = still;
        }
    }

    /** The textures of the nearest icon set, this fluid's own or its material's, going up through the parents. */
    private @Nullable IconSetTextures findIconSetTextures(Material material) {
        MaterialIconSet set = iconSet != null ? iconSet.value() : material.getMaterialIconSet();
        while (set != null) {
            IconSetTextures textures = ICONSET_TEXTURES.get(set.id);
            if (textures != null) return textures;
            set = set.parentIconset == null ? null : set.parentIconset.value();
        }
        return null;
    }

    private void determineTemperature(Material material) {
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

    private void determineColor(Material material) {
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

    private void determineLuminosity(Material material) {
        if (luminosity != INFER_LUMINOSITY) return;
        if (state == FluidState.PLASMA) {
            luminosity = 15;
        } else {
            if (material.hasFlag(MaterialFlags.PHOSPHORESCENT)) {
                luminosity = 15;
            } else if (state == FluidState.LIQUID && material.hasProperty(PropertyKey.DUST)) {
                // liquids only glow if not phosphorescent
                luminosity = 10;
            } else {
                luminosity = 0;
            }
        }
    }

    private void determineViscosity(Material material) {
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

    private FluidType.Properties setupFluidTypeProperties(FluidType.Properties properties) {
        return properties.sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                .temperature(this.temperature)
                .density(this.density)
                .lightLevel(this.luminosity)
                .viscosity(this.viscosity);
    }

    @SuppressWarnings("DataFlowIssue")
    private BaseFlowingFluid.Properties setupFluidProperties(BaseFlowingFluid.Properties properties) {
        if (!this.hasFluidBlock) properties.block(null);
        if (!this.hasBucket) properties.bucket(null);

        // limit flow rate a maximum to 1 tick per step
        // (0 or negative values don't cause issues AFAIK, but they aren't good either.)
        return properties.tickRate(Math.max(this.viscosity / 200, 1));
    }

    private FluidType makeFluidType(AbstractRegistrate<?> owner, FluidType.Properties properties,
                                    Material material, FluidStorageKey key) {
        // sadly, we have to apply this here instead of in setupFluidTypeProperties because Registrate overwrites
        // whatever we assign there.
        final String langKey = this.translation != null ? this.translation : key.getTranslationKeyFor(material);
        properties.descriptionId(langKey);

        FluidType type = new GTFluidType(properties, material);
        OneTimeEventReceiver.addModListener(owner, RegisterClientExtensionsEvent.class, event -> {
            final int color = isColorEnabled ? this.color : INFER_COLOR;
            if (still == null || flowing == null) {
                this.determineTextures(material, key);
            }
            event.registerFluidType(new GTClientFluidTypeExtensions(still, flowing, color), type);
        });
        return type;
    }
}
