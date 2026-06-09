package com.periut.retroapi.entity.client;

import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.entity.EntityRegistration;
import com.periut.retroapi.entity.spawn.RetroEntitySpawnData;
import com.periut.retroapi.entity.spawn.RetroHasOwner;
import com.periut.retroapi.entity.spawn.RetroMobSpawnData;
import com.periut.retroapi.mixin.entity.client.ClientNetworkHandlerAccessor;
import com.periut.retroapi.registry.RetroRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.world.ClientWorld;
import net.ornithemc.osl.networking.api.PacketBuffer;
import net.ornithemc.osl.networking.api.client.ClientPacketListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Client-side spawn handlers for {@link com.periut.retroapi.network.RetroAPINetworking#ENTITY_SPAWN_CHANNEL}
 * and {@code ENTITY_SPAWN_MOB_CHANNEL}. Reconstructs the modded entity and registers it under the server's id
 * via {@link ClientWorld#forceEntity}, so subsequent vanilla position/velocity/despawn packets drive it.
 * Reads fields in the exact order {@link com.periut.retroapi.entity.EntitySpawnCodec} writes them. Mirrors
 * StationAPI's {@code EntityClientNetworkHandler}.
 */
public final class EntitySpawnClient {
	private EntitySpawnClient() {}

	public static void handleEntitySpawn(ClientPacketListener.Context ctx, PacketBuffer buf) throws IOException {
		ctx.ensureOnMainThread(); // reschedules onto the main thread, then re-runs here

		String handler = buf.readString();
		int id = buf.readVarInt();
		int tx = buf.readInt(), ty = buf.readInt(), tz = buf.readInt();
		int ownerId = buf.readVarInt();
		double vx = 0, vy = 0, vz = 0;
		if (ownerId > 0) {
			vx = buf.readShort() / 8000.0D;
			vy = buf.readShort() / 8000.0D;
			vz = buf.readShort() / 8000.0D;
		}
		boolean sync = buf.readBoolean();
		byte[] trackerBytes = sync ? buf.readByteArray() : null;

		EntityRegistration reg = RetroRegistry.getEntityByStringId(handler);
		if (reg == null || reg.getEntityFactory() == null) {
			RetroAPI.LOGGER.warn("No entity factory for spawn handler {}", handler);
			return;
		}
		ClientWorld world = (ClientWorld) ctx.minecraft().world;
		if (world == null) return;

		Entity e = reg.getEntityFactory().create(world, tx / 32.0D, ty / 32.0D, tz / 32.0D);
		if (e == null) return;
		e.trackedPosX = tx;
		e.trackedPosY = ty;
		e.trackedPosZ = tz;
		e.yaw = 0.0F;
		e.pitch = 0.0F;
		e.id = id;
		world.forceEntity(id, e);

		if (ownerId > 0) {
			if (e instanceof RetroHasOwner owned) {
				owned.setOwner(((ClientNetworkHandlerAccessor) ctx.networkHandler()).retroapi$getEntity(ownerId));
			}
			e.setVelocityClient(vx, vy, vz);
		}
		if (trackerBytes != null) {
			e.getDataTracker().writeUpdatedEntries(DataTracker.readEntries(toStream(trackerBytes)));
		}
		if (e instanceof RetroEntitySpawnData provider) {
			provider.readExtra(buf);
		}
	}

	public static void handleMobSpawn(ClientPacketListener.Context ctx, PacketBuffer buf) throws IOException {
		ctx.ensureOnMainThread();

		String handler = buf.readString();
		int id = buf.readVarInt();
		int tx = buf.readInt(), ty = buf.readInt(), tz = buf.readInt();
		float yaw = buf.readByte() * 360 / 256.0F;
		float pitch = buf.readByte() * 360 / 256.0F;
		byte[] trackerBytes = buf.readByteArray();

		EntityRegistration reg = RetroRegistry.getEntityByStringId(handler);
		if (reg == null || reg.getMobFactory() == null) {
			RetroAPI.LOGGER.warn("No mob factory for spawn handler {}", handler);
			return;
		}
		ClientWorld world = (ClientWorld) ctx.minecraft().world;
		if (world == null) return;

		LivingEntity mob = reg.getMobFactory().create(world);
		if (mob == null) return;
		mob.trackedPosX = tx;
		mob.trackedPosY = ty;
		mob.trackedPosZ = tz;
		mob.id = id;
		mob.setPositionAndAngles(tx / 32.0D, ty / 32.0D, tz / 32.0D, yaw, pitch);
		mob.interpolateOnly = true;
		world.forceEntity(id, mob);
		mob.getDataTracker().writeUpdatedEntries(DataTracker.readEntries(toStream(trackerBytes)));

		if (mob instanceof RetroMobSpawnData provider) {
			provider.readExtra(buf);
		}
	}

	private static DataInputStream toStream(byte[] bytes) {
		return new DataInputStream(new ByteArrayInputStream(bytes));
	}
}
