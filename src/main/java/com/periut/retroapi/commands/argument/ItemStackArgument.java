package com.periut.retroapi.commands.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.periut.retroapi.text.Text;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** A parsed item reference: which item, and which subtype of it. */
public record ItemStackArgument(int itemId, int meta) {
    public static final DynamicCommandExceptionType UNKNOWN_ITEM = new DynamicCommandExceptionType(
        id -> Text.literal("Unknown item '" + id + "'"));

    public Item getItem() throws CommandSyntaxException {
        if (itemId < 0 || itemId >= Item.ITEMS.length || Item.ITEMS[itemId] == null) {
            throw UNKNOWN_ITEM.create(itemId);
        }
        return Item.ITEMS[itemId];
    }

    public ItemStack createStack(final int count) throws CommandSyntaxException {
        return new ItemStack(getItem(), count, meta);
    }

    public int getMaxCount() throws CommandSyntaxException {
        return getItem().getMaxCount();
    }

    public String getName() {
        return ItemIds.nameOf(itemId);
    }
}
