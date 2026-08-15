package com.periut.retroapi.mixin.register;

import com.periut.retroapi.entity.spawnegg.Spawners;
import com.periut.retroapi.register.blockentity.RetroSyncedBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes the vanilla mob spawner say what it spawns - to the client, and to disk without breaking it.
 *
 * <p><b>To the client.</b> b1.7.3 has no packet that carries a spawner's mob, so on a server every
 * spawner in the world renders as the pig the block entity is constructed with, whatever it actually
 * spawns. Implementing {@link RetroSyncedBlockEntity} is the whole fix: RetroAPI's block-entity sync
 * already hooks the two moments that matter - the block entities sent with a chunk, and
 * {@code sendBlockEntityUpdate} when one is marked dirty - and both test for exactly this interface.
 * The wire form is only the name, because the delay and the rotation are the client's own business.
 *
 * <p><b>To disk.</b> See {@link Spawners}: a vanilla mob is written where vanilla writes it and nothing
 * else happens; a modded one writes {@code Pig} into the vanilla field so an unmodded game still reads a
 * working spawner, and puts its real name in a key beside it that only RetroAPI looks for.
 */
@Mixin(MobSpawnerBlockEntity.class)
public abstract class MobSpawnerSyncMixin implements RetroSyncedBlockEntity {

	@Shadow private String spawnedEntityId;

	@Override
	public void writeSyncNbt(final NbtCompound nbt) {
		// The REAL name, modded or not: the client has the same entity registry and needs to know which
		// mob to draw in the block. The vanilla-safe substitution is a disk concern, not a wire one.
		nbt.putString("EntityId", spawnedEntityId);
	}

	@Override
	public void readSyncNbt(final NbtCompound nbt) {
		final String id = nbt.getString("EntityId");
		if (id != null && !id.isEmpty()) {
			spawnedEntityId = id;
		}
	}

	/** Runs after vanilla has written {@code EntityId}, so a modded name is replaced rather than added to. */
	@Inject(method = "writeNbt", at = @At("RETURN"))
	private void retroapi$writeModdedMob(final NbtCompound nbt, final CallbackInfo ci) {
		if (Spawners.isModded(spawnedEntityId)) {
			nbt.putString("EntityId", Spawners.VANILLA_FALLBACK);
			nbt.putString(Spawners.MODDED_KEY, spawnedEntityId);
		}
	}

	/**
	 * Puts the modded name back after vanilla has read its own field.
	 *
	 * <p>Only if that mob can still be made: uninstall the mod and the spawner quietly stays the pig it
	 * claims to be on disk, rather than becoming a spawner that spawns nothing.
	 */
	@Inject(method = "readNbt", at = @At("RETURN"))
	private void retroapi$readModdedMob(final NbtCompound nbt, final CallbackInfo ci) {
		final String modded = nbt.getString(Spawners.MODDED_KEY);
		if (Spawners.isKnown(modded)) {
			spawnedEntityId = modded;
		}
	}
}
