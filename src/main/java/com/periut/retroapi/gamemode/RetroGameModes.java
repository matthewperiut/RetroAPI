package com.periut.retroapi.gamemode;

import com.periut.retroapi.storage.RetroData;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Who is in which game mode, and who is currently flying.
 *
 * <p>The mode is <b>permanent player data</b>: it is stored per player in the world save and comes
 * back after death, a dimension change or a reconnect, exactly like modern. Flight is <b>one-life</b>
 * data: it survives leaving and rejoining, so a creative player comes back in the air they left in,
 * but a death clears it along with the rest of that life's state.
 *
 * <p>On a client connected to a server, both come from the server: it sends every player's mode so
 * the client can decide what to render (a spectator is invisible) and how its own player moves.
 */
public final class RetroGameModes {
    private RetroGameModes() {
    }

    /** Key inside the permanent player data. */
    private static final String KEY = "retroapi:gamemode";

    /** Key inside the ONE-LIFE player data: flight survives a rejoin, not a death. */
    private static final String KEY_FLYING = "retroapi:flying";

    /**
     * Key inside the PERMANENT player data: whether this player may fly at all - modern's
     * {@code Abilities.mayfly}, which {@code /fly} sets and a game-mode change resets.
     */
    private static final String KEY_MAY_FLY = "retroapi:mayFly";

    /** Server-sent modes on a client. Null when this game owns its world. */
    private static Map<String, RetroGameMode> remote;

    /** In-memory copy of who is airborne, filled from the world's life data on first ask. */
    private static final Map<String, Boolean> FLYING = new HashMap<>();

    /** Server-sent "may fly" on a client, alongside {@link #remote}. */
    private static Map<String, Boolean> remoteMayFly;

    public static RetroGameMode get(final PlayerEntity player) {
        return player == null ? RetroGameMode.SURVIVAL : get(player.name);
    }

    public static RetroGameMode get(final String playerName) {
        if (playerName == null) {
            return RetroGameMode.SURVIVAL;
        }
        if (remote != null) {
            return remote.getOrDefault(playerName, RetroGameMode.SURVIVAL);
        }
        final String stored = RetroData.player(playerName).getString(KEY);
        final RetroGameMode mode = RetroGameMode.byName(stored);
        return mode == null ? RetroGameMode.SURVIVAL : mode;
    }

    public static boolean is(final PlayerEntity player, final RetroGameMode mode) {
        return get(player) == mode;
    }

    /**
     * Changes a player's mode and tells every client about it.
     *
     * <p>Leaving a flying mode also puts them back on their feet: staying airborne in survival is
     * the one state that would silently break physics until the player noticed they were falling.
     */
    public static void set(final String playerName, final RetroGameMode mode) {
        RetroData.player(playerName).putString(KEY, mode.getName());

        if (mode != RetroGameMode.CREATIVE && mode != RetroGameMode.SPECTATOR) {
            FLYING.remove(playerName);
        }
        // A mode change resets the flight PERMISSION to that mode's default, so switching to survival
        // takes the wings away and switching to creative hands them back without anyone typing /fly.
        RetroData.player(playerName).putBoolean(KEY_MAY_FLY, mode.allowsFlightByDefault());
        if (!mode.allowsFlightByDefault()) {
            FLYING.remove(playerName);
            RetroData.life(playerName).putBoolean(KEY_FLYING, false);
        }

        if (mode != RetroGameMode.SPECTATOR) {
            // The wheel's throttle belongs to spectator; leaving it puts the speed back to 1x rather
            // than saving it up for the next time they spectate.
            com.periut.retroapi.commands.builtin.NoclipCommand.resetSpeed(playerName);
        }
        if (mode == RetroGameMode.SPECTATOR) {
            FLYING.put(playerName, true);
        }

        if (net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType()
                == net.fabricmc.api.EnvType.SERVER) {
            GameModeSync.broadcast(playerName, mode);
        }
        // And the flight state that just changed with it. The mode broadcast alone left a client that
        // was airborne in creative still airborne in survival: it heard about the mode and nothing
        // about the wings, and its own physics is what holds it up.
        notifyFlight(playerName);
    }

    // --- flight -----------------------------------------------------------------------------------

