package com.periut.retroapi.mixin.client;

import net.minecraft.client.render.block.BlockRenderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the renderer's texture override (the block-breaking crack sprite, set by the
 * world renderer while a block is being mined; -1 otherwise) so custom render types can
 * draw the cracking animation over their own geometry like vanilla shapes do.
 */
@Mixin(BlockRenderManager.class)
public interface BlockRenderManagerAccessor {

	@Accessor("textureOverride")
	int retroapi$getTextureOverride();
}
