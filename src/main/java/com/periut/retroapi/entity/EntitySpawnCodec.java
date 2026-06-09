package com.periut.retroapi.entity;

import com.periut.retroapi.entity.spawn.RetroEntitySpawnData;
import com.periut.retroapi.entity.spawn.RetroHasOwner;
import com.periut.retroapi.entity.spawn.RetroMobSpawnData;
import com.periut.retroapi.network.RetroAPINetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.ornithemc.osl.networking.api.PacketBuffer;
import net.ornithemc.osl.networking.api.server.ServerPlayNetworking;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Server-side: writes a modded entity's spawn payload onto an OSL {@link PacketBuffer} and sends it to one
 * player, replacing the vanilla spawn packet (which throws for unknown entity types). The wire format is a
 * faithful, sequential mirror of StationAPI's {@code EntitySpawnDataProvider}/{@code MobSpawnDataProvider}
 * (positions are {@code floor(coord*32)}, rotations are {@code angle*256/360} bytes, the DataTracker blob is
 * {@code writeAllEntries}). The client side ({@code entity.client.EntitySpawnClient}) reads it back in order.
 */
public final class EntitySpawnCodec {
	private EntitySpawnCodec() {}

	private static final double MAX_VELOCITY = 3.9D;

	/**
	 * Spawn sends attempted before the player's OSL channel handshake completes, queued for re-send at
	 * {@code PLAY_READY}. OSL's {@code ServerPlayNetworking.send(player, ...)} SILENTLY DROPS packets
	 * until {@code isPlayReady(player, channel)} - and the entity tracker sends spawns on the joining
	 * player's very first ticks (vanilla {@code EntityTracker.onEntityAdded(ServerPlayerEntity)} pushes
	 * every existing tracked entity through {@code updateListener} immediately), which is before the
	 * handshake. Vanilla then marks the player as a listener and never re-sends, so without this queue
	 * every modded entity is permanently invisible to a joining client. (StationAPI doesn't need this:
	 * its MessagePacket is a vanilla-registered packet with no readiness gate.) Weak keys so entries
	 * vanish with disconnected players (e.g. vanilla clients whose handshake never completes).
	 */
	private static final Map<ServerPlayerEntity, Set<Entity>> PENDING = new WeakHashMap<>();

	/** Send the appropriate spawn packet for a RetroAPI entity to one player. */
	public static void sendSpawn(ServerPlayerEntity player, Entity entity) {
		NamespacedIdentifier channel;
		if (entity instanceof RetroMobSpawnData) {
			channel = RetroAPINetworking.ENTITY_SPAWN_MOB_CHANNEL;
		} else if (entity instanceof RetroEntitySpawnData) {
			channel = RetroAPINetworking.ENTITY_SPAWN_CHANNEL;
		} else {
			// Registered-but-no-spawn-data entities track but don't network-spawn (mod author must implement a
			// RetroEntitySpawnData/RetroMobSpawnData interface) - they simply won't render client-side.
			return;
		}

		if (!ServerPlayNetworking.isPlayReady(player, channel)) {
			synchronized (PENDING) {
				PENDING.computeIfAbsent(player, p -> new LinkedHashSet<>()).add(entity);
			}
			return;
		}

		if (entity instanceof RetroMobSpawnData) {
			ServerPlayNetworking.send(player, channel, buf -> writeMob(buf, (LivingEntity) entity));
		} else {
			ServerPlayNetworking.send(player, channel, buf -> writeEntity(buf, entity));
		}
	}

	/**
	 * Re-send the spawns that were attempted before this player's channel handshake completed. Called
	 * from the {@code PLAY_READY} listener in {@code RetroAPIServer} (registered unconditionally - the
	 * RetroAPI entity spawn path stays active under StationAPI too). Skips entities that died or changed
	 * worlds while queued; the client handler spawns into its current world, so a cross-dimension send
	 * would materialize the entity in the wrong world.
	 */
	public static void flushPending(ServerPlayerEntity player) {
		Set<Entity> queued;
		synchronized (PENDING) {
			queued = PENDING.remove(player);
		}
		if (queued == null) return;
		for (Entity entity : queued) {
			if (entity.dead || entity.world != player.world) continue;
			sendSpawn(player, entity);
		}
	}

	private static void writeEntity(PacketBuffer buf, Entity entity) throws IOException {
		RetroEntitySpawnData p = (RetroEntitySpawnData) entity;
		buf.writeString(p.getHandlerId().toString());
		buf.writeVarInt(entity.id);
		buf.writeInt(MathHelper.floor(entity.x * 32.0D));
		buf.writeInt(MathHelper.floor(entity.y * 32.0D));
		buf.writeInt(MathHelper.floor(entity.z * 32.0D));

		int ownerId = 0;
		if (entity instanceof RetroHasOwner owned) {
			Entity owner = owned.getOwner();
			ownerId = (owner == null ? entity : owner).id;
		}
		buf.writeVarInt(ownerId);
		if (ownerId > 0) {
			buf.writeShort((int) (clamp(entity.velocityX) * 8000));
			buf.writeShort((int) (clamp(entity.velocityY) * 8000));
			buf.writeShort((int) (clamp(entity.velocityZ) * 8000));
		}

		boolean sync = p.syncTrackerAtSpawn();
		buf.writeBoolean(sync);
		if (sync) {
			buf.writeByteArray(dataTrackerBytes(entity));
		}
		p.writeExtra(buf);
	}

	private static void writeMob(PacketBuffer buf, LivingEntity mob) throws IOException {
		RetroMobSpawnData p = (RetroMobSpawnData) mob;
		buf.writeString(p.getHandlerId().toString());
		buf.writeVarInt(mob.id);
		buf.writeInt(MathHelper.floor(mob.x * 32.0D));
		buf.writeInt(MathHelper.floor(mob.y * 32.0D));
		buf.writeInt(MathHelper.floor(mob.z * 32.0D));
		buf.writeByte((int) (mob.yaw * 256.0F / 360.0F));
		buf.writeByte((int) (mob.pitch * 256.0F / 360.0F));
		buf.writeByteArray(dataTrackerBytes(mob)); // mobs always sync the tracker at spawn
		p.writeExtra(buf);
	}

	private static byte[] dataTrackerBytes(Entity entity) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		entity.getDataTracker().writeAllEntries(new DataOutputStream(out));
		return out.toByteArray();
	}

	private static double clamp(double v) {
		return Math.max(-MAX_VELOCITY, Math.min(MAX_VELOCITY, v));
	}
}
