package com.periut.retroapi.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
#if MC_VER >= 160
	@ModifyConstant(method = "finishMiningBlock", constant = @Constant(intValue = 256))
#else
	// b1.4-b1.5 may not have the 256 constant in finishMiningBlock
	@ModifyConstant(method = "finishMiningBlock", constant = @Constant(intValue = 256), require = 0)
#endif
	private int retroapi$widenBlockIdPacking(int original) {
		return 4096;
	}
}
