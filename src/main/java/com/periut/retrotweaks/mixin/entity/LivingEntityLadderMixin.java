package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;

import net.minecraft.block.Block;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ladders work with a one-block gap, as they did before beta 1.6. From BetaTweaks and UniTweaks.
 *
 * <p>Vanilla only checks the block at the entity's feet; checking the one above as well is what
 * lets a climber bridge a missing rung.
 */
@Mixin(LivingEntity.class)
public class LivingEntityLadderMixin {

	@Inject(method = "isOnLadder", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$ladderGaps(CallbackInfoReturnable<Boolean> cir) {
		if (!Config.GAMEPLAY.oldFeatures.allowGapsInLadders) return;
		LivingEntity self = (LivingEntity) (Object) this;
		int x = MathHelper.floor(self.x);
		int y = MathHelper.floor(self.boundingBox.minY);
		int z = MathHelper.floor(self.z);
		cir.setReturnValue(self.world.getBlockId(x, y, z) == Block.LADDER.id
			|| self.world.getBlockId(x, y + 1, z) == Block.LADDER.id);
	}
}
