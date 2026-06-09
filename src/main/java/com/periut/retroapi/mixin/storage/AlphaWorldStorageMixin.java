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

		if (FabricLoader.getInstance().isModLoaded("stationapi")) {
			LOGGER.info("StationAPI present, saving current ID map for world: {}", worldDir);
			IdAssigner.saveCurrentIds(worldDir);
		} else {
			SidecarManager.setWorldDir(worldDir);
			LOGGER.info("Assigning IDs for world: {}", worldDir);
			IdAssigner.assignIds(worldDir);
		}
	}

	@Inject(method = "save(Lnet/minecraft/world/WorldProperties;Ljava/util/List;)V", at = @At("RETURN"))
	private void retroapi$onSave(WorldProperties data, List players, CallbackInfo ci) {
		SidecarManager.saveAll();
	}
}

