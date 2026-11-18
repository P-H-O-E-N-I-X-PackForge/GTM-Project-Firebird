package com.gregtechceu.gtceu.api.multiblock.error;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;

import brachy.modularui.drawable.ItemDrawable;
import brachy.modularui.widgets.ListWidget;
import brachy.modularui.widgets.layout.Flow;
import brachy.modularui.widgets.menu.Menu;

import java.util.List;

/** The menus of the pattern errors. */
final class PatternErrorMenus {

    private PatternErrorMenus() {}

    /** The items, in rows as wide as they need to be, which beats one long column of them. */
    static Menu<?> blockGrid(List<Item> items) {
        int columns = Mth.clamp((int) Math.ceil(Math.sqrt(items.size() * 1.5)), 3, 8);
        ListWidget<brachy.modularui.api.widget.IWidget, ?> list = new ListWidget<>().widthRel(1f)
                .coverChildrenHeight();
        for (int i = 0; i < items.size(); i += columns) {
            Flow row = Flow.row().coverChildren().childPadding(2);
            for (int j = i; j < Math.min(items.size(), i + columns); j++) {
                Item item = items.get(j);
                row.child(new ItemDrawable(item).asIcon().size(16).asWidget().size(16)
                        .tooltip(r -> r.add(Component.translatable(item.getDescriptionId()))));
            }
            list.child(row);
        }
        return new Menu<>().width(columns * 18 + 6).coverChildrenHeight().child(list);
    }
}
