package com.periut.retroapi.mixin.register;

import com.periut.retroapi.registry.RetroIds;
import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(Block.class)
public class BlockArrayExpandMixin {
	private static final int EXPANDED_SIZE = RetroIds.BLOCK_ID_CAPACITY;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void retroapi$expandArrays(CallbackInfo ci) {
		if (Block.BLOCKS.length < EXPANDED_SIZE) {
			Block.BLOCKS = Arrays.copyOf(Block.BLOCKS, EXPANDED_SIZE);
			Block.BLOCKS_OPAQUE = Arrays.copyOf(Block.BLOCKS_OPAQUE, EXPANDED_SIZE);
			Block.BLOCKS_LIGHT_OPACITY = Arrays.copyOf(Block.BLOCKS_LIGHT_OPACITY, EXPANDED_SIZE);
			Block.BLOCKS_ALLOW_VISION = Arrays.copyOf(Block.BLOCKS_ALLOW_VISION, EXPANDED_SIZE);
			Block.BLOCKS_WITH_ENTITY = Arrays.copyOf(Block.BLOCKS_WITH_ENTITY, EXPANDED_SIZE);
			Block.BLOCKS_RANDOM_TICK = Arrays.copyOf(Block.BLOCKS_RANDOM_TICK, EXPANDED_SIZE);
			Block.BLOCKS_LIGHT_LUMINANCE = Arrays.copyOf(Block.BLOCKS_LIGHT_LUMINANCE, EXPANDED_SIZE);
			Block.BLOCKS_IGNORE_META_UPDATE = Arrays.copyOf(Block.BLOCKS_IGNORE_META_UPDATE, EXPANDED_SIZE);
		}
	}
}

