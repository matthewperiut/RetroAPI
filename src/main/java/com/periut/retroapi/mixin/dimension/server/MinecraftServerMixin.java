package com.periut.retroapi.mixin.dimension.server;

import com.periut.retroapi.dimension.DimensionRegistration;
import com.periut.retroapi.dimension.RetroDimensionRegistry;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.entity.EntityTracker;
import net.minecraft.server.world.ReadOnlyServerWorld;
import net.minecraft.server.world.ServerWorldEventListener;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.storage.WorldStorage;
import net.minecraft.world.storage.WorldStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Server-side modded-dimension worlds + entity trackers. Vanilla hardcodes length-2 arrays
 * ({@code worlds[0]}=overworld, {@code worlds[1]}=nether) and resolves {@code getWorld}/
 * {@code getEntityTracker} with a ternary on {@code id == -1}. Rather than resize the arrays and
 * binary-search them (StationAPI's fragile approach), we keep the vanilla arrays untouched and store
 * modded dimensions in side maps keyed by serial id. Each modded world is a {@link ReadOnlyServerWorld}
 * delegating to the overworld - exactly like the nether - so it shares the overworld's storage handle
 * but gets its own {@code DIM<n>/} folder via {@link com.periut.retroapi.mixin.dimension.RegionWorldStorageMixin}.
 * Disabled under StationAPI (it owns server dimension arrays).
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
	@Unique private static final Logger RETRO_LOGGER = LogManager.getLogger("RetroAPI/Dimensions");
	/** Vanilla's spawn-region preload reach in blocks (MinecraftServer.loadWorld uses 196). */
	@Unique private static final int RETRO_PRELOAD_REACH = 196;

	@Shadow public ServerWorld[] worlds;
	@Shadow private boolean running;
	@Shadow public PlayerManager playerManager;
	@Shadow int ticks;

	@Unique private final Map<Integer, ServerWorld> retroapi$moddedWorlds = new HashMap<>();
	@Unique private final Map<Integer, EntityTracker> retroapi$moddedTrackers = new HashMap<>();

	@Inject(method = "loadWorld", at = @At("RETURN"))
	private void retroapi$createModdedWorlds(WorldStorageSource storageSource, String worldDir, long seed, CallbackInfo ci) {
		if (this.worlds == null || this.worlds.length == 0 || this.worlds[0] == null) return;
		MinecraftServer self = (MinecraftServer) (Object) this;
		WorldStorage storage = this.worlds[0].getWorldStorage();
		for (DimensionRegistration reg : RetroDimensionRegistry.getAll()) {
			int serialId = reg.getSerialId();
			// ReadOnlyServerWorld(server, sharedStorage, worldDir, dimId, seed, overworldDelegate) - the
			// exact shape vanilla uses for the nether. Its ctor calls Dimension.fromId(serialId) (resolved
			// by DimensionMixin) and storage.getChunkStorage(thatDimension) (routed to DIM<n>/).
			ServerWorld world = new ReadOnlyServerWorld(self, storage, worldDir, serialId, seed, this.worlds[0]);
			world.addEventListener(new ServerWorldEventListener(self, world));
			// Mirror the REST of vanilla's per-world setup too - loadWorld only applies these to
			// the length-2 array, so without them a modded dimension runs at difficulty 0 forever:
			// hostile AI gated on difficulty (e.g. ranged attacks checking `difficulty != 0`)
			// silently never fires, and natural monster spawning stays off.
			world.difficulty = self.properties.getProperty("spawn-monsters", true) ? 1 : 0;
			world.allowSpawning(self.properties.getProperty("spawn-monsters", true), self.spawnAnimals);
			retroapi$moddedWorlds.put(serialId, world);
			retroapi$moddedTrackers.put(serialId, new EntityTracker(self, serialId));

			// Preload the spawn region EXACTLY like vanilla's loadWorld does for the overworld/nether -
			// which is precisely what StationAPI inherits for free by resizing the worlds array so the
			// vanilla spawn-prep loop iterates the modded worlds too. Mirrors that loop byte-for-byte:
			// getSpawnPos-centered, reach 196 in steps of 16, with the lighting-update drain, abortable
			// via `running`. So a modded dimension generates + persists its start region at startup,
			// visible in the log and as DIM<serialId>/region/*.mcr files.
			RETRO_LOGGER.info("Preparing start region for modded dimension {} (serial id {})", reg.getId(), serialId);
			Vec3i spawn = world.getSpawnPos();
			long lastLog = System.currentTimeMillis();
			for (int k = -RETRO_PRELOAD_REACH; k <= RETRO_PRELOAD_REACH && this.running; k += 16) {
				for (int l = -RETRO_PRELOAD_REACH; l <= RETRO_PRELOAD_REACH && this.running; l += 16) {
					long now = System.currentTimeMillis();
					if (now < lastLog) lastLog = now;
					if (now > lastLog + 1000L) {
						int total = (RETRO_PRELOAD_REACH * 2 + 1) * (RETRO_PRELOAD_REACH * 2 + 1);
						int done = (k + RETRO_PRELOAD_REACH) * (RETRO_PRELOAD_REACH * 2 + 1) + l + 1;
						RETRO_LOGGER.info("Preparing spawn area ({}): {}%", reg.getId(), done * 100 / total);
						lastLog = now;
					}
					world.chunkCache.loadChunk(spawn.x + k >> 4, spawn.z + l >> 4);
					while (world.doLightingUpdates() && this.running) {
					}
				}
			}
		}
	}

	/**
	 * Drive the modded worlds + entity trackers every server tick. Vanilla's {@code tick()} only
	 * iterates the fixed length-2 {@code worlds}/{@code entityTrackers} arrays (overworld + nether),
	 * so the side-mapped modded dimensions (created above) were never ticked: entities in them - the
	 * player included - never ran {@code tickEntities()}, so {@code playerTick} never fired (no
	 * inventory/container resync, frozen item entities, no portal cooldown), and the modded
	 * {@link EntityTracker} never sent spawn/move/destroy packets (disconnected players left as
	 * un-despawned clones, dropped items/mobs invisible). Mirror vanilla's per-world tick body
	 * verbatim for each modded world, then tick the modded trackers - the exact work the vanilla
	 * loops do for the two hardcoded dimensions. Disabled under StationAPI.
	 */
	@Inject(method = "tick", at = @At("TAIL"))
	private void retroapi$tickModdedWorlds(CallbackInfo ci) {
		for (ServerWorld world : retroapi$moddedWorlds.values()) {
			if (this.ticks % 20 == 0) {
				this.playerManager.sendToDimension(new WorldTimeUpdateS2CPacket(world.getTime()), world.dimension.id);
			}
			world.tick();
			while (world.doLightingUpdates()) {
			}
			world.tickEntities();
		}
		for (EntityTracker tracker : retroapi$moddedTrackers.values()) {
			tracker.tick();
		}
	}

	/**
	 * Save the side-mapped modded worlds whenever vanilla saves the overworld/nether. Vanilla's
	 * {@code saveWorlds()} only iterates the fixed length-2 {@code worlds[]} array, so the modded
	 * dimensions (created + ticked above) were never persisted: their chunk changes - placed blocks,
	 * block entities, entities, and the RetroAPI {@code retroapi/DIM<n>/} sidecars they drive - were
	 * silently dropped on autosave and shutdown. Mirror the tick driver: drive their save manually too.
	 * (Same lesson as the tick fix: side-mapped state must be driven by hand - vanilla's loops only see
	 * the arrays.) Disabled under StationAPI.
	 */
	@Inject(method = "saveWorlds", at = @At("TAIL"))
	private void retroapi$saveModdedWorlds(CallbackInfo ci) {
		for (ServerWorld world : retroapi$moddedWorlds.values()) {
			// Mirror vanilla's per-world save exactly: saveWithLoadingDisplay persists the CHUNKS (and
			// thus drives the RetroAPI chunk-save sidecar hook), forceSave flushes level/persistent state.
			// forceSave alone (the obvious-but-wrong call) only flushes storage and never saves chunks.
			world.saveWithLoadingDisplay(true, null);
			world.forceSave();
		}
	}

	@Inject(method = "getWorld", at = @At("HEAD"), cancellable = true)
	private void retroapi$getModdedWorld(int dimensionId, CallbackInfoReturnable<ServerWorld> cir) {
		ServerWorld world = retroapi$moddedWorlds.get(dimensionId);
		if (world != null) cir.setReturnValue(world);
	}

	@Inject(method = "getEntityTracker", at = @At("HEAD"), cancellable = true)
	private void retroapi$getModdedTracker(int dimensionId, CallbackInfoReturnable<EntityTracker> cir) {
		EntityTracker tracker = retroapi$moddedTrackers.get(dimensionId);
		if (tracker != null) cir.setReturnValue(tracker);
	}
}
