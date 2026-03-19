package com.periut.retroapi.mixin.register;

import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(Block.class)
public class BlockArrayExpandMixin {
	private static final int EXPANDED_SIZE = 4096;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void retroapi$expandArrays(CallbackInfo ci) {
		if (Block.BY_ID.length < EXPANDED_SIZE) {
			Block.BY_ID = Arrays.copyOf(Block.BY_ID, EXPANDED_SIZE);
#if MC_VER >= 160
			Block.IS_SOLID_RENDER = Arrays.copyOf(Block.IS_SOLID_RENDER, EXPANDED_SIZE);
#else
			Block.IS_SOLID = Arrays.copyOf(Block.IS_SOLID, EXPANDED_SIZE);
#endif
			Block.OPACITIES = Arrays.copyOf(Block.OPACITIES, EXPANDED_SIZE);
			Block.IS_TRANSLUCENT = Arrays.copyOf(Block.IS_TRANSLUCENT, EXPANDED_SIZE);
			Block.HAS_BLOCK_ENTITY = Arrays.copyOf(Block.HAS_BLOCK_ENTITY, EXPANDED_SIZE);
			Block.TICKS_RANDOMLY = Arrays.copyOf(Block.TICKS_RANDOMLY, EXPANDED_SIZE);
			Block.LIGHT = Arrays.copyOf(Block.LIGHT, EXPANDED_SIZE);
#if MC_VER >= 160
			Block.UPDATE_CLIENTS = Arrays.copyOf(Block.UPDATE_CLIENTS, EXPANDED_SIZE);
#endif
		}
	}
}
