package com.periut.retroapi.mixin.client;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Live GUI/sprite texture lookup for extended-range block items.
 * <p>
 * Vanilla {@code BlockItem} snapshots {@code block.getTexture(2)} once in its
 * constructor. RetroAPI mods typically assign block textures AFTER
 * {@code register()} (which constructs the BlockItem), so the snapshot is a
 * stale slot 0 - flat-rendered blocks (torch/cross render types) then draw
 * terrain atlas entry 0 in inventories, in hand, and as dropped items.
 * (3D-rendered blocks are unaffected: BlockRenderer queries the block live.)
 * <p>
 * Querying the block live removes the registration-order dependency entirely,
 * follows StationAPI atlas re-resolution ({@code RetroTextures.resolveStationAPITextures}
 * updates {@code block.textureId}), and follows ID remaps ({@code IdAssigner}
 * updates {@code blockId}). Vanilla blocks (id &lt; 256) keep the snapshot
 * behavior bit-for-bit.
 */
@Mixin(BlockItem.class)
public abstract class BlockItemTextureMixin extends Item {

	@Shadow private int blockId;

	private BlockItemTextureMixin(int i) {
		super(i);
	}

	@Override
	public int getTextureId(int damage) {
		if (blockId >= 256 && blockId < Block.BLOCKS.length) {
			Block block = Block.BLOCKS[blockId];
			if (block != null) {
				return block.getTexture(2, damage);
			}
		}
		return super.getTextureId(damage);
	}
}