    /**
     * Whether this player may fly at all - modern's {@code mayfly}.
     *
     * <p>Permanent data, so it outlives a death and a reconnect, and it is what {@code /fly} changes.
     * A spectator always may, whatever is stored; every other mode starts at its own default and stays
     * wherever {@code /fly} last put it.
     */
    public static boolean mayFly(final String playerName) {
        if (playerName == null) {
            return false;
        }
        if (get(playerName) == RetroGameMode.SPECTATOR) {
            return true;
        }
        if (remoteMayFly != null) {
            return Boolean.TRUE.equals(remoteMayFly.get(playerName));
        }

        // Absent means "nobody has said either way", which is the mode's own default - so a world that
        // predates /fly, or a player who has never been given it, behaves exactly as before.
        final net.minecraft.nbt.NbtCompound data = RetroData.player(playerName);
        return data.contains(KEY_MAY_FLY)
            ? data.getBoolean(KEY_MAY_FLY)
            : get(playerName).allowsFlightByDefault();
    }

    public static boolean mayFly(final PlayerEntity player) {
        return player != null && mayFly(player.name);
    }

    /** {@code /fly}: hands the permission out or takes it back, and lands them if it is taken back. */
    public static void setMayFly(final String playerName, final boolean mayFly) {
        RetroData.player(playerName).putBoolean(KEY_MAY_FLY, mayFly);
        if (!mayFly) {
            FLYING.remove(playerName);
            RetroData.life(playerName).putBoolean(KEY_FLYING, false);
        }
        notifyFlight(playerName);
    }

    /**
     * Tells a player's client about their flight state, on a server.
     *
     * <p>Behind the environment check for the same reason {@code broadcast} is: GameModeSync names
     * ServerPlayerEntity, which a client cannot even load, so the reference must not be reached unless
     * this really is a server. Putting it here rather than at each caller means {@code /fly} - common
     * code that both sides run - never mentions the class at all.
     */
    private static void notifyFlight(final String playerName) {
        if (net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType()
                == net.fabricmc.api.EnvType.SERVER) {
            GameModeSync.sendFlight(playerName);
        }
    }

    /** A client being told both halves of its own flight state. */
    public static void applyFlightFromServer(final String playerName, final boolean mayFly, final boolean flying) {
        if (remoteMayFly == null) {
            remoteMayFly = new HashMap<>();
        }
        remoteMayFly.put(playerName, mayFly);
        FLYING.put(playerName, flying);
    }

    /** A spectator always flies; a creative player flies when they have turned it on. */
    public static boolean isFlying(final String playerName) {
        if (get(playerName) == RetroGameMode.SPECTATOR) {
            return true;
        }
        if (!mayFly(playerName)) {
            return false;
        }

        final Boolean known = FLYING.get(playerName);
        if (known != null) {
            return known;
        }
        if (remote != null) {
            return false;   // a client asks the server, not its own copy of the world
        }

        // Nothing in memory: this is a rejoin, so read what the player was doing when they left.
        final boolean stored = RetroData.life(playerName).getBoolean(KEY_FLYING);
        FLYING.put(playerName, stored);
        return stored;
    }

    public static void setFlying(final String playerName, final boolean flying) {
        if (!mayFly(playerName)) {
            return;
        }
        FLYING.put(playerName, flying);
        notifyFlight(playerName);
        if (remote == null) {
            // Whoever owns the world writes it down. LIFE data, not permanent player data: coming back
            // after a disconnect still flying is convenient, coming back from a DEATH still flying is
            // not - and life data is cleared on death for exactly that kind of state.
            RetroData.life(playerName).putBoolean(KEY_FLYING, flying);
        }
    }

    public static boolean toggleFlying(final String playerName) {
        final boolean next = !isFlying(playerName);
        setFlying(playerName, next);
        return isFlying(playerName);
    }

    // --- client sync --------------------------------------------------------------------------------

    public static void applyFromServer(final Map<String, RetroGameMode> modes) {
        remote = new HashMap<>(modes);
    }

    public static void applyFromServer(final String playerName, final RetroGameMode mode) {
        if (remote == null) {
            remote = new HashMap<>();
        }
        final RetroGameMode previous = remote.put(playerName, mode);

        // A mode that does not fly by default drops the cached wings the moment it is announced, rather
        // than waiting for the flight packet a tick behind it. If /fly says otherwise that packet says
        // so, and puts them straight back.
        if (previous != mode && !mode.allowsFlightByDefault()) {
            FLYING.remove(playerName);
            if (remoteMayFly != null) {
                remoteMayFly.remove(playerName);
            }
        }
    }

    /** Leaving a server: this game's own world data is the truth again. */
    public static void clearRemote() {
        remote = null;
        remoteMayFly = null;
        FLYING.clear();
    }

    public static Map<String, RetroGameMode> snapshot(final Iterable<String> playerNames) {
        final Map<String, RetroGameMode> modes = new HashMap<>();
        for (final String name : playerNames) {
            modes.put(name, get(name));
        }
        return modes;
    }
}
