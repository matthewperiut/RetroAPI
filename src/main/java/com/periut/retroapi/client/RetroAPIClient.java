package com.periut.retroapi.client;

import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.dimension.DimensionHelper;
import com.periut.retroapi.dimension.DimensionRegistration;
import com.periut.retroapi.dimension.RetroDimensionRegistry;
import com.periut.retroapi.client.dimension.ClientTeleporter;
import com.periut.retroapi.network.RetroAPINetworking;
import com.periut.retroapi.entity.client.EntitySpawnClient;
import com.periut.retroapi.lang.LangLoader;
import com.periut.retroapi.registry.IdAssigner;
import net.fabricmc.loader.api.FabricLoader;
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers;
import com.periut.retroapi.client.sound.ClientSoundDedup;
import net.ornithemc.osl.entrypoints.api.client.ClientModInitializer;
import net.ornithemc.osl.lifecycle.api.client.MinecraftClientEvents;
import net.ornithemc.osl.networking.api.client.ClientPlayNetworking;

public class RetroAPIClient implements ClientModInitializer {
	@Override
	public void initClient() {
		boolean hasStationAPI = FabricLoader.getInstance().isModLoaded("stationapi");

		// The client's dimension teleport back end. Referenced only here (client entrypoint), so the
		// dedicated server never loads ClientTeleporter or its client-only class refs. Registered into
		// the CLIENT slot so that under the integrated server (both entrypoints in one JVM) it does not
		// clobber the server back end.
		DimensionHelper.setClientTeleporter(new ClientTeleporter());

		// The client's container-GUI opener (singleplayer direct-open). Referenced only here.
		com.periut.retroapi.gui.RetroGuis.setClientOpener(new com.periut.retroapi.gui.client.ClientGuiOpener());

		// The JSON-model renderer behind the built-in retroapi:model render type. The id is
		// registered on both sides (RenderType's static table); the renderer is client-only.
		com.periut.retroapi.register.rendertype.RenderType.attach(
			com.periut.retroapi.register.rendertype.RenderTypes.MODEL,
			new com.periut.retroapi.client.model.ModelBlockRenderer());

		MinecraftClientEvents.READY.register(minecraft -> {
			LangLoader.loadTranslations();
		});

		// The commands API's client half: the translation resolver, and the listeners for the tree,
		// completions and rich messages a server sends.
		com.periut.retroapi.commands.util.NetworkingUtil.registerClient();

		// Game modes as the server has them (every player's, not just this client's).
		com.periut.retroapi.gamemode.GameModeNetworking.registerClient();

		// A server's world-scoped config values, and whether this player is allowed to change them.
		com.periut.retroapi.config.ConfigNetworking.registerClient();

		// The creative screen's way back to the server. Registered here so the container - which is
		// common code a dedicated server loads - never names a client-only class itself.
		com.periut.retroapi.gamemode.CreativeSync.Holder.set(new com.periut.retroapi.gamemode.CreativeSync() {
			@Override
			public void give(net.minecraft.item.ItemStack stack) {
				com.periut.retroapi.gamemode.GameModeNetworking.requestCreativeItem(stack);
			}

			@Override
			public void setSlot(int slot, net.minecraft.item.ItemStack stack) {
				com.periut.retroapi.gamemode.GameModeNetworking.setCreativeSlot(slot, stack);
			}
		});

		// Game rules as the server has them. Arrives as a full set on join and as single changes
		// after that; either way it replaces what this client believed.
		ClientPlayNetworking.registerListener(RetroAPINetworking.GAMERULE_CHANNEL, (ctx, buffer) -> {
			int count = buffer.readVarInt();
			java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
			for (int i = 0; i < count; i++) {
				values.put(buffer.readString(), buffer.readString());
			}
			ctx.ensureOnMainThread();
			if (count == 1) {
				values.forEach(com.periut.retroapi.gamerule.RetroGameRules::applyFromServer);
			} else {
				com.periut.retroapi.gamerule.RetroGameRules.applyFromServer(values);
			}
		});

		if (!hasStationAPI) {
			ClientPlayNetworking.registerListener(RetroAPINetworking.ID_SYNC_CHANNEL, (ctx, buffer) -> {
				// Do NOT use ensureOnMainThread() here - the ID remap must complete
				// synchronously on the network thread before any subsequent packets
				// (inventory, chunk data) are deserialized, otherwise Item.BY_ID / Block.BY_ID
				// lookups will NPE for server-assigned IDs that haven't been remapped yet.
				RetroAPI.LOGGER.debug("Received ID sync packet from server");
				IdAssigner.applyFromNetwork(buffer);
			});

			// Align our modded dimension serial ids with the server's so PlayerRespawnPacket dimension
			// ids resolve correctly. Runs on the net thread (no world mutation, just registry serial ids).
			ClientPlayNetworking.registerListener(RetroAPINetworking.DIM_SYNC_CHANNEL, (ctx, buffer) -> {
				int count = buffer.readVarInt();
				for (int i = 0; i < count; i++) {
					String idStr = buffer.readString();
					int serialId = buffer.readVarInt();
					String[] parts = idStr.split(":", 2);
					if (parts.length != 2) continue;
					DimensionRegistration reg = RetroDimensionRegistry.getByIdentifier(
						NamespacedIdentifiers.from(parts[0], parts[1]));
					if (reg != null && reg.getSerialId() != serialId) {
						RetroAPI.LOGGER.debug("Synced dimension {} -> serial id {} (was {})", idStr, serialId, reg.getSerialId());
						reg.setSerialId(serialId);
					}
				}
			});
		}

		// Modded entity spawns ALWAYS use RetroAPI's OSL path - even under StationAPI. RetroAPI entities
		// implement RetroAPI's spawn interfaces (not StationAPI's CustomSpawnDataProvider), so the two
		// spawn systems are orthogonal (disjoint entity sets, byte-identical wire format) and coexist
		// without interference. Reconstruction resolves the factory by string id via RetroRegistry, so it
		// does not depend on the (StationAPI-gated) numeric id-sync above.
		// Server-driven container GUI open: look up the registered screen factory and open it with a
		// client-side inventory stand-in; window sync id ties vanilla item-sync packets to the container.
		ClientPlayNetworking.registerListener(RetroAPINetworking.OPEN_GUI_CHANNEL, (ctx, buffer) -> {
			String idStr = buffer.readString();
			int syncId = buffer.readVarInt();
			ctx.ensureOnMainThread();
			com.periut.retroapi.gui.client.RetroGuiHandler handler = com.periut.retroapi.gui.client.RetroGuiRegistry.get(idStr);
			if (handler != null) {
				net.minecraft.entity.player.PlayerEntity player = ctx.minecraft().player;
				ctx.minecraft().setScreen(handler.screenFactory().create(player, handler.inventoryFactory().create()));
				player.currentScreenHandler.syncId = syncId;
			}
		});

		// Bridged particles: the server has no particle packet in b1.7.3, so RetroAPI forwards
		// world.addParticle calls over OSL and replays them in the local world here.
		ClientPlayNetworking.registerListener(RetroAPINetworking.PARTICLE_CHANNEL, (ctx, buffer) -> {
			String name = buffer.readString();
			double x = buffer.readDouble();
			double y = buffer.readDouble();
			double z = buffer.readDouble();
			double vx = buffer.readDouble();
			double vy = buffer.readDouble();
			double vz = buffer.readDouble();
			ctx.ensureOnMainThread();
			net.minecraft.world.World world = ctx.minecraft().world;
			if (world != null) {
				world.addParticle(name, x, y, z, vx, vy, vz);
			}
		});

		ClientPlayNetworking.registerListener(RetroAPINetworking.ENTITY_SPAWN_CHANNEL,
			EntitySpawnClient::handleEntitySpawn);
		ClientPlayNetworking.registerListener(RetroAPINetworking.ENTITY_SPAWN_MOB_CHANNEL,
			EntitySpawnClient::handleMobSpawn);

		// Single-block flattened-state updates (RetroStates.set on the server): apply the low
		// bits through setBlockMeta and the high bits into the chunk's xmeta, then re-render.
		ClientPlayNetworking.registerListener(RetroAPINetworking.STATE_SYNC_CHANNEL, (ctx, buffer) -> {
			int x = buffer.readInt();
			int y = buffer.readInt();
			int z = buffer.readInt();
			int index = buffer.readVarInt();
			ctx.ensureOnMainThread();
			net.minecraft.world.World world = ctx.minecraft().world;
			if (world != null) {
				com.periut.retroapi.state.RetroStates.applySynced(world, x, y, z, index);
			}
		});

		// Single-position auxiliary data updates (RetroBlockData.set on the server). Whole chunks come
		// in on the chunk packet; this is the post-load change - a block being re-clad, a counter moving.
		ClientPlayNetworking.registerListener(RetroAPINetworking.BLOCK_DATA_CHANNEL, (ctx, buffer) -> {
			int x = buffer.readInt();
			int y = buffer.readInt();
			int z = buffer.readInt();
			String key = buffer.readString();
			int value = buffer.readInt();
			ctx.ensureOnMainThread();
			net.minecraft.world.World world = ctx.minecraft().world;
			if (world != null) {
				com.periut.retroapi.storage.RetroBlockData.applySynced(world, x, y, z, key, value);
			}
		});

		// Cubic (cave) biome cells: a whole chunk's table on chunk send, a single cell on change.
		// Cells arrive keyed by biome ID STRING, because runtime ids are allocated per side.
		ClientPlayNetworking.registerListener(RetroAPINetworking.CUBIC_BIOME_CHANNEL, (ctx, buffer) -> {
			int chunkX = buffer.readInt();
			int chunkZ = buffer.readInt();
			int count = buffer.readShort() & 0xFFFF;
			int[] cellYs = new int[count];
			String[] biomeIds = new String[count];
			for (int i = 0; i < count; i++) {
				cellYs[i] = buffer.readInt();
				biomeIds[i] = buffer.readString();
			}
			ctx.ensureOnMainThread();
			net.minecraft.world.World world = ctx.minecraft().world;
			if (world == null) {
				return;
			}
			for (int i = 0; i < count; i++) {
				com.periut.retroapi.biome.cubic.RetroCubicBiomes.applySynced(world, chunkX, cellYs[i], chunkZ, biomeIds[i]);
			}
		});

		// Block entity sync: b1.7.3 has no generic block-entity packet, so the server sends a modded
		// block entity's NBT over OSL (ChunkMapBlockEntitySyncMixin on change, and
		// PlayerChunkBlockEntitySyncMixin on chunk send) and we apply it to the client's own copy.
		ClientPlayNetworking.registerListener(RetroAPINetworking.BLOCK_ENTITY_SYNC_CHANNEL, (ctx, buffer) -> {
			int x = buffer.readInt();
			int y = buffer.readInt();
			int z = buffer.readInt();
			byte[] data = buffer.readByteArray();
			ctx.ensureOnMainThread();
			net.minecraft.world.World world = ctx.minecraft().world;
			if (world == null) {
				return;
			}
			net.minecraft.block.entity.BlockEntity blockEntity = world.getBlockEntity(x, y, z);
			if (blockEntity == null) {
				// The chunk has not landed yet. This IS the chunk-send pass, so nothing will send it
				// again - hold it and apply it as soon as the block entity exists.
				com.periut.retroapi.register.blockentity.PendingBlockEntitySync.stash(x, y, z, data);
				return;
			}
			com.periut.retroapi.register.blockentity.BlockEntitySyncCodec.decode(blockEntity, data);
			world.setBlocksDirty(x, y, z, x, y, z);
		});

		// World sound bridge: b1.7.3 has no sound packet, so the server forwards world.playSound
		// calls over OSL (ServerWorldSoundMixin) and we play them through the local world here.
		// Local plays win: if the client already played this sound itself (prediction, entity
		// status hurt sounds, ...), the bridged copy is a duplicate and is dropped (ClientSoundDedup).
		ClientPlayNetworking.registerListener(RetroAPINetworking.PLAY_SOUND_CHANNEL, (ctx, buffer) -> {
			String sound = buffer.readString();
			double x = buffer.readDouble();
			double y = buffer.readDouble();
			double z = buffer.readDouble();
			float volume = buffer.readFloat();
			float pitch = buffer.readFloat();
			ctx.ensureOnMainThread();
			net.minecraft.world.World world = ctx.minecraft().world;
			if (world != null && !ClientSoundDedup.consumeIfRecentlyPlayed(sound, x, y, z)) {
				ClientSoundDedup.applyingBridgedSound = true;
				try {
					world.playSound(x, y, z, sound, volume, pitch);
				} finally {
					ClientSoundDedup.applyingBridgedSound = false;
				}
			}
		});

		// The `retroapi-client` entrypoint: mods' renderers, screens, particle factories and block
		// colors, run once the client platform above is in place. Last, so everything it may touch exists.
		com.periut.retroapi.entrypoint.RetroEntrypoints.invokeClient();
	}
}

