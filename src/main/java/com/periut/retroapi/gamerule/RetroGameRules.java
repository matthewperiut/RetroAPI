package com.periut.retroapi.gamerule;

import com.periut.retroapi.storage.RetroData;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Game rules, backported.
 *
 * <p>Beta has none: whether fire spreads, whether mobs spawn and whether the sun moves are all
 * hard-coded. These are the modern rules that have a direct beta equivalent, plus two that beta
 * <em>cannot</em> have without help ({@code sprinting}, {@code swimming}, both off by default, both
 * implemented by RetroAPI's own movement code).
 *
 * <p>A mod adds its own the same way RetroAPI adds these:
 *
 * <pre>{@code
 * public static final RetroGameRule MY_RULE = RetroGameRules.registerBoolean("mymod:doSomething", true);
 *
 * if (RetroGameRules.getBoolean(MY_RULE)) { ... }
 * }</pre>
 *
 * <p>Register during your mod's init - before any world loads - so the rule exists by the time
 * {@code /gamerule} lists it and a saved value is read back.
 *
 * <p><b>Where the values live.</b> With the world, in {@link RetroData#world()}, so they travel with
 * the save and survive a restart. The server owns them and sends the whole set to every client on
 * join and again whenever one changes - a client asking for {@code sprinting} has to get the same
 * answer as the server or it would predict a move the server then rejects.
 */
public final class RetroGameRules {
    private RetroGameRules() {
    }

    /** Where the values sit inside the world data compound. */
    private static final String SECTION = "retroapi:gamerules";

    private static final Map<String, RetroGameRule> RULES = new LinkedHashMap<>();

    /**
     * The server's values, on a client that is connected to one. Null when this game owns its world
     * (singleplayer, or the server itself), in which case the world data is the truth.
     */
    private static Map<String, String> remote;

    // --- the vanilla-equivalent set -------------------------------------------------------------

    public static final RetroGameRule DO_FIRE_TICK = registerBoolean("doFireTick", true);
    public static final RetroGameRule DO_MOB_SPAWNING = registerBoolean("doMobSpawning", true);
    public static final RetroGameRule DO_MOB_LOOT = registerBoolean("doMobLoot", true);
    public static final RetroGameRule DO_TILE_DROPS = registerBoolean("doTileDrops", true);
    public static final RetroGameRule KEEP_INVENTORY = registerBoolean("keepInventory", false);
    public static final RetroGameRule MOB_GRIEFING = registerBoolean("mobGriefing", true);
    public static final RetroGameRule DO_DAYLIGHT_CYCLE = registerBoolean("doDaylightCycle", true);
    public static final RetroGameRule DO_WEATHER_CYCLE = registerBoolean("doWeatherCycle", true);
    public static final RetroGameRule DO_IMMEDIATE_RESPAWN = registerBoolean("doImmediateRespawn", false);
    public static final RetroGameRule COMMAND_BLOCK_OUTPUT = registerBoolean("commandBlockOutput", true);
    public static final RetroGameRule SEND_COMMAND_FEEDBACK = registerBoolean("sendCommandFeedback", true);
    /**
     * Scaled onto beta's own random-tick count rather than replacing it: 3 means "exactly what beta
     * always did", which is what modern's default of 3 means there too.
     */
    public static final RetroGameRule RANDOM_TICK_SPEED = registerInt("randomTickSpeed", 3);
    /** Modern's {@code maxCommandChainLength}: how far one command block chain may walk in a tick. */
    public static final RetroGameRule MAX_COMMAND_CHAIN_LENGTH = registerInt("maxCommandChainLength", 65536);
    /** Modern's {@code maxBlockModifications}: the most blocks one {@code /fill} may touch. */
    public static final RetroGameRule MAX_BLOCK_MODIFICATIONS = registerInt("maxBlockModifications", 32768);

    // --- rules beta has no equivalent of at all --------------------------------------------------

    /** Sprinting did not exist until 1.8; off unless a world asks for it. */
    public static final RetroGameRule SPRINTING = registerBoolean("sprinting", false);
    /** Swimming (as an animation and a movement mode) likewise. */
    public static final RetroGameRule SWIMMING = registerBoolean("swimming", false);
    /**
     * Whether the creative screen shows the Operator Utilities tab - command blocks, bedrock, the
     * things a world's owner hands out rather than a player finds.
     *
     * <p>Modern gates that tab on a client option plus a permission level. Beta has neither, so it is
     * a rule instead: the server owns it, it travels with the world, and it is off by default the same
     * way modern's option is. Turn it on with {@code /gamerule operatorItemsTab true}.
     */
    public static final RetroGameRule OPERATOR_ITEMS_TAB = registerBoolean("operatorItemsTab", false);

    // --- registration ---------------------------------------------------------------------------

    public static RetroGameRule registerBoolean(final String key, final boolean defaultValue) {
        return register(new RetroGameRule(key, RetroGameRule.Type.BOOLEAN, String.valueOf(defaultValue)));
    }

    public static RetroGameRule registerInt(final String key, final int defaultValue) {
        return register(new RetroGameRule(key, RetroGameRule.Type.INTEGER, String.valueOf(defaultValue)));
    }

    private static RetroGameRule register(final RetroGameRule rule) {
        final RetroGameRule existing = RULES.get(rule.getKey());
        if (existing != null) {
            return existing;
        }
        RULES.put(rule.getKey(), rule);
        return rule;
    }

    public static RetroGameRule get(final String key) {
        return RULES.get(key);
    }

    /** Every registered rule, in registration order. */
    public static Collection<RetroGameRule> all() {
        return RULES.values();
    }

    public static List<String> keys() {
        return new ArrayList<>(RULES.keySet());
    }

    // --- reading --------------------------------------------------------------------------------

    public static boolean getBoolean(final RetroGameRule rule) {
        final String value = raw(rule);
        return value == null ? rule.getDefaultBoolean() : Boolean.parseBoolean(value);
    }

    public static boolean getBoolean(final String key) {
        final RetroGameRule rule = RULES.get(key);
        return rule != null && getBoolean(rule);
    }

    public static int getInt(final RetroGameRule rule) {
        final String value = raw(rule);
        if (value == null) {
            return rule.getDefaultInt();
        }
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException ignored) {
            return rule.getDefaultInt();
        }
    }

    /** The value as text, exactly as {@code /gamerule <rule>} reports it. */
    public static String getString(final RetroGameRule rule) {
        final String value = raw(rule);
        return value == null ? rule.getDefaultValue() : value;
    }

    private static String raw(final RetroGameRule rule) {
        if (remote != null) {
            return remote.get(rule.getKey());
        }
        final NbtCompound values = section();
        return values.contains(rule.getKey()) ? values.getString(rule.getKey()) : null;
    }

    private static NbtCompound section() {
        final NbtCompound world = RetroData.world();
        final NbtCompound values = world.getCompound(SECTION);
        if (!world.contains(SECTION)) {
            world.put(SECTION, values);
        }
        return values;
    }

    // --- writing --------------------------------------------------------------------------------

    /**
     * Sets a rule and tells every connected client about it.
     *
     * @return false when the rule does not exist or the value is not one it can hold
     */
    public static boolean set(final String key, final String value) {
        final RetroGameRule rule = RULES.get(key);
        if (rule == null || !rule.accepts(value)) {
            return false;
        }

        // Normalised, so `/gamerule keepInventory TRUE` reads back as `true`.
        final String normalised = rule.getType() == RetroGameRule.Type.BOOLEAN
            ? String.valueOf(Boolean.parseBoolean(value))
            : String.valueOf(Integer.parseInt(value));

        if (remote != null) {
            // A client does not get to decide: whatever it was told last still stands until the
            // server says otherwise. (The command sends the change to the server instead.)
            return false;
        }

        section().putString(key, normalised);
        // Only a dedicated server has clients to tell; touching the sync class anywhere else would
        // load ServerPlayerEntity in an environment that may not have it.
        if (net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.SERVER) {
            GameRuleSync.broadcast(key, normalised);
        }
        return true;
    }

    // --- client sync ------------------------------------------------------------------------------

    /** Replaces everything a client believes with what the server just said. */
    public static void applyFromServer(final Map<String, String> values) {
        remote = new HashMap<>(values);
    }

    /** One rule changed on the server. */
    public static void applyFromServer(final String key, final String value) {
        if (remote == null) {
            remote = new HashMap<>();
        }
        remote.put(key, value);
    }

    /** Leaving a server: this game's own world data is the truth again. */
    public static void clearRemote() {
        remote = null;
    }

    /** Every rule and its current value, for sending to a client. */
    public static Map<String, String> snapshot() {
        final Map<String, String> values = new LinkedHashMap<>();
        for (final RetroGameRule rule : RULES.values()) {
            values.put(rule.getKey(), getString(rule));
        }
        return values;
    }
}
