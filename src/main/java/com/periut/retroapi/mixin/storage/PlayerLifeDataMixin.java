package com.periut.retroapi.mixin.storage;

import com.periut.retroapi.storage.RetroData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ends a player's "life" for {@link RetroData#life}.
 *
 * <p>Hung on the death itself rather than on the respawn because {@code PlayerEntity.respawn()} is a
 * CLIENT-only method in b1.7.3 - it does not exist on a dedicated server, where most of this data
 * lives. {@code LivingEntity.onKilledBy} exists on both sides and is the one place every death goes
 * through, so one mixin covers singleplayer and servers alike.
 */
@Mixin(LivingEntity.class)
public class PlayerLifeDataMixin {

	@Inject(method = "onKilledBy", at = @At("HEAD"))
	private void retroapi$clearLifeData(Entity killer, CallbackInfo ci) {
		if ((Object) this instanceof PlayerEntity player) {
			RetroData.clearLife(player.name);
		}
	}
}
