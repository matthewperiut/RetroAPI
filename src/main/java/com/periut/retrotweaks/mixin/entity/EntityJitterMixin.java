package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops other players' and mobs' positions being overwritten by the client's own physics, which is
 * what makes them jitter in multiplayer. Originally by calmilamsy, via UniTweaks.
 *
 * <p>The client should simply draw where the server says an entity is. Letting client-side movement
 * write the position too means the two fight every tick.
 */
@Mixin(Entity.class)
public class EntityJitterMixin {

	@WrapOperation(method = "move", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;x:D", opcode = Opcodes.PUTFIELD))
	private void retrotweaks$keepServerX(Entity entity, double value, Operation<Void> original) {
		if (retrotweaks$serverOwnsPosition(entity)) return;
		original.call(entity, value);
	}

	@WrapOperation(method = "move", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;y:D", opcode = Opcodes.PUTFIELD))
	private void retrotweaks$keepServerY(Entity entity, double value, Operation<Void> original) {
		if (retrotweaks$serverOwnsPosition(entity)) return;
		original.call(entity, value);
	}

	@WrapOperation(method = "move", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;z:D", opcode = Opcodes.PUTFIELD))
	private void retrotweaks$keepServerZ(Entity entity, double value, Operation<Void> original) {
		if (retrotweaks$serverOwnsPosition(entity)) return;
		original.call(entity, value);
	}

	/** True for a remote living entity that is not the local player: the server is authoritative. */
	private static boolean retrotweaks$serverOwnsPosition(Entity entity) {
		if (!Config.BUGFIXES.multiplayerEntityJitterFix) return false;
		return entity.world.isRemote && entity instanceof LivingEntity && !(entity instanceof PlayerEntity);
	}
}
