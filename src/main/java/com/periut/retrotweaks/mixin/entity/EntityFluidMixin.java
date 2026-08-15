package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Restores the pre-beta-1.6 rule where a fluid is entered from its south-east corner.
 *
 * <p>Vanilla shrinks the entity's box before testing for fluid, which is what makes you enter water
 * from any side. Dropping that shrink brings back the corner-only behaviour. From BetaTweaks.
 */
@Mixin(Entity.class)
public class EntityFluidMixin {

	@Inject(method = "checkWaterCollisions", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$southEastWater(CallbackInfoReturnable<Boolean> cir) {
		if (!Config.GAMEPLAY.oldFeatures.allowSouthEastRule) return;
		Entity self = (Entity) (Object) this;
		Box box = self.boundingBox.expand(0.0, -0.4000000059604645, 0.0);
		cir.setReturnValue(self.world.updateMovementInFluid(box, Material.WATER, self));
	}

	@Inject(method = "isTouchingLava", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$southEastLava(CallbackInfoReturnable<Boolean> cir) {
		if (!Config.GAMEPLAY.oldFeatures.allowSouthEastRule) return;
		Entity self = (Entity) (Object) this;
		Box box = self.boundingBox.expand(0.0, -0.4000000059604645, 0.0);
		cir.setReturnValue(self.world.isMaterialInBox(box, Material.LAVA));
	}
}
