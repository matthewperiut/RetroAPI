package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Punching sheep for wool, and white-only sheep. From BetaTweaks and UniTweaks. */
@Mixin(SheepEntity.class)
public abstract class SheepEntityMixin {

	@Shadow public abstract boolean isSheared();
	@Shadow public abstract void setSheared(boolean sheared);
	@Shadow public abstract int getColor();

	@Inject(method = "damage", at = @At("HEAD"))
	private void retrotweaks$punchForWool(Entity attacker, int amount, CallbackInfoReturnable<Boolean> cir) {
		if (!Config.GAMEPLAY.oldFeatures.punchSheepForWool) return;
		if (!(attacker instanceof PlayerEntity) || isSheared()) return;

		SheepEntity self = (SheepEntity) (Object) this;
		if (self.world.isRemote) return;

		setSheared(true);
		java.util.Random random = self.world.random;
		int count = 2 + random.nextInt(3);
		for (int i = 0; i < count; i++) {
			ItemEntity wool = self.dropItem(new ItemStack(Block.WOOL.id, 1, getColor()), 1.0F);
			wool.velocityY += random.nextFloat() * 0.05F;
			wool.velocityX += (random.nextFloat() - random.nextFloat()) * 0.1F;
			wool.velocityZ += (random.nextFloat() - random.nextFloat()) * 0.1F;
		}
	}

	@ModifyReturnValue(method = "generateDefaultColor", at = @At("RETURN"))
	private static int retrotweaks$alwaysWhite(int original) {
		return Config.MOBS.disableColoredSheepSpawning ? 0 : original;
	}

	/**
	 * A snip sound when shears take the wool. From UniTweaks (More Sounds). Uses
	 * {@code entity.sheep.shear}, not the custom sound UniTweaks bundled under its own namespace -
	 * see {@code MoreSoundsMixin}/{@code ChestSoundMixin} for why.
	 */
	@Inject(method = "interact", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;damage(ILnet/minecraft/entity/Entity;)V"))
	private void retrotweaks$shearSound(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
		if (!Config.SOUNDS.moreSounds) return;
		SheepEntity self = (SheepEntity) (Object) this;
		float pitch = (self.world.random.nextFloat() - self.world.random.nextFloat()) * 0.2F + 1.0F;
		self.world.playSound(player, "entity.sheep.shear", 1.0F, pitch);
	}
}
