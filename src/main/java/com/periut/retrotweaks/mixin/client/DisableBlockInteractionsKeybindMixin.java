package com.periut.retrotweaks.mixin.client;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.feature.options.ModKeyBindings;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.InteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The client half of "Disable Block Interactions With Keybind": while the dedicated key (default
 * Left Shift, independent of the sneak key) is held, block interaction becomes placement instead.
 * From GameplayEssentials.
 *
 * <p>Same trick as {@link InteractionManagerMixin}'s shift-placing (a separate, sneak-based feature
 * from UniTweaks) - reporting the block as air makes vanilla treat the click as a placement rather
 * than a use.
 */
@Mixin(InteractionManager.class)
public class DisableBlockInteractionsKeybindMixin {

	@Shadow @Final protected Minecraft minecraft;

	@WrapOperation(method = "interactBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockId(III)I"))
	private int retrotweaks$disableBlockInteractionsKeybind(World world, int x, int y, int z, Operation<Integer> original) {
		if (Config.GAMEPLAY.disableBlockInteractionsKeybind
			&& Keyboard.isKeyDown(ModKeyBindings.DISABLE_BLOCK_INTERACTIONS.code)
			&& !(this.minecraft.player.getHand() == null)
		) {
			return 0;
		}

		return original.call(world, x, y, z);
	}
}
