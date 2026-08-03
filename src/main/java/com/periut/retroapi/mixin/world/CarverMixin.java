package com.periut.retroapi.mixin.world;

import com.periut.retroapi.world.carver.RetroCarvers;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSource;
import net.minecraft.world.gen.carver.Carver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The single seam RetroAPI's carver registry hangs off.
 *
 * <p>{@code Carver.carve(ChunkSource, World, int, int, byte[])} is the one public entry every beta
 * generator calls, exactly once per generated chunk, with the raw block array in hand - so hooking it
 * covers vanilla's three generators without RetroAPI knowing anything about any of them. Registered
 * carvers run at RETURN (after vanilla's caves) when vanilla carving is enabled, or in place of it at
 * HEAD when a mod has vetoed vanilla carving for the dimension.
 *
 * <p>{@code require = 0} on both: a custom generator may never call this at all, and that is a supported
 * configuration rather than a mixin failure.
 */
@Mixin(Carver.class)
public class CarverMixin {

	@Inject(
		method = "carve(Lnet/minecraft/world/chunk/ChunkSource;Lnet/minecraft/world/World;II[B)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void retroapi$carveHead(ChunkSource source, World world, int chunkX, int chunkZ, byte[] blocks,
			CallbackInfo ci) {
		if (world == null || RetroCarvers.isVanillaCarvingEnabled(world.dimension.id)) {
			return;
		}
		// Vanilla carving is vetoed for this dimension: run the registered carvers in its place and
		// skip beta's own cave pass entirely.
		RetroCarvers.carve(world, chunkX, chunkZ, blocks);
		ci.cancel();
	}

	@Inject(
		method = "carve(Lnet/minecraft/world/chunk/ChunkSource;Lnet/minecraft/world/World;II[B)V",
		at = @At("RETURN"),
		require = 0
	)
	private void retroapi$carveTail(ChunkSource source, World world, int chunkX, int chunkZ, byte[] blocks,
			CallbackInfo ci) {
		if (world == null || !RetroCarvers.isVanillaCarvingEnabled(world.dimension.id)) {
			return;
		}
		RetroCarvers.carve(world, chunkX, chunkZ, blocks);
	}
}
