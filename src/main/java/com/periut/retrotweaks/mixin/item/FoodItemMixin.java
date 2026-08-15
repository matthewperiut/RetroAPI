package com.periut.retrotweaks.mixin.item;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.feature.fishing.Fishing;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FoodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Food behaviour: no eating at full health (UniTweaks, GameplayEssentials), fish healing scaled
 * by the size the fish was caught at (FishinFoodTweaks), and an eating/burp sound (UniTweaks, More
 * Sounds).
 *
 * <p>Uses {@code random.eat}/{@code random.burp}, not the custom sounds UniTweaks bundled under its
 * own namespace - RetroTweaks adds no assets, so a sound it cannot supply would simply be silent, same
 * as {@code MoreSoundsMixin} and {@code ChestSoundMixin} from this same feature.
 */
@Mixin(FoodItem.class)
public class FoodItemMixin {

	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$noFoodWastage(ItemStack stack, World world, PlayerEntity player, CallbackInfoReturnable<ItemStack> cir) {
		if (Config.GAMEPLAY.disableEatingAtMaxHealth && player.health >= 20) cir.setReturnValue(stack);
	}

	@Inject(method = "use", at = @At("RETURN"))
	private void retrotweaks$eatSound(ItemStack stack, World world, PlayerEntity user, CallbackInfoReturnable<ItemStack> cir) {
		if (!Config.SOUNDS.moreSounds) return;
		float pitch = (world.random.nextFloat() - world.random.nextFloat()) * 0.2F + 1.0F;
		world.playSound(user, "random.eat", 1.0F, pitch);
		if (world.random.nextInt(10) > 3) {
			float burpPitch = (world.random.nextFloat() - world.random.nextFloat()) * 0.2F + 1.0F;
			world.playSound(user, "random.burp", 0.5F, burpPitch);
		}
	}

	// FISHINFOODTWEAKS DISABLED - re-enable by uncommenting this whole method.
	//
	// Only THIS injector is FishinFoodTweaks; the two above it are not (disableEatingAtMaxHealth is a
	// Gameplay option, the eating sound is a Sounds one), which is why this file is commented out
	// method-by-method rather than being switched off wholesale in RetroTweaksMixinPlugin the way the
	// fish-only mixins are.
	//
	// @WrapOperation(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;heal(I)V"))
	// private void retrotweaks$fishHealsBySize(PlayerEntity player, int amount, Operation<Void> original,
	// 		ItemStack stack, World world, PlayerEntity user) {
	// 	if (Config.FISHING.randomFishSizes && Fishing.isFish(stack)) {
	// 		original.call(player, Fishing.healingFor(stack));
	// 		return;
	// 	}
	// 	original.call(player, amount);
	// }
}
