package com.periut.retroapi.mixin.client;

import com.periut.retroapi.client.screen.StationAPIWorldScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.storage.WorldSaveInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

@Mixin(SelectWorldScreen.class)
public abstract class MinecraftMixin extends Screen {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI");

	@Shadow
	private List<WorldSaveInfo> saves;

	@Shadow
	protected abstract String getSaveFileName(int index);

	@Inject(method = "getSaves", at = @At("TAIL"))
	private void retroapi$markStationAPIWorlds(CallbackInfo ci) {
		if (FabricLoader.getInstance().isModLoaded("stationapi")) return;

		File gameDir = ((MinecraftAccessor) this.minecraft).retroapi$getGameDir();
		File savesDir = new File(gameDir, "saves");

		for (int i = 0; i < this.saves.size(); i++) {
			WorldSaveInfo info = this.saves.get(i);
			File levelDat = new File(new File(savesDir, info.getSaveName()), "level.dat");

			if (levelDat.exists()) {
				try (FileInputStream fis = new FileInputStream(levelDat)) {
					NbtCompound root = NbtIo.readCompressed(fis);
					NbtCompound data = root.getCompound("Data");
					if (data.contains("stationapi:data_versions") && data.getCompound("stationapi:data_versions").contains("stationapi")) {
						String name = info.getName();
						if (name == null || name.trim().isEmpty()) {
							name = "World " + (i + 1);
						}
						this.saves.set(i, new WorldSaveInfo(
							info.getSaveName(),
							"\u00a7c[StationAPI] \u00a7r" + name,
							info.getLastPlayed(),
							info.getSize(),
							true
						));
					}
				} catch (Exception e) {
					LOGGER.error("Failed to read level.dat for StationAPI detection", e);
				}
			}
		}
	}

	@Inject(method = "selectWorld", at = @At("HEAD"), cancellable = true)
	private void retroapi$checkStationAPIWorld(int worldId, CallbackInfo ci) {
		if (FabricLoader.getInstance().isModLoaded("stationapi")) return;

		String worldFolder = this.getSaveFileName(worldId);
		if (worldFolder == null) worldFolder = "World" + worldId;

		File gameDir = ((MinecraftAccessor) this.minecraft).retroapi$getGameDir();
		File worldDir = new File(new File(gameDir, "saves"), worldFolder);
		File levelDat = new File(worldDir, "level.dat");

		if (levelDat.exists()) {
			try (FileInputStream fis = new FileInputStream(levelDat)) {
				NbtCompound root = NbtIo.readCompressed(fis);
				NbtCompound data = root.getCompound("Data");
				if (data.contains("stationapi:data_versions") && data.getCompound("stationapi:data_versions").contains("stationapi")) {
					LOGGER.warn("StationAPI world detected without StationAPI: {}", worldDir);
					this.minecraft.openScreen(new StationAPIWorldScreen());
					ci.cancel();
				}
			} catch (Exception e) {
				LOGGER.error("Failed to read level.dat for StationAPI detection", e);
			}
		}
	}
}
