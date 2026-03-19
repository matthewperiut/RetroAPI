package com.periut.retroapi.mixin.storage;

import com.periut.retroapi.registry.IdAssigner;
import com.periut.retroapi.registry.RetroRegistry;
import com.periut.retroapi.storage.SidecarManager;
#if MC_B1_6_OR_LATER
import net.fabricmc.loader.api.FabricLoader;
#endif
import net.minecraft.world.WorldData;
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

#if MC_B1_6_OR_LATER
		if (FabricLoader.getInstance().isModLoaded("stationapi")) {
			LOGGER.info("StationAPI present, saving current ID map for world: {}", worldDir);
			IdAssigner.saveCurrentIds(worldDir);
		} else {
			SidecarManager.setWorldDir(worldDir);
			LOGGER.info("Assigning IDs for world: {}", worldDir);
			IdAssigner.assignIds(worldDir);
		}
#else
		SidecarManager.setWorldDir(worldDir);
		LOGGER.info("Assigning IDs for world: {}", worldDir);
		IdAssigner.assignIds(worldDir);
#endif
	}

	@Inject(method = "saveData(Lnet/minecraft/world/WorldData;Ljava/util/List;)V", at = @At("RETURN"))
	private void retroapi$onSave(WorldData data, List players, CallbackInfo ci) {
		SidecarManager.saveAll();
	}
}
