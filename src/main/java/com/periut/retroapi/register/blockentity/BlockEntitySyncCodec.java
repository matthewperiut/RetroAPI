package com.periut.retroapi.register.blockentity;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Turns a {@link RetroSyncedBlockEntity}'s state into the bytes that ride the
 * {@code retroapi:block_entity_sync} channel, and back. Common to both sides so the two halves can't
 * disagree about the format.
 */
public final class BlockEntitySyncCodec {

	private BlockEntitySyncCodec() {}

	/** Serialises a block entity's sync NBT, or null if it produced nothing writable. */
	public static byte[] encode(BlockEntity blockEntity) {
		if (!(blockEntity instanceof RetroSyncedBlockEntity)) {
			return null;
		}
		try {
			NbtCompound nbt = new NbtCompound();
			((RetroSyncedBlockEntity) blockEntity).writeSyncNbt(nbt);
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (DataOutputStream out = new DataOutputStream(bytes)) {
				NbtIo.write(nbt, out);
			}
			return bytes.toByteArray();
		} catch (IOException e) {
			// A dropped sync is non-fatal: the block entity still works server-side, the client just
			// shows stale data until the next update.
			return null;
		}
	}

	/** Applies bytes from {@link #encode} to a block entity on the receiving side. */
	public static void decode(BlockEntity blockEntity, byte[] data) {
		if (!(blockEntity instanceof RetroSyncedBlockEntity) || data == null) {
			return;
		}
		try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
			NbtCompound nbt = NbtIo.read(in);
			RetroSyncedBlockEntity synced = (RetroSyncedBlockEntity) blockEntity;
			synced.readSyncNbt(nbt);
			synced.onSynced();
		} catch (IOException e) {
			// see encode()
		}
	}
}
