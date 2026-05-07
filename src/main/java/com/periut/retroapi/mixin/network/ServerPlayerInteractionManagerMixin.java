package com.periut.retroapi.mixin.network;

import net.minecraft.server.ServerPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {
	@ModifyConstant(method = "tryMineBlock", constant = @Constant(intValue = 256))
	private int retroapi$widenBlockIdPacking(int original) {
		return 4096;
	}
}
