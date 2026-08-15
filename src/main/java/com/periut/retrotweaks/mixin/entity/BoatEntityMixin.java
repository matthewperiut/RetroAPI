package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.config.Enums;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.Item;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Boat behaviour: what a broken boat drops, what happens when one hits a wall, boat elevators, and
 * the dismount fix. From AnnoyanceFix, BetaTweaks and UniTweaks.
 *
 * <p>Vanilla scatters three planks and two sticks when a boat breaks, which is a loss every time
 * you park badly. "Boats drop themselves" swaps that for the boat item.
 *
 * <p>The collision option has three settings, checked by ordinal so adding a mode later does not
 * mean rewriting the checks: Vanilla (break into planks), Drop Boat (break, but drop the boat) and
 * Invincible (do not break at all).
 *
 * <p>This whole class is skipped by {@code RetroTweaksMixinPlugin} when UniTweaks is installed - four
 * of its own mixins patch {@code BoatEntity} (elevators, dismount, drops-itself, doesn't-break), and
 * the {@code @ModifyConstant}s below used to carry {@code require = 0} only to survive that overlap
 * silently. Not loading this class at all is what actually fixes it; the {@code require = 0}s were
 * removed once that stand-down made them unnecessary, so a genuine future collision is loud again.
 */
@Mixin(BoatEntity.class)
public abstract class BoatEntityMixin extends Entity {

	protected BoatEntityMixin(net.minecraft.world.World world) {
		super(world);
	}

	// ------------------------------------------------------------ broken by a player

	@ModifyConstant(method = "damage", constant = @Constant(intValue = 3))
	private int retrotweaks$noPlanksFromDamage(int vanilla) {
		return Config.MOBS.boatDropFixes ? 0 : vanilla;
	}

	@ModifyConstant(method = "damage", constant = @Constant(intValue = 2))
	private int retrotweaks$noSticksFromDamage(int vanilla) {
		return Config.MOBS.boatDropFixes ? 0 : vanilla;
	}

	@Inject(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/vehicle/BoatEntity;markDead()V"))
	private void retrotweaks$dropBoatOnDamage(Entity attacker, int damage, CallbackInfoReturnable<Boolean> cir) {
		if (Config.MOBS.boatDropFixes) dropItem(Item.BOAT.id, 1, 0);
	}

	// ------------------------------------------------------------ broken by a wall

	@ModifyConstant(method = "tick", constant = @Constant(intValue = 3))
	private int retrotweaks$noPlanksFromCrash(int vanilla) {
		return retrotweaks$crashDropsBoat() ? 0 : vanilla;
	}

	@WrapOperation(method = "tick", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/entity/vehicle/BoatEntity;dropItem(IIF)Lnet/minecraft/entity/ItemEntity;", ordinal = 1))
	private ItemEntity retrotweaks$noSticksFromCrash(BoatEntity boat, int id, int count, float spread, Operation<ItemEntity> original) {
		return retrotweaks$crashDropsBoat() ? null : original.call(boat, id, count, spread);
	}

	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/vehicle/BoatEntity;markDead()V"))
	private void retrotweaks$dropBoatOnCrash(CallbackInfo ci) {
		if (retrotweaks$crashDropsBoat()) dropItem(Item.BOAT.id, 1, 0);
	}

	@WrapOperation(method = "tick", at = @At(value = "FIELD",
		target = "Lnet/minecraft/entity/vehicle/BoatEntity;horizontalCollision:Z", opcode = Opcodes.GETFIELD))
	private boolean retrotweaks$invincibleBoat(BoatEntity boat, Operation<Boolean> original) {
		if (Config.MOBS.boatCollisionBehavior == Enums.BoatCollision.INVINCIBLE) return false;
		return original.call(boat);
	}

	private static boolean retrotweaks$crashDropsBoat() {
		return Config.MOBS.boatCollisionBehavior != Enums.BoatCollision.VANILLA;
	}

	// ------------------------------------------------------------ misc

	@Inject(method = "interact", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/entity/player/PlayerEntity;setVehicle(Lnet/minecraft/entity/Entity;)V", shift = At.Shift.AFTER))
	private void retrotweaks$dismountFix(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
		if (!Config.BUGFIXES.boatDismountFix) return;
		// A null vehicle here means the interact was a dismount. Nudging the player up by a
		// hundredth of a block keeps rounding from dropping them back through the hull.
		if (player.vehicle == null) player.setPosition(player.x, player.y + 0.01, player.z);
	}

	@ModifyConstant(method = "tick", constant = @Constant(doubleValue = 1.0, ordinal = 1))
	private double retrotweaks$elevatorBoats(double vanilla) {
		return Config.GAMEPLAY.oldFeatures.elevatorBoats ? Double.MAX_VALUE : vanilla;
	}
}
