package com.periut.retroapi.commands.argument;

import com.periut.retroapi.text.Translations;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityRegistry;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Human-readable entity names, of the sort modern Minecraft shows in {@code Summoned new Creeper}.
 *
 * <p>The item counterpart is {@link ItemNames}, and this is the shorter version of the same chain
 * because beta has nothing to fall back on: its lang file contains no {@code entity.} keys at all, so
 * there is no vanilla translation to prefer. Two steps:
 *
 * <ol>
 *   <li>{@link #translationKey a translation}, which is what a player or mod author writes to name an
 *       entity themselves - see below;</li>
 *   <li>the identifier, spelled out: {@code zombie_pigman} becomes "Zombie Pigman", and a modded
 *       {@code mymod:big_dog} becomes "Big Dog". This step needs no lang file, so a dedicated server
 *       never comes up empty and no mod has to ship one to get a readable name.</li>
 * </ol>
 *
 * <p><b>Writing your own.</b> The key is modern Minecraft's, {@code entity.<namespace>.<path>}, built
 * from the identifier rather than from beta's CamelCase word - so a lang file line of
 * {@code entity.minecraft.zombie_pigman=Zombie Pigman} renames the vanilla mob and
 * {@code entity.mymod.big_dog=Rex} renames a modded one, with nothing to register either way. Mod lang
 * files are loaded by {@code LangLoader} from {@code assets/<modid>/lang/en_US.lang}.
 */
public final class EntityNames {
    private EntityNames() {
    }

    /**
     * The name to show for a beta {@code EntityRegistry} id, e.g. {@code PrimedTnt} to "TNT"'s
     * translation or, failing that, "Tnt".
     */
    public static String displayName(final String betaId) {
        if (betaId == null || betaId.isEmpty()) {
            return "Entity";
        }

        final String translated = Translations.find(translationKey(betaId));
        if (translated != null) {
            return translated;
        }
        return ItemNames.spellOut(pathOf(betaId));
    }

    /**
     * The name to show for one entity in the world. A player is named by their own name, as modern
     * does and as beta's own messages do; anything else is named by its type.
     */
    public static String displayName(final Entity entity) {
        if (entity == null) {
            return "Entity";
        }
        if (entity instanceof PlayerEntity player) {
            return player.name;
        }
        return displayName(EntityRegistry.getId(entity));
    }

    /**
     * The lang key that names an entity: {@code entity.minecraft.creeper}, {@code entity.mymod.big_dog}.
     *
     * <p>Modern's shape, built from the identifier - so the key stays put when beta's own word for the
     * mob does not match it ({@code PigZombie} is {@code entity.minecraft.zombie_pigman}).
     */
    public static String translationKey(final String betaId) {
        final String vanilla = VanillaEntityIds.nameOf(betaId);
        if (vanilla != null) {
            return "entity." + VanillaEntityIds.NAMESPACE + "." + vanilla;
        }
        // A modded id is already an identifier, so its namespace is the one to key on. Anything with
        // no namespace at all is a beta word this table does not know - a mod that wrote straight into
        // EntityRegistry - and minecraft: is the only sensible namespace left for it.
        final int separator = betaId.indexOf(':');
        return separator < 0
            ? "entity." + VanillaEntityIds.NAMESPACE + "." + betaId
            : "entity." + betaId.substring(0, separator) + "." + betaId.substring(separator + 1);
    }

    /** The identifier path a name is spelled out from - {@code mymod:big_dog} to {@code big_dog}. */
    private static String pathOf(final String betaId) {
        final String vanilla = VanillaEntityIds.nameOf(betaId);
        if (vanilla != null) {
            return vanilla;
        }
        final int separator = betaId.indexOf(':');
        return separator < 0 ? betaId : betaId.substring(separator + 1);
    }
}
