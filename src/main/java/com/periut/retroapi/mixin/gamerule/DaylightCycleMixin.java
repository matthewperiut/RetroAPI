package com.periut.retroapi.mixin.gamerule;

import com.periut.retroapi.gamerule.RetroGameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * {@code doDaylightCycle}: the sun stops moving.
 *
 * <p>{@code World.tick} writes the time twice - once when everyone sleeps through to morning, once
 * for the ordinary one-tick advance - and this redirect covers both, so with the rule off neither a
 * night's sleep nor the passage of time moves the clock. {@code /time set} writes it elsewhere and
 * still works, which is the behaviour modern has.
 */
@Mixin(World.class)
public class DaylightCycleMixin {

	@Redirect(
		method = "tick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldProperties;setTime(J)V")
	)
	private void retroapi$doDaylightCycle(WorldProperties properties, long time) {
		if (RetroGameRules.getBoolean(RetroGameRules.DO_DAYLIGHT_CYCLE)) {
			properties.setTime(time);
		}
	}
}
