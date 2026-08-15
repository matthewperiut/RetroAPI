package com.periut.retrotweaks.mixin.world;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.World;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Makes the autosave interval configurable. From UniTweaks.
 *
 * <p>Vanilla saves every 40 seconds (800 ticks) whatever the world size, which on a large world is
 * a visible stutter. The option is in seconds; the field is in ticks.
 */
@Mixin(World.class)
public class WorldAutosaveMixin {

	@Shadow protected int saveInterval;

	@WrapOperation(method = "<init>(Lnet/minecraft/world/storage/WorldStorage;Ljava/lang/String;JLnet/minecraft/world/dimension/Dimension;)V",
		at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/world/World;saveInterval:I"))
	private void retrotweaks$setSaveInterval(World world, int vanilla, Operation<Void> original) {
		original.call(world, Config.SYSTEM.autosaveInterval * 20);
	}
}
