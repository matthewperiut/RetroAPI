package com.periut.retroapi.itemgroup;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * What a tab is being filled with, handed to every {@code entries} callback.
 *
 * <p>Order is the order things are added, because a creative tab is a curated list and not a set.
 */
public final class ItemGroupEntries {

    private final List<ItemStack> stacks = new ArrayList<>();

    public void add(final ItemStack stack) {
        if (stack != null) {
            stacks.add(stack);
        }
    }

    public void add(final Item item) {
        if (item != null) {
            stacks.add(new ItemStack(item.id, 1, 0));
        }
    }

    public void add(final Block block) {
        if (block != null) {
            stacks.add(new ItemStack(block.id, 1, 0));
        }
    }

    /** A specific subtype - the metadata that tells one wool, log or dye from another. */
    public void add(final Item item, final int meta) {
        if (item != null) {
            stacks.add(new ItemStack(item.id, 1, meta));
        }
    }

    public void add(final Block block, final int meta) {
        if (block != null) {
            stacks.add(new ItemStack(block.id, 1, meta));
        }
    }

    /** Every subtype from {@code first} to {@code last} inclusive, which is how wool and dye are listed. */
    public void addRange(final Block block, final int first, final int last) {
        for (int meta = first; meta <= last; meta++) {
            add(block, meta);
        }
    }

    public void addRange(final Item item, final int first, final int last) {
        for (int meta = first; meta <= last; meta++) {
            add(item, meta);
        }
    }

    public List<ItemStack> getStacks() {
        return stacks;
    }
}
