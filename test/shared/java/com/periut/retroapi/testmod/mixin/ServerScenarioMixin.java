package com.periut.retroapi.testmod.mixin;

import com.periut.retroapi.storage.SidecarManager;
import com.periut.retroapi.testmod.conv.ConvResult;
import com.periut.retroapi.testmod.conv.Scenario;
import com.periut.retroapi.testmod.conv.ServerVerifier;
import com.periut.retroapi.testmod.conv.WorldPopulator;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

/**
 * Drives the dedicated-server side of the conversion round-trip test. Runs the selected scenario once,
 * a few ticks after startup (so the spawn region is fully loaded), then stops the server so the run
 * task exits.
 *
 * <ul>
 *   <li>{@code populate} - fill a fresh world with modded content + write the manifest;</li>
 *   <li>{@code verify} - load the reverse-converted world and confirm the content is runtime-valid in
 *       plain RetroAPI (no StationAPI), the proof that an un-flattened world is genuinely usable.</li>
 * </ul>
 */
@Mixin(MinecraftServer.class)
public abstract class ServerScenarioMixin {
	@Unique private boolean retroapi_test$ran = false;
	@Unique private int retroapi_test$settleTicks = 0;

	@Inject(method = "tick", at = @At("TAIL"))
	private void retroapi_test$runScenario(CallbackInfo ci) {
		boolean populate = Scenario.is(Scenario.POPULATE);
		boolean verify = Scenario.is(Scenario.VERIFY);
		if (!populate && !verify) return;
		if (retroapi_test$ran) return;
		// Let the world finish loading + a couple of ticks of entity settling before acting.
		if (++retroapi_test$settleTicks < 10) return;
		retroapi_test$ran = true;

		MinecraftServer server = (MinecraftServer) (Object) this;
		File worldDir = SidecarManager.getWorldDir();
		if (worldDir == null) worldDir = Scenario.worldDir();

		boolean ok;
		try {
			ok = populate ? WorldPopulator.run(server) : ServerVerifier.run(server);
		} catch (Throwable t) {
			ok = false;
			com.periut.retroapi.testmod.TestMod.LOGGER.error("[conv] scenario '{}' threw", Scenario.name(), t);
		}
		ConvResult.write(worldDir, populate ? "populate" : "verify(mcregion)", ok, null);

		((ServerControlAccessor) server).retroapi_test$setRunning(false);
	}
}
