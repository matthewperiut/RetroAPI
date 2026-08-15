package com.periut.retroapi.entity.spawnegg;

import com.periut.retroapi.register.blockentity.RetroBlockEntities;
import com.periut.retroapi.registry.RetroRegistry;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import com.periut.retroapi.mixin.entity.EntityRegistryAccessor;
import net.minecraft.world.World;

/**
 * What mob a spawner spawns: setting it, telling clients about it, and getting it onto disk without
 * making the save unreadable to anything that does not know RetroAPI.
 *
 * <p><b>On disk.</b> A vanilla mob is written exactly where vanilla writes it, {@code EntityId}, and
 * nothing else changes - a RetroAPI world opened in plain beta finds the spawner it expects. A modded
 * mob cannot go there: vanilla would read a name its {@code EntityRegistry} has never heard of and the
 * spawner would spawn nothing at all. So the vanilla field gets {@link #VANILLA_FALLBACK} - the spawner
 * beta itself defaults to - and the real name goes in {@link #MODDED_KEY} beside it, which vanilla
 * ignores and RetroAPI reads back. Remove the mod and the spawner is a pig spawner; put it back and it
 * is a moa spawner again, with nothing lost in between.
 *
 * <p><b>On the wire.</b> b1.7.3 never tells a client what a spawner holds - there is no packet for it,
 * so every spawner on a server renders as vanilla's default pig. RetroAPI's block-entity sync carries
 * it: see {@code MobSpawnerSyncMixin}, which makes the vanilla block entity a
 * {@link com.periut.retroapi.register.blockentity.RetroSyncedBlockEntity} and so picks up both of that
 * system's hooks - the state a chunk is sent with, and every later change.
 */
public final class Spawners {
    private Spawners() {
    }

    /** Where the modded name lives, alongside vanilla's own {@code EntityId} rather than instead of it. */
    public static final String MODDED_KEY = "retroapi:EntityId";

    /** What a modded spawner claims to be in the vanilla field: beta's own default. */
    public static final String VANILLA_FALLBACK = "Pig";

    /**
     * Points a spawner at a mob and tells everyone watching.
     *
     * @return false when there is no spawner there
     */
    public static boolean setMob(final World world, final int x, final int y, final int z, final String entityId) {
        if (world == null || entityId == null || entityId.isEmpty()) {
            return false;
        }
        if (!(world.getBlockEntity(x, y, z) instanceof MobSpawnerBlockEntity spawner)) {
            return false;
        }

        spawner.setSpawnedEntityId(entityId);
        // Marks it dirty, which is the same path a modded block entity's own changes take: on a server
        // it reaches every player tracking the chunk, and in singleplayer there is nobody to tell.
        RetroBlockEntities.sync(spawner);
        return true;
    }

    /** Whether this name is a mod's rather than one of beta's own. */
    public static boolean isModded(final String entityId) {
        return entityId != null && !entityId.isEmpty() && RetroRegistry.getEntityByStringId(entityId) != null;
    }

    /** Whether anything at all can be made from this name right now - a mod may since have been removed. */
    public static boolean isKnown(final String entityId) {
        return entityId != null && !entityId.isEmpty()
            && EntityRegistryAccessor.retroapi$getIdToClass().containsKey(entityId);
    }
}
