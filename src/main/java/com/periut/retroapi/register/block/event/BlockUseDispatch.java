package com.periut.retroapi.register.block.event;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The one place both interaction-manager mixins call into, so the client (singleplayer) and dedicated
 * server halves of {@link BlockUseCallback} can never drift apart in how they translate a
 * {@link BlockUseCallback.Result} into {@code interactBlock}'s boolean return.
 */
public final class BlockUseDispatch {

	private BlockUseDispatch() {}

	/** Runs the listeners and, if one claimed the click, sets {@code interactBlock}'s return value. */
	public static void fire(PlayerEntity player, World world, ItemStack held, int x, int y, int z, int face,
			CallbackInfoReturnable<Boolean> cir) {
		BlockUseCallback.Result result =
			BlockUseCallback.EVENT.invoker().onUseBlock(player, world, held, x, y, z, face);
		if (result == BlockUseCallback.Result.SUCCESS) {
			cir.setReturnValue(true);
		} else if (result == BlockUseCallback.Result.FAIL) {
			cir.setReturnValue(false);
		}
	}
}
