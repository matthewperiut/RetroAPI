package com.periut.retroapi.mixin.network;

import com.periut.retroapi.register.block.event.BlockUseDispatch;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dedicated-server half of {@link com.periut.retroapi.register.block.event.BlockUseCallback}: the
 * authoritative right-click path in multiplayer. The singleplayer half lives on the client's own
 * InteractionManager (b1.7.3 has no integrated server, so the two never both run).
 *
 * <p>Separate from {@code ServerPlayerInteractionManagerMixin} on purpose - see
 * {@code BlockUseClientMixin} for why sharing that class would have disabled the event under StationAPI.
 */
@Mixin(ServerPlayerInteractionManager.class)
public class BlockUseServerMixin {

	@Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
	private void retroapi$blockUse(PlayerEntity player, World world, ItemStack held, int x, int y, int z,
			int face, CallbackInfoReturnable<Boolean> cir) {
		BlockUseDispatch.fire(player, world, held, x, y, z, face, cir);
	}
}
