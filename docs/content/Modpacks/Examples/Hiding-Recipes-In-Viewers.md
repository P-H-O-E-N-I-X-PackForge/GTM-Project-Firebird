---
title: "Hiding Recipes in the Recipe Viewers"
---


# Hiding Recipes in the Recipe Viewers

Rules decide which recipes EMI and JEI show. A rule is called for every recipe, with the id of its category, the id of
the recipe, and the recipe itself (a GT recipe for GT recipe categories), and returns `false` to hide it. A script
that adds a rule runs at startup, so it applies when the viewers load.

=== "Java"
    ```java title="AddonMod.java"
    GTViewerVisibility.addRule((category, id, entry) ->
            !(entry instanceof GTRecipe recipe) || !id.getPath().startsWith("hidden/"));
    ```
=== "JavaScript"
    ```js title="hide_recipes.js"
    // hide every recipe whose id starts with "hidden/"
    GTViewerVisibility.addRule((category, id, entry) => !id.path.startsWith('hidden/'))
    ```

## When what a rule reads changes

If a rule depends on something that can change while the game runs, such as research or a setting, call `refresh()`
after it changes. JEI hides and shows only the recipes affected. EMI cannot do that, so it rebuilds its recipes, which
takes longer: call it when something has changed, and not every tick.

=== "Java"
    ```java
    GTViewerVisibility.refresh();
    ```
=== "JavaScript"
    ```js
    GTViewerVisibility.refresh()
    ```
