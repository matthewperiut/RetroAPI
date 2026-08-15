package com.periut.retroapi.commands.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import com.periut.retroapi.commands.RetroCommands;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandManager;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.commands.network.ClientSuggestions;
import com.periut.retroapi.text.Text;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.util.concurrent.CompletableFuture;

/**
 * The client's view of the command system.
 *
 * <p>Which dispatcher answers depends on where the player is. In singleplayer the client owns the
 * only dispatcher there is and runs commands against its own world - beta has no integrated server
 * to hand them to. On a server running this mod, the tree the server sent is used for parsing,
 * colouring and completions, and the command itself is sent on to be executed there. On a vanilla
 * server the local tree is used, which at least colours the syntax of the commands beta understands.
 */
public final class ClientCommands {
    private static RetroCommandManager local;
    private static RetroCommandSource source;
    private static CommandDispatcher<RetroCommandSource> serverDispatcher;

    private ClientCommands() {
    }

    /** Builds the client tree; called when a world is loaded, because commands act on that world. */
    public static void onWorldLoad() {
        local = new RetroCommandManager(RegistrationEnvironment.INTEGRATED);
        RetroCommandManager.setInstance(local);
        source = null;
    }

    public static void onDisconnect() {
        serverDispatcher = null;
        source = null;
    }

    /** Replaces the parsing tree with the one a server sent. */
    public static void setServerDispatcher(final CommandDispatcher<RetroCommandSource> dispatcher) {
        serverDispatcher = dispatcher;
    }

    public static CommandDispatcher<RetroCommandSource> getDispatcher() {
        if (serverDispatcher != null) {
            return serverDispatcher;
        }
        ensureLocal();
        return local.getDispatcher();
    }

    /**
     * The source used for parsing and completions.
     *
     * <p>Rebuilt on demand rather than cached, because it carries the player's position and a
     * selector like {@code @e[distance=..5]} has to be judged against where they are now.
     */
    public static RetroCommandSource getSource() {
        final Minecraft minecraft = minecraft();
        if (minecraft == null) {
            return source;
        }
        source = ClientCommandSources.create(minecraft);
        return source;
    }

    public static ParseResults<RetroCommandSource> parse(final StringReader reader) {
        return getDispatcher().parse(reader, getSource());
    }

    /**
     * @param cursor the cursor position within the command, excluding its leading slash
     */
    public static CompletableFuture<Suggestions> suggest(final ParseResults<RetroCommandSource> parse, final int cursor) {
        return suggest(parse, cursor, null);
    }

    /**
     * Completions, answered at once and improved later.
     *
     * <p>What the tree in hand can offer comes back immediately - the tree a server sent is already
     * filtered to what this player may run, and the argument types suggest from the client's own
     * registries - so the window opens on the keystroke rather than a round trip later. That wait was
     * the whole of the sluggishness: the answer was almost always the same one the client could have
     * produced without asking.
     *
     * <p>The server is still asked, because it knows things the client cannot - who is online, what
     * another mod added, what a selector matches right now - and when its answer arrives with something
     * in it, {@code refined} is handed the better list to swap in. An answer that never comes, or comes
     * back empty, simply leaves the instant one standing.
     *
     * @param refined called on the main thread when the server improves on the local answer, or never
     */
    public static CompletableFuture<Suggestions> suggest(final ParseResults<RetroCommandSource> parse, final int cursor,
                                                         final java.util.function.Consumer<Suggestions> refined) {
        final CompletableFuture<Suggestions> local = getDispatcher().getCompletionSuggestions(parse, cursor);

        if (RetroCommands.mp_rc && isRemote()) {
            ClientSuggestions.request(parse.getReader().getString(), cursor).thenAccept(remote -> {
                if (refined != null && remote != null && !remote.isEmpty()) {
                    refined.accept(remote);
                }
            });
        }

        return local;
    }

    /** Runs a command locally. Only correct in singleplayer; a server executes its own. */
    public static void execute(final String command) {
        ensureLocal();
        local.execute(getSource(), command);
    }

    public static boolean isRemote() {
        final Minecraft minecraft = minecraft();
        return minecraft != null && minecraft.world != null && minecraft.world.isRemote;
    }

    public static Text describeUnavailable() {
        return Text.literal("Commands are not available yet");
    }

    private static void ensureLocal() {
        if (local == null) {
            onWorldLoad();
        }
    }

    private static Minecraft minecraft() {
        return (Minecraft) FabricLoader.getInstance().getGameInstance();
    }
}
