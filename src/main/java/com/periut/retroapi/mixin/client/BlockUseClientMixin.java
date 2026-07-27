package com.periut.retroapi.mixin.client;

import com.periut.retroapi.register.block.event.BlockUseDispatch;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.InteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Singleplayer half of {@link com.periut.retroapi.register.block.event.BlockUseCallback}.
 *
 * <p>This base class IS the singleplayer path: SingleplayerInteractionManager does not override
 * interactBlock, while MultiplayerInteractionManager does - which is why the hook goes here and not on
 * the subclass. In multiplayer the dedicated server decides and its own copy of this hook fires there, so
 * a listener runs once per click either way.
 *
 * <p>Deliberately its own mixin rather than a method on {@code ClientPlayerInteractionManagerMixin},
 * which targets the same class: that one is disabled under StationAPI (its {@code @ModifyConstant} would
 * collide with StationAPI's identical break-packing widening) and this {@code @Inject} composes fine, so
 * sharing a class would have silently killed the event for every StationAPI user.
 */
@Environment(EnvType.CLIENT)
@Mixin(InteractionManager.class)
public class BlockUseClientMixin {

	@Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
	private void retroapi$blockUse(PlayerEntity player, World world, ItemStack held, int x, int y, int z,
			int face, CallbackInfoReturnable<Boolean> cir) {
		BlockUseDispatch.fire(player, world, held, x, y, z, face, cir);
	}
}
