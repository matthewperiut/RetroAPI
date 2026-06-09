package com.periut.retroapi.mixin.world;

import com.periut.retroapi.world.event.BlockSetCallback;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fires {@link BlockSetCallback} for both world block-set funnels. {@code World.setBlock}
 * delegates to these, so hooking the two {@code setBlockWithoutNotifyingNeighbors} overloads
 * covers every code path exactly once.
 */
@Mixin(World.class)
public abstract class WorldSetBlockMixin {

	@Inject(method = "setBlockWithoutNotifyingNeighbors(IIII)Z", at = @At("HEAD"), cancellable = true)
	private void retroapi$onSetBlock(int x, int y, int z, int blockId, CallbackInfoReturnable<Boolean> cir) {
		if (BlockSetCallback.EVENT.invoker().onBlockSet((World) (Object) this, x, y, z, blockId, 0)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "setBlockWithoutNotifyingNeighbors(IIIII)Z", at = @At("HEAD"), cancellable = true)
	private void retroapi$onSetBlockMeta(int x, int y, int z, int blockId, int meta, CallbackInfoReturnable<Boolean> cir) {
		if (BlockSetCallback.EVENT.invoker().onBlockSet((World) (Object) this, x, y, z, blockId, meta)) {
			cir.setReturnValue(false);
		}
	}
}
