package com.periut.retroapi.commands.argument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The mod's own {@code minecraft:} entity registry: beta's twenty-four entity types under the names
 * modern Minecraft gives them.
 *
 * <p>Beta identifies entities by a CamelCase word - {@code Creeper}, {@code PrimedTnt} - and that word
 * is what goes on disk, in the {@code id} tag of every saved entity and in a spawner's
 * {@code EntityId}. Nothing here changes that: this table only maps the modern identifier a player
 * types onto the beta word the game stores, exactly as {@link VanillaIds} does for items.
 *
 * <p>The modern side was read out of 26.2's own {@code minecraft:entity_type} registry rather than
 * written from memory, so every name here is one the game really uses - with two deliberate exceptions,
 * both mobs modern renamed long after beta and whose current name says something about them that is not
 * true in b1.7.3:
 *
 * <ul>
 *   <li>{@code PigZombie} is {@code minecraft:zombie_pigman}, the id it carried from the 1.11 flattening
 *       until 1.16 made it the zombified piglin. In a beta world it is still a pigman, which is what the
 *       spawn egg is named on too.</li>
 *   <li>{@code Boat} is {@code minecraft:boat}, the id it carried until 1.21.2 split boats per wood type.
 *       Beta has exactly one boat and no wood variants at all, so calling it {@code minecraft:oak_boat}
 *       would claim a distinction the game cannot make.</li>
 * </ul>
 *
 * <p>Both modern names stay in {@link #ALIASES} and resolve; they are only not the ones offered.
 *
 * <p>{@code Mob} and {@code Monster} are the odd pair: they are beta's bare {@code LivingEntity} and
 * {@code MonsterEntity}, summonable here but with no modern counterpart at all, so their identifiers
 * are simply their own names lower-cased.
 */
public final class VanillaEntityIds {
    public static final String NAMESPACE = "minecraft";

    /** Modern identifier path -> the beta {@code EntityRegistry} id it names. */
    private static final Map<String, String> CANONICAL = new LinkedHashMap<>();
    /** Other spellings that resolve, but are not offered as completions. */
    private static final Map<String, String> ALIASES = new LinkedHashMap<>();
    /** The reverse of {@link #CANONICAL}, for naming an entity back to a player. */
    private static final Map<String, String> BY_BETA_ID = new LinkedHashMap<>();

    private VanillaEntityIds() {
    }

    static {
        entity("item", "Item");
        entity("painting", "Painting");
        entity("arrow", "Arrow");
        entity("snowball", "Snowball");

        entity("mob", "Mob");
        entity("monster", "Monster");

        entity("creeper", "Creeper");
        entity("skeleton", "Skeleton");
        entity("spider", "Spider");
        entity("giant", "Giant");
        entity("zombie", "Zombie");
        entity("slime", "Slime");
        entity("ghast", "Ghast");
        entity("zombie_pigman", "PigZombie");

        entity("pig", "Pig");
        entity("sheep", "Sheep");
        entity("cow", "Cow");
        entity("chicken", "Chicken");
        entity("squid", "Squid");
        entity("wolf", "Wolf");

        entity("tnt", "PrimedTnt");
        entity("falling_block", "FallingSand");
        entity("minecart", "Minecart");
        entity("boat", "Boat");

        // Beta's own spellings, and the modern ones that have since been renamed. A player who knows
        // this mob as a pigman should not have to learn what 1.16 called it to summon one.
        alias("pigman", "PigZombie");
        alias("zombified_piglin", "PigZombie");
        alias("primed_tnt", "PrimedTnt");
        alias("falling_sand", "FallingSand");
        alias("oak_boat", "Boat");
    }

    private static void entity(final String path, final String betaId) {
        CANONICAL.put(path, betaId);
        BY_BETA_ID.put(betaId, path);
    }

    private static void alias(final String path, final String betaId) {
        ALIASES.put(path, betaId);
    }

    /** The beta {@code EntityRegistry} id a modern path names, or null. Canonical names and aliases both. */
    public static String byName(final String path) {
        if (path == null) {
            return null;
        }
        final String lower = path.toLowerCase(Locale.ROOT);
        final String canonical = CANONICAL.get(lower);
        return canonical != null ? canonical : ALIASES.get(lower);
    }

    /** The modern path for a beta id - {@code PrimedTnt} to {@code tnt} - or null if it is not beta's. */
    public static String nameOf(final String betaId) {
        return betaId == null ? null : BY_BETA_ID.get(betaId);
    }

    /** Canonical paths only, in the order above; aliases are resolvable but never suggested. */
    public static List<String> names() {
        return Collections.unmodifiableList(new ArrayList<>(CANONICAL.keySet()));
    }
}
