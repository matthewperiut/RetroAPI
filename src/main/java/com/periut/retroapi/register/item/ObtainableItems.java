package com.periut.retroapi.register.item;

import net.minecraft.block.Block;
import net.minecraft.item.Item;

/**
 * Which ids name something a player can actually hold.
 *
 * <p>Beta's block list is full of forms that exist only as world states: the lit copy of a furnace,
 * the powered copy of a repeater, the unlit redstone torch, farmland, wheat, fire, flowing water, the
 * two halves of a piston mid-push. None of them is an item, and offering them where an ITEM is wanted
 * - {@code /give}, the creative search tab - is offering something that cannot be given.
 *
 * <p>Separate from "does this id exist", which is what a BLOCK argument wants: {@code /setblock
 * minecraft:fire} is a perfectly reasonable thing to type, and this list must not shrink it.
 *
 * <p>One list, used by both the creative screen and the command suggestions, so the two cannot drift
 * apart into "the tab hides it but the command offers it".
 */
public final class ObtainableItems {
    private ObtainableItems() {
    }

    /**
     * Blocks that exist only as world states: fluids, fire, the piston's moving parts, the lit variants
     * a block switches to itself, the double slab two slabs become. Modern has no items for any of them
     * either, and a creative tab full of them is how a backport looks unfinished.
     */
    public static boolean isTechnicalBlock(final int blockId) {
        return blockId == Block.FLOWING_WATER.id || blockId == Block.WATER.id
            || blockId == Block.FLOWING_LAVA.id || blockId == Block.LAVA.id
            || blockId == Block.FIRE.id || blockId == Block.NETHER_PORTAL.id
            || blockId == Block.PISTON_HEAD.id || blockId == Block.MOVING_PISTON.id
            || blockId == Block.DOUBLE_SLAB.id
            || blockId == Block.LIT_FURNACE.id || blockId == Block.LIT_REDSTONE_ORE.id
            || blockId == Block.REDSTONE_TORCH.id
            || blockId == Block.WALL_SIGN.id || blockId == Block.LOCKED_CHEST.id
            || blockId == Block.FARMLAND.id || blockId == Block.WHEAT.id
            || blockId == Block.SNOW.id
            || blockId == Block.POWERED_REPEATER.id;
    }

    /**
     * Blocks whose item is a different id entirely - a door block is placed by a door item, a repeater
     * block by a repeater item. The block id is not the thing to hand out; the item id is, and it is in
     * the list on its own.
     */
    public static boolean hasSeparateItem(final int blockId) {
        return blockId == Block.BED.id || blockId == Block.DOOR.id || blockId == Block.IRON_DOOR.id
            || blockId == Block.SIGN.id || blockId == Block.CAKE.id
            || blockId == Block.REPEATER.id || blockId == Block.SUGAR_CANE.id
            || blockId == Block.REDSTONE_WIRE.id;
    }

    /** Whether this id can be given to a player: an item, or a block that has one. */
    public static boolean isObtainable(final int id) {
        if (id <= 0 || id >= Item.ITEMS.length || Item.ITEMS[id] == null) {
            return false;
        }
        if (id < Block.BLOCKS.length && Block.BLOCKS[id] != null) {
            return !isTechnicalBlock(id) && !hasSeparateItem(id);
        }
        return true;
    }
}
