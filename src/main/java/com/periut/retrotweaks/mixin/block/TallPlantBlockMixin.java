package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.feature.flora.TallPlantVariants;

import net.minecraft.block.PlantBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Tall grass and ferns: shears collect the plant itself, and the whole thing can be hidden.
 * From MiscTweaks and BetaTweaks.
 *
 * <p>"Hide long grass" is a visibility option for players who find the clutter distracting; it
 * removes the render and the hitbox but leaves the block in the world, so nothing is destroyed and
 * turning it back on restores everything.
 */
@Mixin(TallPlantBlock.class)
public abstract class TallPlantBlockMixin extends PlantBlock {

	protected TallPlantBlockMixin(int id, int textureId) {
		super(id, textureId);
	}

	@Unique
	private boolean retrotweaks$brokenByShears;

	@Override
	public void afterBreak(World world, PlayerEntity player, int x, int y, int z, int meta) {
		retrotweaks$brokenByShears = retrotweaks$shearsCollect(meta) && retrotweaks$usedShears(player);
		if (retrotweaks$brokenByShears) player.inventory.getSelectedItem().damage(1, player);
		super.afterBreak(world, player, x, y, z, meta);
		// Cleared here rather than inside getDroppedItemId, because dropStacks calls that AND
		// getDroppedItemMeta once per dropped item; clearing on the first would leave every drop after
		// it looking like a normal break.
		retrotweaks$brokenByShears = false;
	}

	@Inject(method = "getDroppedItemId", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$dropGrassBlock(int blockMeta, java.util.Random random, CallbackInfoReturnable<Integer> cir) {
		if (retrotweaks$brokenByShears) {
			cir.setReturnValue(this.id);
			return;
		}
		if (Config.WORLD.flora.hideLongGrass) cir.setReturnValue(-1);
	}

	/**
	 * Keeps the variant on the item when the plant is sheared into itself.
	 *
	 * <p>{@code Block.getDroppedItemMeta} returns 0 for everything and this block never overrode it, so
	 * a sheared plant became {@code id 31, meta 0} whatever it had been - and meta 0 of this block is
	 * the dead shrub. That flattening caused two visible bugs at once: the item drew with the shrub
	 * sprite instead of grass or fern, and placing it back turned tall grass into a shrub. It also made
	 * the three variants a single indistinguishable stack, so nothing downstream could tint grass and
	 * fern while leaving the shrub brown.
	 *
	 * <p>Only for the shears drop. A normal break yields seeds, and those must keep a damage of 0 - the
	 * block's meta would be a nonsense damage value on a seed.
	 */
	@Override
	protected int getDroppedItemMeta(int meta) {
		return retrotweaks$brokenByShears && Config.WORLD.flora.tallGrassItems
			? meta
			: super.getDroppedItemMeta(meta);
	}

	@Environment(EnvType.CLIENT)
	@Override
	public int getRenderType() {
		return Config.WORLD.flora.hideLongGrass ? 0 : super.getRenderType();
	}

	@Override
	public void updateBoundingBox(BlockView blockView, int x, int y, int z) {
		if (!Config.WORLD.flora.hideLongGrass) {
			super.updateBoundingBox(blockView, x, y, z);
			return;
		}
		this.setBoundingBox(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
	}

	/**
	 * Whether shears should collect this particular variant, which is two options rather than one:
	 * the fern has its own switch, because a player who wants ferns obtainable does not necessarily
	 * want every patch of short grass to become a stack of grass items.
	 *
	 * <p>Both drop RetroTweaks' own plant - {@code id 31} at the meta it was - and neither needs any
	 * other mod installed. An earlier version handed meta 2 over to BHCreative's separately registered
	 * fern item whenever that mod was present; it no longer does, because block 31's own metas are
	 * proper items in their own right now (see {@link TallPlantVariants}) and making the drop depend on
	 * which other mods are installed only made the same shears give you different things.
	 */
	@Unique
	private static boolean retrotweaks$shearsCollect(int meta) {
		return meta == TallPlantVariants.FERN
			? Config.WORLD.flora.shearsCollectFern
			: Config.WORLD.flora.shearsCollectTallGrass;
	}

	@Unique
	private static boolean retrotweaks$usedShears(PlayerEntity player) {
		if (player == null || player.inventory == null) return false;
		ItemStack held = player.inventory.getSelectedItem();
		return held != null && held.itemId == Item.SHEARS.id;
	}
}
