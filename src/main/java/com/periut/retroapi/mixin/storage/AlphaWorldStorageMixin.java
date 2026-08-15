package com.periut.retroapi.mixin.storage;

import com.periut.retroapi.registry.IdAssigner;
import com.periut.retroapi.registry.RetroRegistry;
import com.periut.retroapi.storage.SidecarManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.WorldProperties;
import net.minecraft.world.storage.AlphaWorldStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.List;

@Mixin(AlphaWorldStorage.class)
public class AlphaWorldStorageMixin {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI");

	@Inject(method = "<init>", at = @At("TAIL"))
	private void retroapi$assignIds(CallbackInfo ci) {
		if (RetroRegistry.getBlocks().isEmpty() && RetroRegistry.getItems().isEmpty()) return;

		File worldDir = ((WorldStorageAccessor) (Object) this).retroapi$getDir();

		// Both worlds get a sidecar. Under StationAPI it holds no block IDS - those are StationAPI's, in
		// its own chunk sections - but everything else a position carries still has nowhere else to live:
		// RetroBlockData, the state bits above the vanilla nibble, cubic biome cells. Leaving the sidecar
		// unopened there meant all of it was written nowhere and read back as absent, so a clad block came
		// back from a rejoin wearing nothing.
		SidecarManager.setWorldDir(worldDir);

		if (FabricLoader.getInstance().isModLoaded("stationapi")) {
			LOGGER.debug("StationAPI present, saving current ID map for world: {}", worldDir);
			IdAssigner.saveCurrentIds(worldDir);
		} else {
			LOGGER.debug("Assigning IDs for world: {}", worldDir);
			IdAssigner.assignIds(worldDir);
		}
	}

	@Inject(method = "save(Lnet/minecraft/world/WorldProperties;Ljava/util/List;)V", at = @At("RETURN"))
	private void retroapi$onSave(WorldProperties data, List players, CallbackInfo ci) {
		SidecarManager.saveAll();
	}
}

