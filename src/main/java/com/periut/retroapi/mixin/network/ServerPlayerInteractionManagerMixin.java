package com.periut.retroapi.mixin.network;

import com.periut.retroapi.registry.RetroIds;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {
	// Server-side half of the worldEvent(2001) block-break packing widening (see RetroIds /
	// ClientPlayerInteractionManagerMixin). Matches StationAPI's flattening layout: meta * (1<<28).
	@ModifyConstant(method = "tryBreakBlock", constant = @Constant(intValue = 256))
	private int retroapi$widenBlockIdPacking(int original) {
		return RetroIds.BREAK_EVENT_META_MULTIPLIER;
	}
}
