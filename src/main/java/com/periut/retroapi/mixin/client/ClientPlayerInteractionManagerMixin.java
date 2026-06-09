package com.periut.retroapi.mixin.client;

import com.periut.retroapi.registry.RetroIds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.InteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Environment(EnvType.CLIENT)
@Mixin(InteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
	// Widen the worldEvent(2001) block-break packing from vanilla's `blockId + meta*256` (8-bit id)
	// to `blockId + meta*(1<<28)` (28-bit id), matching StationAPI's flattening layout so the encode
	// here agrees with WorldRendererMixin's decode. See RetroIds for the full rationale.
	@ModifyConstant(method = "breakBlock", constant = @Constant(intValue = 256))
	private int retroapi$widenBlockIdPacking(int original) {
		return RetroIds.BREAK_EVENT_META_MULTIPLIER;
	}
}

