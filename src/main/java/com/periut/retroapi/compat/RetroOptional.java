package com.periut.retroapi.compat;

import com.periut.retroapi.itemgroup.AutoItemGroups;
import com.periut.retroapi.entity.spawnegg.RetroSpawnEggs;
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers;

/**
 * The bits of RetroAPI a mod can drive without depending on RetroAPI.
 *
 * <p>RetroAPI gives a mod a creative tab and spawn eggs whether or not that mod asked, which is the
 * point - but a mod that wants to say something about it should not have to take a hard dependency to
 * do so, or it stops being optional. So everything here takes and returns nothing but {@code String}
 * and {@code void}: no RetroAPI type appears in any signature, which is what makes the caller's
 * reflection three lines instead of a page of {@code Class.forName} on types it cannot name.
 *
 * <pre>{@code
 * if (FabricLoader.getInstance().isModLoaded("retroapi")) {
 *     Class.forName("com.periut.retroapi.compat.RetroOptional")
 *         .getMethod("excludeItemGroup", String.class)
 *         .invoke(null, "mymod");
 * }
 * }</pre>
 *
 * <p>Or copy {@code RetroCompat.java} out of RetroAPI's {@code templates} resource folder, which wraps
 * all of this including the mod check and the exception handling.
 *
 * <p><b>Timing.</b> Call from a {@code stationapi:event_bus} listener, a {@code retroapi} entrypoint,
 * or anywhere else that runs before RetroAPI's init finishes. The automatic passes read these
 * exclusions at the end of init, so anything registered by then is in time.
 */
public final class RetroOptional {
    private RetroOptional() {
    }

    /** No automatic creative tab for this mod. Registering a tab of your own does this implicitly. */
    public static void excludeItemGroup(final String namespace) {
        AutoItemGroups.exclude(namespace);
    }

    /** No automatic spawn eggs for anything this mod registers. */
    public static void excludeSpawnEggs(final String namespace) {
        RetroSpawnEggs.excludeMod(namespace);
    }

    /**
     * No automatic spawn egg for one mob.
     *
     * @param entityPath the entity's own name as registered, e.g. {@code "ShadowWolf"} - not snake_case
     */
    public static void excludeSpawnEgg(final String namespace, final String entityPath) {
        RetroSpawnEggs.exclude(NamespacedIdentifiers.from(namespace, entityPath));
    }

    /**
     * Whether RetroAPI is the one that will be doing the above. Always {@code true} when this class
     * loads at all - it exists so a caller can probe for the facade without guessing at a method
     * signature, and so a reflective wrapper has something harmless to warm up on.
     */
    public static boolean isAvailable() {
        return true;
    }
}
