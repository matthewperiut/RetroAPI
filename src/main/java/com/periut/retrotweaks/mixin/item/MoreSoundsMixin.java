package com.periut.retrotweaks.mixin.item;

import com.periut.retrotweaks.config.Config;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.stat.Stat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A sound when a tool or piece of armour breaks. From UniTweaks (More Sounds).
 *
 * <p>Uses {@code random.break}, which ships with the game's resources, rather than the custom sound
 * UniTweaks bundled - RetroTweaks adds no assets, so a sound it cannot supply would simply be silent.
 * The "falling pipe" joke variant needed such an asset; its config toggle did nothing and has been
 * removed rather than left dead - see PORT_STATUS.md.
 */
@Mixin(ItemStack.class)
public class MoreSoundsMixin {

	@Inject(method = "damage", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/entity/player/PlayerEntity;increaseStat(Lnet/minecraft/stat/Stat;I)V", shift = At.Shift.AFTER))
	private void retrotweaks$toolBreakSound(int amount, Entity holder, CallbackInfo ci) {
		if (!Config.SOUNDS.moreSounds || !(holder instanceof PlayerEntity player)) return;
		float pitch = (player.world.random.nextFloat() - player.world.random.nextFloat()) * 0.2F + 1.0F;
		player.world.playSound(player, "random.break", 1.0F, pitch);
	}
}
