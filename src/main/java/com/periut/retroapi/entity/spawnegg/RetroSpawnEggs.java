package com.periut.retroapi.entity.spawnegg;

import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.mixin.entity.EntityRegistryAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
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
 * create - a working egg either way, and a warning rather than a missing texture. That fallback egg is
 * tinted to the average of the mob's own render texture ({@link EggTint}), so a mod's eggs are still
 * told apart at a glance before anyone draws them.
 *
 * <p><b>Opting out.</b> Either one mob or the whole mod:
 *
 * <pre>{@code
 * RetroSpawnEggs.exclude(id("BigDog"));   // no egg for this mob
 * RetroSpawnEggs.excludeMod("mymod");     // no automatic eggs for anything of mine
 * }</pre>
 *
 * Call it before RetroAPI's own init finishes - a mod's {@code retroapi} entrypoint or its
 * {@code EntityRegistrationCallback} both run in time. A mod that does not depend on RetroAPI can ask
 * for the same thing reflectively through {@link com.periut.retroapi.compat.RetroOptional}.
 *
 * <p><b>Under StationAPI too.</b> This drives off vanilla's own {@code EntityRegistry}, which is the
 * map StationAPI populates as well, so a StationAPI mob gets an egg on the same terms as a RetroAPI
 * one without either mod knowing about the other. Anything already carrying an egg from elsewhere
 * simply ends up with two, which is harmless - the other mod may not always be installed.
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
        // Mod -> the mobs it got an egg for without having a texture, so the log says it once per mod
        // rather than once per mob.
        final Map<String, List<String>> missingTextures = new LinkedHashMap<>();

        // Copied because registering an egg registers an item, and we would rather not reason about
        // whether anything downstream can reach back into the entity registry mid-iteration.
        final Map<String, Class<? extends Entity>> registered =
            new LinkedHashMap<>(EntityRegistryAccessor.retroapi$getIdToClass());

        for (final Map.Entry<String, Class<? extends Entity>> entry : registered.entrySet()) {
            final String stringId = entry.getKey();
            final int colon = stringId.indexOf(':');
            if (colon < 0) {
                continue;   // vanilla's own CamelCase names already have hand-drawn eggs
            }

            final Class<? extends Entity> entityClass = entry.getValue();
            if (entityClass == null || !LivingEntity.class.isAssignableFrom(entityClass)) {
                continue;   // a projectile or a boat has nothing to hatch
            }

            final String namespace = stringId.substring(0, colon);
            final String path = stringId.substring(colon + 1);
            if (EXCLUDED_MODS.contains(namespace) || EXCLUDED_ENTITIES.contains(stringId)) {
                continue;
            }

            final String eggName = snakeCase(path) + "_spawn_egg";
            final NamespacedIdentifier texture = NamespacedIdentifiers.from(namespace, eggName);
            final boolean hasTexture = textureExists(namespace, eggName);

            // Only the fallback egg is tinted; a mod that drew its own gets it untouched.
            final int tint = hasTexture ? SpawnEggs.NO_TINT : EggTint.forEntityClass(entityClass);
            if (tint != SpawnEggs.NO_TINT) {
                RetroAPI.LOGGER.debug("Tinted {}'s fallback spawn egg #{} from its own texture",
                    stringId, String.format("%06X", tint));
            }

            SpawnEggs.registerFor(
                NamespacedIdentifiers.from(namespace, eggName),
                stringId,
                hasTexture ? texture : SpawnEggs.DEFAULT_TEXTURE,
                tint);

            if (!hasTexture) {
                missingTextures.computeIfAbsent(namespace, key -> new ArrayList<>()).add(path);
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
