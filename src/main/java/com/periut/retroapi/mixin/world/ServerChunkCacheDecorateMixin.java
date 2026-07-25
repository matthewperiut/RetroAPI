package com.periut.retroapi.mixin.world;

import com.periut.retroapi.world.feature.RetroFeatures;
import net.minecraft.server.world.chunk.ServerChunkCache;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.chunk.ChunkSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The dedicated-server half of {@link ChunkCacheDecorateMixin}. */
@Mixin(ServerChunkCache.class)
public class ServerChunkCacheDecorateMixin {

	@Shadow public ServerWorld world;

	@Inject(method = "decorate", at = @At("RETURN"), require = 0)
	private void retroapi$decorate(ChunkSource source, int chunkX, int chunkZ, CallbackInfo ci) {
		RetroFeatures.decorate(this.world, chunkX, chunkZ);
	}
}
