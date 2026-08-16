package com.periut.retroapi.commands.argument;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies {@code /summon}'s NBT to an entity, and works out which attributes an entity has.
 *
 * <p><b>How it applies.</b> Beta splits its NBT in two: {@code Entity.write}/{@code read} handle the
 * universal fields (position, motion, rotation, fire, air) and then hand off to {@code writeNbt}/
 * {@code readNbt}, which each subclass overrides for its own ({@code Color}, {@code powered},
 * {@code Fuse}). Writing the entity out, laying the player's keys over the result and reading it back
 * runs both halves, so one path covers every attribute of every entity:
 *
 * <pre>{@code
 * NbtCompound full = new NbtCompound();
 * entity.write(full);          // everything it currently is
 * full.put(key, value);        // ...with the player's changes over the top
 * entity.read(full);
 * }</pre>
 *
 * <p>Laying the keys over a <em>full</em> compound rather than reading a sparse one is the part that
 * matters: beta's {@code getShort} answers 0 for a key that is not there, so reading a compound
 * holding only {@code Color} would set the sheep's colour and its health to zero in the same call.
 *
 * <p><b>Why mods need no API for this.</b> A modded entity already overrides {@code writeNbt} and
 * {@code readNbt} - it has to, or it would not survive a save - so its attributes are summonable the
 * moment it is registered, under the same names it uses on disk, with suggestions and all. There is
 * nothing to register and nothing to keep in step.
 */
public final class EntityAttributes {
    /** Attribute names per entity id. Built once per type from a throwaway entity. */
    private static final Map<String, List<String>> KEYS = new ConcurrentHashMap<>();

    private EntityAttributes() {
    }

    /**
     * Lays {@code attributes} over the entity's current state.
     *
     * @return false if the entity rejected them, which means its own {@code readNbt} threw - a value
     *         of the wrong type for the field it names is the usual cause
     */
    public static boolean apply(final Entity entity, final NbtCompound attributes) {
        if (entity == null || attributes == null) {
            return true;
        }

        final NbtCompound full = new NbtCompound();
        try {
            entity.write(full);
            for (final NbtElement value : attributes.values()) {
                full.put(value.getKey(), value);
            }
            entity.read(full);
            return true;
        } catch (final RuntimeException ignored) {
            // A bad value must not take the command - or the server - down with it.
            return false;
        }
    }

    /**
     * Every attribute name this entity type accepts, sorted.
     *
     * <p>Read off a throwaway entity rather than a table, so it is right by construction for modded
     * entities and cannot drift from what the class actually writes. The entity is never added to the
     * world; it exists for the length of one {@code write} call.
     */
    public static List<String> keys(final String betaId, final World world) {
        if (betaId == null || world == null) {
            return List.of();
        }

        final List<String> cached = KEYS.get(betaId);
        if (cached != null) {
            return cached;
        }

        final List<String> keys = new ArrayList<>();
        try {
            final Entity sample = EntityRegistry.create(betaId, world);
            if (sample != null) {
                final NbtCompound nbt = new NbtCompound();
                sample.write(nbt);
                final TreeSet<String> sorted = new TreeSet<>();
                for (final NbtElement value : nbt.values()) {
                    sorted.add(value.getKey());
                }
                keys.addAll(sorted);
            }
        } catch (final RuntimeException | LinkageError ignored) {
            // An entity that cannot be built cannot be described; suggest nothing rather than fail.
        }

        // Cached even when empty: a type that could not be sampled will not start being sampleable,
        // and retrying it on every keystroke is what makes a completion feel slow.
        KEYS.put(betaId, keys);
        return keys;
    }
}
