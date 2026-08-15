package com.periut.retrotweaks.mixin.blockentity;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.feature.fishing.Fishing;

import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cooking a raw fish keeps the size it was caught at, instead of coming out of the furnace as a
 * plain COOKED_FISH with the size reset to zero. From FishinFoodTweaks.
 *
 * <p>craftRecipe() builds the cooked stack fresh from SmeltingRecipeManager, so the source mod
 * captures the raw fish's damage (its size) at the head of craftRecipe(), before it runs, then
 * re-applies it once tick() sees the craft go through. It also remembers which of the four
 * non-vanilla species (see {@link Fishing}) was consumed, if any, so the cooked output is that
 * same species rather than plain COOKED_FISH - those items and their smelting recipes only exist
 * once {@link Fishing#registerNonVanillaFish()} has found RetroAPI; {@link Fishing#rawFishType}
 * reads as -1 for the vanilla fish (and for everything when it has not) so this degrades to the
 * single COOKED_FISH case exactly as before.
 *
 * <p>The source kept the captured size in a static field shared by every furnace in the world -
 * the same crosstalk bug fixed elsewhere in this mod (FurnaceLavaBucketMixin): two furnaces
 * finishing a fish in the same tick could hand the wrong size to the wrong one. This uses
 * per-instance state instead.
 */
@Mixin(FurnaceBlockEntity.class)
public abstract class FurnaceFishSizeMixin {

	@Shadow
	private ItemStack[] inventory;

	@Unique
	private boolean retrotweaks$rawFishConsumed;

	@Unique
	private int retrotweaks$cookedFishSize;

	/** -1 for vanilla RAW_FISH, otherwise an index into {@link Fishing#COOKED}. */
	@Unique
	private int retrotweaks$cookedFishType = -1;

	@Inject(method = "craftRecipe", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$captureFishSize(CallbackInfo ci) {
		if (Config.FISHING.randomFishSizes) {
			ItemStack input = this.inventory[0];
			if (input != null && Fishing.isRawFish(input)) {
				retrotweaks$cookedFishSize = input.getDamage();
				retrotweaks$cookedFishType = Fishing.rawFishType(input.itemId);
				retrotweaks$rawFishConsumed = true;
			}
		}
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void retrotweaks$applyFishSize(CallbackInfo ci) {
		if (retrotweaks$rawFishConsumed) {
			this.inventory[2] = retrotweaks$cookedFishType < 0
				? new ItemStack(Item.COOKED_FISH)
				: new ItemStack(Fishing.COOKED[retrotweaks$cookedFishType]);
			this.inventory[2].setDamage(retrotweaks$cookedFishSize);
			retrotweaks$rawFishConsumed = false;
			retrotweaks$cookedFishType = -1;
		}
	}
}
