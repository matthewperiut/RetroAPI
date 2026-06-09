package com.periut.retroapi.mixin.dimension;

import com.periut.retroapi.dimension.DimensionRegistration;
import com.periut.retroapi.dimension.RetroDimensionRegistry;
import net.minecraft.world.dimension.Dimension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes a modded serial id resolvable to its {@link Dimension} instance. Vanilla
 * {@code Dimension.fromId} only knows {@code -1/0/1} (Nether/Overworld/Skylands) and returns
 * {@code null} for anything else. We intercept at HEAD for registered modded ids only; vanilla ids
 * fall through to vanilla. Single hook that makes any modded id instantiable - mirrors StationAPI's
 * {@code DimensionMixin}. Disabled when StationAPI is present (its DimensionMixin owns this).
 */
@Mixin(Dimension.class)
public class DimensionMixin {
	@Inject(method = "fromId", at = @At("HEAD"), cancellable = true)
	private static void retroapi$fromId(int id, CallbackInfoReturnable<Dimension> cir) {
		if (RetroDimensionRegistry.isVanillaId(id)) return;
		DimensionRegistration reg = RetroDimensionRegistry.getBySerialId(id);
		if (reg != null) {
			cir.setReturnValue(reg.create());
		}
	}
}
