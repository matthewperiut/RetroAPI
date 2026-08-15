package com.periut.retrotweaks.mixin.blockentity;

import com.periut.retrotweaks.config.Config;

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
 * Burning a lava bucket in a furnace leaves the empty bucket behind, as in modern Minecraft.
 *
 * <p>From GameplayEssentials, reworked: the original counted consumed buckets in a static field
 * shared by every furnace in the world, so two furnaces burning lava in the same tick could hand a
 * bucket to the wrong one. This looks at the slot before and after the tick on the furnace itself,
 * which has no such crosstalk.
 */
@Mixin(FurnaceBlockEntity.class)
public class FurnaceLavaBucketMixin {

	@Shadow
	private ItemStack[] inventory;

	@Unique
	private boolean retrotweaks$hadLavaBucket;

	@Inject(method = "tick", at = @At("HEAD"))
	private void retrotweaks$noteLavaBucket(CallbackInfo ci) {
		ItemStack fuel = this.inventory[1];
		retrotweaks$hadLavaBucket = Config.BUGFIXES.furnaceConsumeBucketFix
			&& fuel != null && fuel.itemId == Item.LAVA_BUCKET.id;
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void retrotweaks$returnEmptyBucket(CallbackInfo ci) {
		if (!retrotweaks$hadLavaBucket) return;
		retrotweaks$hadLavaBucket = false;
		// Only when the bucket actually went in: an unlit furnace ticks without consuming fuel.
		if (this.inventory[1] == null) this.inventory[1] = new ItemStack(Item.BUCKET);
	}
}
