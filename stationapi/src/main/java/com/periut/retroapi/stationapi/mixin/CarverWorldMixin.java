package com.periut.retroapi.stationapi.mixin;

import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSource;
import net.minecraft.world.gen.carver.Carver;
import net.modificationstation.stationapi.impl.world.CaveGenBaseImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Binds StationAPI's per-carver world on EVERY cave/structure carver right before it carves, so a custom
 * chunk generator written against RetroAPI (e.g. the Aether's {@code ChunkProviderAether}) works under
 * StationAPI's flattening.
 *
 * <p>StationAPI's flattening makes each {@link Carver} hold a {@code stationapi_world} (its
 * {@code CaveGenBaseImpl} mixin), read while carving to clamp cave Y to the flattened world height. But
 * StationAPI only SETS it inside the vanilla chunk generators' constructors
 * ({@code OverworldChunkGeneratorMixin} etc.) - a custom {@code ChunkSource} StationAPI has no mixin for
 * never gets it, so its carvers' {@code carveTunnels} reads a null world and throws
 * ("use CaveGenBaseImpl.stationapi_setWorld in your custom ChunkSource constructor to fix"). Rather than
 * make every RetroAPI consumer learn StationAPI's API, set it generically at the public carve entry -
 * which receives the world and runs before any {@code carveTunnels}.
 *
 * <p>StationAPI-only (references {@code CaveGenBaseImpl}); gated by {@code RetroAPIMixinPlugin}'s
 * {@code STATIONAPI_ONLY_MIXINS}.
 */
@Mixin(Carver.class)
public class CarverWorldMixin {
	@Inject(
		method = "carve(Lnet/minecraft/world/chunk/ChunkSource;Lnet/minecraft/world/World;II[B)V",
		at = @At("HEAD")
	)
	private void retroapi$bindStationWorld(ChunkSource chunkSource, World world, int chunkX, int chunkZ, byte[] blocks, CallbackInfo ci) {
		((CaveGenBaseImpl) (Object) this).stationapi_setWorld(world);
	}
}
