package com.periut.retroapi.mixin.register;

import com.periut.retroapi.register.blockentity.event.BlockEntityLoadedCallback;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link BlockEntityLoadedCallback} on a block entity's first tick - the "everything is read and
 * the world exists" moment beta has no hook for. See the callback for why {@code readNbt} is too early.
 */
@Mixin(BlockEntity.class)
public abstract class BlockEntityLoadMixin {

	@Shadow public World world;

	@Unique private boolean retroapi$loadFired;

	@Inject(method = "tick", at = @At("HEAD"), require = 0)
	private void retroapi$fireLoaded(CallbackInfo ci) {
		if (this.retroapi$loadFired || this.world == null) {
			return;
		}
		this.retroapi$loadFired = true;
		BlockEntityLoadedCallback.EVENT.invoker().onBlockEntityLoaded((BlockEntity) (Object) this);
	}
}
