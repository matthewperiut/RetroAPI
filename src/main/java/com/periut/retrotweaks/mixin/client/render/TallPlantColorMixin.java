package com.periut.retrotweaks.mixin.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.client.color.world.GrassColors;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Gives tall grass and ferns their green back when they are drawn outside the world.
 *
 * <p>A block has two colours in b1.7.3. {@code getColorMultiplier(BlockView, x, y, z)} is the biome
 * tint and needs a world to look one up in; {@link Block#getColor(int)} is the one used when there is
 * no world - a block drawn as an item. {@code TallPlantBlock} overrides only the first, so the
 * greyscale grass sprite gets multiplied by {@code getColor}'s default of white and comes out grey.
 * {@code LeavesBlock} is the one vanilla block that overrides both, which is exactly why leaves look
 * right in the inventory and tall grass never has.
 *
 * <p>Meta 0 is left alone, matching {@code getColorMultiplier}: that meta is the dead shrub, and a dead
 * shrub is meant to be brown as an item exactly as it is in the world. This only tells grass and fern
 * apart from the shrub because {@code TallPlantBlockMixin.getDroppedItemMeta} keeps the variant on the
 * item - vanilla flattened all three to meta 0, which made them one indistinguishable stack and left
 * this guard excluding every item there was.
 *
 * <p>{@link GrassColors} is asked rather than a constant being baked in here, so a resource pack that
 * ships its own {@code grasscolor.png} tints the item exactly as it tints the world.
 *
 * <p>Separate from {@code mixin.block.TallPlantBlockMixin}, which targets the same class, because that
 * one is a common mixin and this needs a client-only colour map - see this file's entry in the
 * {@code client} list of {@code retrotweaks.mixins.json}.
 */
@Environment(EnvType.CLIENT)
@Mixin(TallPlantBlock.class)
public abstract class TallPlantColorMixin extends PlantBlock {

	protected TallPlantColorMixin(int id, int textureId) {
		super(id, textureId);
	}

	@Override
	public int getColor(int meta) {
		// The player's CHOSEN value, not the effective Config field: the option is suppressed when
		// UniTweaks is installed, but UniTweaks' grassblockitemfix never touches TallPlantBlock or
		// getColor at all - this white answer is what left short grass and ferns grey (icons, hand,
		// dropped AND the pickup animation: ItemTint funnels through getColor, so DroppedItemTintMixin
		// saw "white, no tint" and stood by). Same reasoning as ItemTint and StationApiItemColors.
		if (!com.periut.retrotweaks.config.ConfigManager.chosenBoolean("bugfixes.grassBlockItemFix", true)
				|| meta == 0) {
			return super.getColor(meta);
		}
		return GrassColors.getColor(0.5, 1.0);
	}
}
