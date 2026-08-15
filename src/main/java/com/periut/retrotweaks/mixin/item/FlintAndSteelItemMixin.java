package com.periut.retrotweaks.mixin.item;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Flint and steel only wears out when it actually lights something. From AnnoyanceFix and UniTweaks.
 *
 * <p>Vanilla charges a durability point for every right click, including the ones that hit stone and
 * do nothing. Whether a fire appeared is decided by comparing the block before and after.
 */
@Mixin(FlintAndSteelItem.class)
public abstract class FlintAndSteelItemMixin extends Item {

	protected FlintAndSteelItemMixin(int id) {
		super(id);
	}

	/**
	 * Right-clicking a mob with flint and steel sets it on fire. From UniTweaks. An override
	 * rather than an inject: FlintAndSteelItem inherits this method rather than declaring it.
	 */
	@Override
	public void useOnEntity(ItemStack stack, LivingEntity entity) {
		if (Config.MOBS.betterBurning.igniteEntitiesWithFlintAndSteel) {
			// 100 ticks is five seconds, the same burn vanilla gives a mob standing in fire.
			entity.fireTicks += 100;
			stack.damage(1, null);
		}
		super.useOnEntity(stack, entity);
	}

	@Unique
	private boolean retrotweaks$wasAlreadyLit;

	@Inject(method = "useOnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockId(III)I"))
	private void retrotweaks$noteFireBefore(ItemStack stack, PlayerEntity player, World world,
			int x, int y, int z, int side, CallbackInfoReturnable<Boolean> cir) {
		if (!Config.BLOCKS.flintAndSteelFixes) return;
		retrotweaks$wasAlreadyLit = world.getBlockId(x, y, z) == Block.FIRE.id;
	}

	@WrapOperation(method = "useOnBlock", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/item/ItemStack;damage(ILnet/minecraft/entity/Entity;)V"))
	private void retrotweaks$onlyDamageOnIgnite(ItemStack stack, int amount, Entity holder, Operation<Void> original) {
		if (!Config.BLOCKS.flintAndSteelFixes) {
			original.call(stack, amount, holder);
			return;
		}
		// Nothing was lit if fire was already there; the click did no work, so it costs nothing.
		if (!retrotweaks$wasAlreadyLit) original.call(stack, amount, holder);
	}
}
