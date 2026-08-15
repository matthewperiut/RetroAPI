package com.periut.retroapi.commands.api;

import com.mojang.brigadier.CommandDispatcher;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.commands.RegistrationEnvironment;

import java.util.ArrayList;
import java.util.List;

/**
 * How another mod adds commands.
 *
 * <p>Register a callback during your mod's initialisation and it runs every time the command tree is
 * built, which is once per world on the client and once per server start:
 *
 * <pre>{@code
 * CommandRegistrationCallback.register((dispatcher, environment) ->
 *     dispatcher.register(literal("mycommand")
 *         .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_OWNER))
 *         .then(argument("target", players())
 *             .executes(context -> { ... return Command.SINGLE_SUCCESS; }))));
 * }</pre>
 *
 * <p>The dispatcher, the builders and the argument types are Brigadier's own, so code written
 * against modern Minecraft's command API compiles here unchanged.
 */
@FunctionalInterface
public interface CommandRegistrationCallback {
    void register(CommandDispatcher<RetroCommandSource> dispatcher, RegistrationEnvironment environment);

    /** Callbacks in registration order; the tree merges same-named nodes, so order rarely matters. */
    List<CommandRegistrationCallback> CALLBACKS = new ArrayList<>();

    static void register(final CommandRegistrationCallback callback) {
        CALLBACKS.add(callback);
    }
}
