package com.periut.retroapi.entity.spawnegg;

import com.periut.retroapi.register.item.RetroItemAccess;
import net.minecraft.item.Item;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The spawn eggs, one per mob beta has, in the order modern's Spawn Eggs tab lists them.
 *
 * <p>Registered under {@code minecraft:} for the same reason the command blocks are: these are
 * vanilla items beta happens not to have yet. The one departure from modern's naming is the pigman:
 * modern renamed that mob to the zombified piglin long after beta, and in a beta world it is still a
 * pigman, so the egg is {@code minecraft:pigman_spawn_egg} and reads "Pigman Spawn Egg".
 *
 * <p>The textures are modern's own per-mob sprites - since 1.21 modern draws each egg from its own
 * file rather than tinting one template twice, so there is nothing to recolour here.
 *
 * <p>No egg for Giant or Monster: modern has none either, and both are mobs beta never spawns.
 */
public final class SpawnEggs {
    private SpawnEggs() {
    }

    /** Egg name (also its texture) -> the vanilla {@code EntityRegistry} id it spawns. */
    private static final Map<String, String> EGGS = new LinkedHashMap<>();

    static {
        // Modern's Spawn Eggs tab order: the farm animals, then the rest of the passives, then the
        // hostiles, each group alphabetical.
        EGGS.put("chicken_spawn_egg", "Chicken");
        EGGS.put("cow_spawn_egg", "Cow");
        EGGS.put("pig_spawn_egg", "Pig");
        EGGS.put("sheep_spawn_egg", "Sheep");
        EGGS.put("wolf_spawn_egg", "Wolf");
        EGGS.put("squid_spawn_egg", "Squid");
        EGGS.put("skeleton_spawn_egg", "Skeleton");
        EGGS.put("zombie_spawn_egg", "Zombie");
        EGGS.put("spider_spawn_egg", "Spider");
        EGGS.put("creeper_spawn_egg", "Creeper");
        EGGS.put("slime_spawn_egg", "Slime");
        EGGS.put("ghast_spawn_egg", "Ghast");
        EGGS.put("pigman_spawn_egg", "PigZombie");
    }

    /** RetroAPI's plain egg, for a modded mob whose author has not drawn one. */
    public static final NamespacedIdentifier DEFAULT_TEXTURE =
        NamespacedIdentifiers.from("retroapi", "spawn_egg");

    private static final Map<String, Item> REGISTERED = new LinkedHashMap<>();

    /** Every egg, in tab order. Empty until {@link #register()} has run. */
    public static List<Item> all() {
        return new ArrayList<>(REGISTERED.values());
    }

    /** The egg the creative tab shows on its own icon: modern's Spawn Eggs tab uses the creeper's. */
    public static Item icon() {
        return REGISTERED.get("creeper_spawn_egg");
    }

    /**
     * Called from RetroAPI's own init, next to the command blocks - after the item registration
     * events, so a mod's mob is registered by then and could be given an egg the same way.
     */
    public static void register() {
        if (!REGISTERED.isEmpty()) {
            return;
        }

        EGGS.forEach((name, entityId) -> registerFor(vanilla(name), entityId,
            NamespacedIdentifiers.from("retroapi", name)));
    }

    /**
     * One egg, for anyone building them: the vanilla set above, and the automatic ones
     * {@link RetroSpawnEggs} makes for a mod's own mobs.
     *
     * @param itemId   what to register the item as, e.g. {@code mymod:big_dog_spawn_egg}
     * @param entityId the vanilla {@code EntityRegistry} string this egg spawns
     * @param texture  the sprite, which may be {@link #DEFAULT_TEXTURE}
     */
    public static Item registerFor(final NamespacedIdentifier itemId, final String entityId,
                                   final NamespacedIdentifier texture) {
        final Item existing = REGISTERED.get(itemId.identifier());
        if (existing != null) {
            return existing;
        }

        final Item egg = RetroItemAccess.of(new SpawnEggItem(RetroItemAccess.AUTO_ID, entityId))
            .texture(texture)
            .register(itemId);
        REGISTERED.put(itemId.identifier(), egg);
        return egg;
    }

    private static NamespacedIdentifier vanilla(final String name) {
        return NamespacedIdentifiers.from("minecraft", name);
    }
}
