package com.periut.retroapi.entity.spawnegg;

import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.entity.EntityRegistration;
import com.periut.retroapi.registry.RetroRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A spawn egg for every mob a mod registers, without the mod asking.
 *
 * <p>Registering a mob is the whole of it: {@code RetroEntities.register(id("BigDog"), BigDog.class)}
 * also gets {@code mymod:big_dog_spawn_egg}, named "Big Dog Spawn Egg", in the Spawn Eggs tab, spawning
 * exactly what the vanilla registry says that id spawns. Beta's entity names are CamelCase, so the item
 * name is that name split on its capitals - which is also how modern names its own.
 *
 * <p><b>The texture.</b> Looked for at {@code assets/<mod>/textures/item/<name>_spawn_egg.png}. A mod
 * that has not drawn one gets RetroAPI's plain egg and a line in the log saying exactly which file to
 * create - a working egg either way, and a warning rather than a missing texture.
 *
 * <p><b>Opting out.</b> Either one mob or the whole mod:
 *
 * <pre>{@code
 * RetroSpawnEggs.exclude(id("BigDog"));   // no egg for this mob
 * RetroSpawnEggs.excludeMod("mymod");     // no automatic eggs for anything of mine
 * }</pre>
 *
 * Call it before RetroAPI's own init finishes - a mod's {@code retroapi} entrypoint or its
 * {@code EntityRegistrationCallback} both run in time.
 *
 * <p><b>Not under StationAPI.</b> There StationAPI owns registration and its mods bring their own
 * items, so nothing is added automatically and a mod that wants an egg registers one itself.
 */
public final class RetroSpawnEggs {
    private RetroSpawnEggs() {
    }

    private static final Set<String> EXCLUDED_ENTITIES = new LinkedHashSet<>();
    private static final Set<String> EXCLUDED_MODS = new LinkedHashSet<>();

    /** No automatic egg for this one mob. */
    public static void exclude(final NamespacedIdentifier entityId) {
        if (entityId != null) {
            EXCLUDED_ENTITIES.add(entityId.toString());
        }
    }

    /** No automatic eggs for anything this mod registers. */
    public static void excludeMod(final String namespace) {
        if (namespace != null && !namespace.isEmpty()) {
            EXCLUDED_MODS.add(namespace);
        }
    }

    /**
     * Called from RetroAPI's own init, after the entity registration event - so every mod's mobs are
     * known, and so are the exclusions any of them asked for.
     */
    public static void registerAll() {
        if (FabricLoader.getInstance().isModLoaded("stationapi")) {
            return;
        }

        // Mod -> the mobs it got an egg for without having a texture, so the log says it once per mod
        // rather than once per mob.
        final Map<String, List<String>> missingTextures = new LinkedHashMap<>();

        for (final EntityRegistration registration : RetroRegistry.getEntities()) {
            final NamespacedIdentifier id = registration.getId();
            if (id == null || !registration.isLiving()) {
                continue;   // a projectile or a boat has nothing to hatch
            }
            if (EXCLUDED_MODS.contains(id.namespace()) || EXCLUDED_ENTITIES.contains(id.toString())) {
                continue;
            }

            final String eggName = snakeCase(id.identifier()) + "_spawn_egg";
            final NamespacedIdentifier texture = NamespacedIdentifiers.from(id.namespace(), eggName);
            final boolean hasTexture = textureExists(id.namespace(), eggName);

            SpawnEggs.registerFor(
                NamespacedIdentifiers.from(id.namespace(), eggName),
                id.toString(),
                hasTexture ? texture : SpawnEggs.DEFAULT_TEXTURE);

            if (!hasTexture) {
                missingTextures.computeIfAbsent(id.namespace(), key -> new ArrayList<>())
                    .add(id.identifier());
            }
        }

        missingTextures.forEach(RetroSpawnEggs::warnAboutTextures);
    }

    private static void warnAboutTextures(final String namespace, final List<String> entities) {
        final StringBuilder message = new StringBuilder();
        message.append("[RetroAPI] ").append(namespace)
            .append(" warning: Spawn egg has been automatically registered for your ")
            .append(entities.size() == 1 ? "entity" : "entities").append(": ")
            .append(String.join(", ", entities))
            .append(", please put a texture in");

        for (final String entity : entities) {
            message.append('\n').append("    assets/").append(namespace)
                .append("/textures/item/").append(snakeCase(entity)).append("_spawn_egg.png");
        }

        message.append('\n').append("OR opt out of spawn eggs with").append('\n');
        for (final String entity : entities) {
            message.append("    RetroSpawnEggs.exclude(NamespacedIdentifiers.from(\"")
                .append(namespace).append("\", \"").append(entity).append("\"));").append('\n');
        }
        message.append("    RetroSpawnEggs.excludeMod(\"").append(namespace)
            .append("\");   // or all of them at once");

        RetroAPI.LOGGER.warn(message.toString());
    }

    /** Whether the mod ships the texture RetroAPI would use, so the plain egg is only a fallback. */
    private static boolean textureExists(final String namespace, final String eggName) {
        final String path = "/assets/" + namespace + "/textures/item/" + eggName + ".png";
        return RetroSpawnEggs.class.getResource(path) != null;
    }

    /**
     * {@code BigDog} to {@code big_dog}, which is beta's CamelCase entity naming turned into modern's
     * identifier naming - the same conversion that makes "Big Dog Spawn Egg" out of the item id.
     *
     * <p>Runs of capitals stay together, so {@code TNTGolem} is {@code tnt_golem} rather than
     * {@code t_n_t_golem}, and a name already in snake_case comes back unchanged.
     */
    static String snakeCase(final String camelCase) {
        final StringBuilder out = new StringBuilder(camelCase.length() + 4);

        for (int i = 0; i < camelCase.length(); i++) {
            final char c = camelCase.charAt(i);
            if (c == '_' || c == ' ') {
                appendSeparator(out);
                continue;
            }

            final boolean startsWord = i > 0 && Character.isUpperCase(c)
                && (!Character.isUpperCase(camelCase.charAt(i - 1))
                    || (i + 1 < camelCase.length() && Character.isLowerCase(camelCase.charAt(i + 1))));
            if (startsWord) {
                appendSeparator(out);
            }
            out.append(Character.toLowerCase(c));
        }

        return out.toString();
    }

    private static void appendSeparator(final StringBuilder out) {
        if (out.length() > 0 && out.charAt(out.length() - 1) != '_') {
            out.append('_');
        }
    }
}
