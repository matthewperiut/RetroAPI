package com.periut.retroapi.mixin.client;

import com.periut.retroapi.register.block.RetroTextures;
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
 * <p>
 * A block item that has been given an icon of its own with
 * {@link RetroTextures#setItemSprite} keeps it: see {@code getTextureId} below.
 */
@Mixin(BlockItem.class)
public abstract class BlockItemTextureMixin extends Item {

	@Shadow private int blockId;

	private BlockItemTextureMixin(int i) {
		super(i);
	}

	@Override
	public int getTextureId(int damage) {
		// Unless the item was given a sprite of its own. Following the block is a repair for a snapshot
		// taken too early, not a rule that a block item may not have its own icon, and it has to yield to
		// someone who has actually said what they want. Without this, setTextureId on a modded block item
		// is silently ignored: the icon follows the block no matter what anyone sets, which is a hard
		// thing to see, because the sprite it falls back to is a real one that draws perfectly.
		if (RetroTextures.hasOwnItemSprite(this.id)) {
			return super.getTextureId(damage);
		}
		if (blockId >= 256 && blockId < Block.BLOCKS.length) {
			Block block = Block.BLOCKS[blockId];
			if (block != null) {
				return block.getTexture(2, damage);
			}
		}
		return super.getTextureId(damage);
	}
}
