package com.periut.retroapi.commands.argument;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * Turns what a player typed into a beta {@code EntityRegistry} id.
 *
 * <p>The counterpart of {@link ItemIds}, and the same division of labour: {@link VanillaEntityIds} is
 * RetroAPI's own table and always answers for the {@code minecraft} namespace, so identifiers work on
 * a plain b1.7.3 install; every other namespace is looked up in the vanilla registry, which is where
 * both RetroAPI and StationAPI put their entities ({@code RetroEntities.register} and StationAPI's
 * {@code EntityRegisterEvent} both write {@code idToClass} keyed by {@code identifier.toString()}).
 * One lookup therefore covers modded entities under either loader with no bridge in between.
 *
 * <p><b>What comes back is beta's own word</b> - {@code Creeper}, {@code PrimedTnt} - because that is
 * what {@code EntityRegistry.create} takes and what every save file already contains. The identifier
 * form is the input and the display, never the storage: a modded entity's id happens to be identical
 * in both because a mod's identifier <em>is</em> its registry key.
 */
public final class EntityIds {
    private EntityIds() {
    }

    /**
     * Resolves a token to the beta registry id it names, or null when nothing answers to it.
     *
     * <p>Accepted, in order: the registry key itself ({@code Creeper}, {@code mymod:moa}), a
     * {@code minecraft:}-namespaced or bare modern name ({@code minecraft:tnt}, {@code tnt}), and the
     * beta word in any casing ({@code creeper}), which is what the command accepted before identifiers
     * existed and so must keep accepting.
     */
    public static String resolve(final String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        if (EntityRegistry.idToClass.containsKey(token)) {
            return token;
        }

        final String lower = token.toLowerCase(Locale.ROOT);
        final int separator = lower.indexOf(':');
        final String namespace = separator < 0 ? VanillaEntityIds.NAMESPACE : lower.substring(0, separator);
        final String path = separator < 0 ? lower : lower.substring(separator + 1);

        if (VanillaEntityIds.NAMESPACE.equals(namespace)) {
            final String betaId = VanillaEntityIds.byName(path);
            // Only if it is actually registered: a mod could have removed one, and a name that
            // resolves to something the registry cannot make is not a name that resolves.
            if (betaId != null && EntityRegistry.idToClass.containsKey(betaId)) {
                return betaId;
            }
        }

        // Modded ids, and beta's CamelCase words typed in any casing. Matching the whole key means a
        // bare "moa" never reaches "mymod:moa" - the same rule ItemIds follows, where a namespace-less
        // name is a minecraft: name.
        for (final String candidate : EntityRegistry.idToClass.keySet()) {
            if (candidate.equalsIgnoreCase(token)) {
                return candidate;
            }
        }

        return null;
    }

    /**
     * The identifier to show a player for a beta registry id - {@code PrimedTnt} to
     * {@code minecraft:tnt}, {@code mymod:moa} to itself.
     */
    public static String identifierOf(final String betaId) {
        final String vanilla = VanillaEntityIds.nameOf(betaId);
        return vanilla == null ? betaId : VanillaEntityIds.NAMESPACE + ":" + vanilla;
    }

    public static Class<? extends Entity> classOf(final String betaId) {
        return EntityRegistry.idToClass.get(betaId);
    }

    /** Every identifier worth suggesting, namespaced as modern Minecraft writes them. */
    public static List<String> allIdentifiers() {
        final TreeSet<String> identifiers = new TreeSet<>();
        for (final String betaId : EntityRegistry.idToClass.keySet()) {
            identifiers.add(identifierOf(betaId));
        }
        return new ArrayList<>(identifiers);
    }
}
