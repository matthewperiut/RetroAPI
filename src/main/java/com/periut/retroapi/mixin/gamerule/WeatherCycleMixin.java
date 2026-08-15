package com.periut.retroapi.mixin.gamerule;

import com.periut.retroapi.gamerule.RetroGameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code doWeatherCycle}: the weather stops changing on its own and stays however it is.
 *
 * <p>Only the automatic cycle is frozen; {@code /weather} still sets it, exactly as in modern.
 */
@Mixin(World.class)
public class WeatherCycleMixin {

	@Inject(method = "updateWeatherCycles", at = @At("HEAD"), cancellable = true)
	private void retroapi$doWeatherCycle(CallbackInfo ci) {
		if (!RetroGameRules.getBoolean(RetroGameRules.DO_WEATHER_CYCLE)) {
			ci.cancel();
		}
	}
}
