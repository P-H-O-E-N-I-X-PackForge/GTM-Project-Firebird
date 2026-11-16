---
title: Icon Sets
---


# Icon Sets

The material system uses icon sets to determine the textures of generated blocks and items.


## Available Icon Sets

The following icon sets are available by default:

- `GTMaterialIconSet.BRIGHT`
- `GTMaterialIconSet.CERTUS`
- `GTMaterialIconSet.DIAMOND`
- `GTMaterialIconSet.DULL`
- `GTMaterialIconSet.EMERALD`
- `GTMaterialIconSet.FINE`
- `GTMaterialIconSet.FLINT`
- `GTMaterialIconSet.FLUID`
- `GTMaterialIconSet.GAS`
- `GTMaterialIconSet.GEM_HORIZONTAL`
- `GTMaterialIconSet.GEM_VERTICAL`
- `GTMaterialIconSet.GLASS`
- `GTMaterialIconSet.LAPIS`
- `GTMaterialIconSet.LIGNITE`
- `GTMaterialIconSet.MAGNETIC`
- `GTMaterialIconSet.METALLIC`
- `GTMaterialIconSet.NETHERSTAR`
- `GTMaterialIconSet.OPAL`
- `GTMaterialIconSet.QUARTZ`
- `GTMaterialIconSet.ROUGH`
- `GTMaterialIconSet.RUBY`
- `GTMaterialIconSet.SAND`
- `GTMaterialIconSet.SHINY`
- `GTMaterialIconSet.WOOD`


## Custom Icon Sets

Custom iconsets can be specified as well, using the `gtceu:matieral_icon_set` event:
=== "Java"
    ```java title="AddonIconSets.java"
    public static final Holder<MaterialIconSet> STARRY = ADDON_REGISTRATE.materialIconSet("starry", MaterialIconSet.SHINY);
    ```
=== "JavaScript"
    ```js title="custom_iconsets.js"
    GTCEuStartupEvents.registry('gtceu:material_icon_set', event => {
        event.create('starry')
            .parentIconset('gtceu:shiny')
    })
    ```


## Fluid Icon Sets

A fluid's liquid can take its textures from an icon set of its own, instead of the one its material uses for its
items. An icon set gets fluid textures by registering them with the `FluidBuilder`. Two come with textures:

- `GTMaterialIconSet.WATER` uses vanilla's water textures, tinted by the fluid's color.
- `GTMaterialIconSet.LAVA` uses vanilla's lava textures, which are not tinted.

Give a fluid an icon set when you create it, or later with `modifyFluid`:

=== "Java"
    ```java title="AddonMaterials.java"
    // when creating the material
    .liquid(new FluidBuilder().iconSet(MaterialIconSet.LAVA))
    // tinted with your own color; a color you set always tints it
    .liquid(new FluidBuilder().iconSet(MaterialIconSet.WATER).color(0x44AA88))
    ```
=== "JavaScript"
    ```js title="materials.js"
    GTCEuStartupEvents.registry('gtceu:material', event => {
        event.create('ember_fluid')
            .liquid(new GTFluidBuilder().iconSet(GTMaterialIconSet.LAVA))
    })
    ```

Only the liquid takes the icon set's textures; gases and plasmas keep using the material's icon set paths.

### Textures for a custom icon set

After registering an icon set, register its fluid textures. They can be any texture in the block atlas, from any mod.
The last value says whether the fluid's color tints them: `true` for a gray texture that should take the color, `false`
for one that already has its colors. A flowing texture is normally twice as wide as the still one.

=== "Java"
    ```java title="AddonIconSets.java"
    public static final Holder<MaterialIconSet> EMBER = ADDON_REGISTRATE.materialIconSet("ember", MaterialIconSet.FLUID);

    // in your mod's constructor, before the fluids are built
    FluidBuilder.registerIconSetTextures(ResourceLocation.parse("mymod:ember"),
            ResourceLocation.parse("mymod:block/fluids/ember_still"),
            ResourceLocation.parse("mymod:block/fluids/ember_flow"), false);
    ```
=== "JavaScript"
    ```js title="fluid_iconsets.js"
    GTCEuStartupEvents.registry('gtceu:material_icon_set', event => {
        event.create('ember').parentIconset('gtceu:fluid')
    })

    // a startup script
    GTFluidBuilder.registerIconSetTextures('gtceu:ember',
        'kubejs:block/fluids/ember_still', 'kubejs:block/fluids/ember_flow', false)

    GTCEuStartupEvents.materialModification(event => {
        // a set registered from a script is given by its id; scripts register icon sets under `gtceu`
        GTMaterials.Iron.modifyFluid(GTFluidStorageKeys.LIQUID, fluid => fluid.iconSetId('gtceu:ember'))
    })
    ```

Registering textures for an icon set GT already has, such as `gtceu:metallic`, changes the liquid of every material that
uses it or one of its child sets. To retexture one fluid, use `modifyFluid` with `textures(...)` instead.
