package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.item.Item;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pig drops: the saddle comes back when a saddled pig dies (AnnoyanceFix and UniTweaks), and pigs
 * can drop brown mushrooms as they did in early versions (BetaTweaks).
 */
@Mixin(PigEntity.class)
public abstract class PigEntityMixin {

	@Shadow
	public abstract boolean isSaddled();

	@Inject(method = "getDroppedItemId", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$pigDrops(CallbackInfoReturnable<Integer> cir) {
		PigEntity self = (PigEntity) (Object) this;
		if (Config.MOBS.pigSaddleDropFix && isSaddled()) {
			// Dropped separately rather than replacing the porkchop - vanilla only supports one
			// drop id, and losing the meat to get the saddle back would be a worse trade.
			self.dropItem(Item.SADDLE.id, 1);
		}
		if (Config.GAMEPLAY.oldFeatures.pigsDropBrownMushrooms) {
			cir.setReturnValue(Block.BROWN_MUSHROOM.id);
		}
	}
}
