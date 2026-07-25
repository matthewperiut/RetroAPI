package com.periut.retroapi.mixin.world;

import com.periut.retroapi.world.feature.RetroFeatures;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkCache;
import net.minecraft.world.chunk.ChunkSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs registered {@link RetroFeatures} after vanilla decorates a chunk, on the singleplayer/client-hosted
 * chunk cache.
 *
 * <p>Hooking the CACHE rather than each generator is deliberate: every generator - vanilla's three and any
 * custom {@code ChunkSource} a modded dimension supplies - is decorated through here, so a mod's ore
 * generates in modded dimensions too without RetroAPI knowing anything about them.
 */
@Mixin(ChunkCache.class)
public class ChunkCacheDecorateMixin {

	@Shadow public World world;

	@Inject(method = "decorate", at = @At("RETURN"), require = 0)
	private void retroapi$decorate(ChunkSource source, int chunkX, int chunkZ, CallbackInfo ci) {
		RetroFeatures.decorate(this.world, chunkX, chunkZ);
	}
}
