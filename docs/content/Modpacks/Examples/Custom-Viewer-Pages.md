---
title: "Custom Recipe Viewer Pages"
---


# Custom Recipe Viewer Pages and Preview Widgets

Scripts can add their own page type to the recipe viewers (EMI and JEI), and put their own widgets into the multiblock
preview. Both are client side, so put these in a startup script; they run on a dedicated server too, but do nothing.

## A page type

A page is described by data. One function gives the entries to show a page for, another gives the page's id, and another
gives its layout as a plain object. The inputs and outputs of the layout are also what the viewers' "uses" and
"recipes" lookups use to find the page.

```js title="heat_sources_page.js"
GTViewerPages.create('kubejs:heat_sources')
    .icon('minecraft:magma_block')
    .title('Heat Sources')
    .entries(() => [
        { block: 'minecraft:magma_block', heat: 300 },
        { block: 'minecraft:lava_bucket', heat: 1200 }
    ])
    .id(e => 'kubejs:heat/' + e.block.replace(':', '_'))
    .layout(e => ({
        title: 'Heat source',
        inputs: [e.block],
        lines: ['Heat: ' + e.heat, 'Burns for a while']
    }))
    .workstation('minecraft:furnace')
    .register()
```

What the layout object can contain:

| Key | Meaning |
|---|---|
| `title` | Bold title text |
| `inputs`, `outputs` | Lists of items (an item stack, or text like `'3x minecraft:iron_ingot'`) or fluid stacks. Shown as slots with an arrow between. |
| `lines` | A list of text lines under the slots |

Other options on the builder: `maxSize(width, height)` (JEI), `noRecipeTree()` (a page that isn't about crafting),
`hideCraftable()`, `listFirst()` (tab before the GT recipe categories), and `enabledIf(() => bool)`.

!!! note
    `register()` has to run before the viewers load, so use a startup script.

## Widgets in the multiblock preview

`GTPreviewExtensions.register(slot, appliesTo, builder)` adds a widget to a multiblock's preview, in the recipe viewers
and in the terminal. `appliesTo` says which multiblocks, and `builder` is called each time a preview is made and returns
the widget, or `null` for none.

| Slot | Where |
|---|---|
| `GTPreviewSlot.TOOLBAR` | The row of buttons across the top |
| `GTPreviewSlot.LEFT` | A column left of the 3D view |
| `GTPreviewSlot.RIGHT` | A column right of the parts list |
| `GTPreviewSlot.BELOW` | Under the 3D view and the parts list |
| `GTPreviewSlot.OVERLAY` | Over the 3D view, placed from its top-left |

`GTViewerWidgets` has what you need to build the widgets: `text`, `lang`, `dynamicText`, `column`, `row`, `section`,
`gap`, `itemSlot`, `fluidSlot` and `button`.

```js title="preview_hint.js"
GTPreviewExtensions.register(GTPreviewSlot.BELOW,
    def => def.id.toString() == 'gtceu:electric_blast_furnace',
    preview => GTViewerWidgets.section('Tip',
        GTViewerWidgets.text('Use a coil of at least the recipe temperature.'),
        GTViewerWidgets.row(
            GTViewerWidgets.itemSlot('minecraft:blaze_rod'),
            GTViewerWidgets.text('and a lot of power'))))
```
