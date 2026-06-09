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
		// dedicated server never loads ClientTeleporter or its client-only class refs.
		DimensionHelper.setTeleporter(new ClientTeleporter());

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

		if (!hasStationAPI) {
			ClientPlayNetworking.registerListener(RetroAPINetworking.ID_SYNC_CHANNEL, (ctx, buffer) -> {
				// Do NOT use ensureOnMainThread() here - the ID remap must complete
				// synchronously on the network thread before any subsequent packets
				// (inventory, chunk data) are deserialized, otherwise Item.BY_ID / Block.BY_ID
				// lookups will NPE for server-assigned IDs that haven't been remapped yet.
				RetroAPI.LOGGER.info("Received ID sync packet from server");
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
						RetroAPI.LOGGER.info("Synced dimension {} -> serial id {} (was {})", idStr, serialId, reg.getSerialId());
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
	}
}

