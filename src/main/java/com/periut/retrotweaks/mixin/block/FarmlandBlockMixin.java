package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/** Farmland trampling rules. From MiscTweaks. */
@Mixin(FarmlandBlock.class)
public class FarmlandBlockMixin {

	@Inject(method = "onSteppedOn", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$bootRules(World world, int x, int y, int z, Entity entity, CallbackInfo ci) {
		Config.FarmlandTrampling trampling = Config.MOBS.trampling;
		if (!(entity instanceof PlayerEntity player)) return;

		ItemStack boots = player.inventory.armor[0];
		if (trampling.noTramplingBarefoot && boots == null) {
			ci.cancel();
			return;
		}
		if (trampling.noTramplingLeatherBoots && boots != null && boots.itemId == Item.LEATHER_BOOTS.id) {
			ci.cancel();
		}
	}

	@WrapOperation(method = "onSteppedOn", at = @At(value = "INVOKE", target = "Ljava/util/Random;nextInt(I)I"))
	private int retrotweaks$neverTrample(Random random, int bound, Operation<Integer> original) {
		// Vanilla tramples when the roll comes up 0; a value it can never equal disables it.
		if (Config.MOBS.trampling.disableTrampling) return -1;
		return original.call(random, bound);
	}
}
