package com.periut.retroapi.mixin.dimension.server;

import com.periut.retroapi.dimension.DimensionRegistration;
import com.periut.retroapi.dimension.RetroDimensionRegistry;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.ChunkMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-dimension chunk maps for modded dimensions. Vanilla's {@code chunkMaps} is a length-2 array
 * ({@code [0]}=overworld {@code [1]}=nether) resolved by a ternary, mirroring the worlds array - so
 * we use the same side-map trick (see
 * {@link com.periut.retroapi.mixin.dimension.server.MinecraftServerMixin}) rather than resizing it:
 * create a {@link ChunkMap} per modded dimension, return it from {@code getChunkMap}, and remove the
 * player from all modded maps on a dimension change (vanilla only clears the two it knows). Disabled
 * under StationAPI.
 */
@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
	@Unique private final Map<Integer, ChunkMap> retroapi$moddedChunkMaps = new HashMap<>();

	@Inject(method = "<init>", at = @At("TAIL"))
	private void retroapi$createModdedChunkMaps(MinecraftServer server, CallbackInfo ci) {
		int viewDistance = server.properties.getProperty("view-distance", 10);
		for (DimensionRegistration reg : RetroDimensionRegistry.getAll()) {
			retroapi$moddedChunkMaps.put(reg.getSerialId(), new ChunkMap(server, reg.getSerialId(), viewDistance));
		}
	}

	@Inject(method = "getChunkMap", at = @At("HEAD"), cancellable = true)
	private void retroapi$getModdedChunkMap(int dimensionId, CallbackInfoReturnable<ChunkMap> cir) {
		ChunkMap chunkMap = retroapi$moddedChunkMaps.get(dimensionId);
		if (chunkMap != null) {
			cir.setReturnValue(chunkMap);
		}
	}

	@Inject(method = "updatePlayerAfterDimensionChange", at = @At("HEAD"))
	private void retroapi$removeFromModdedChunkMaps(ServerPlayerEntity player, CallbackInfo ci) {
		for (ChunkMap chunkMap : retroapi$moddedChunkMaps.values()) {
			chunkMap.removePlayer(player);
		}
	}

	/**
	 * Flush the modded chunk maps every tick. Vanilla's {@code updateAllChunks} only iterates the
	 * length-2 {@code chunkMaps} array, so the side-mapped modded maps never had
	 * {@link ChunkMap#updateChunks} called - queued block-change broadcasts in a modded dimension
	 * were never sent, so with 2+ players one player's block changes never reached the others. Same
	 * side-map-must-be-driven gap as the world/tracker tick in
	 * {@link com.periut.retroapi.mixin.dimension.server.MinecraftServerMixin}.
	 */
	@Inject(method = "updateAllChunks", at = @At("TAIL"))
	private void retroapi$updateModdedChunks(CallbackInfo ci) {
		for (ChunkMap chunkMap : retroapi$moddedChunkMaps.values()) {
			chunkMap.updateChunks();
		}
	}
}
