package com.periut.retroapi.commands.network;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import com.periut.retroapi.commands.RetroCommandsNetworking;
import com.periut.retroapi.commands.RetroCommandManager;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.commands.ServerCommandSources;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.ornithemc.osl.networking.api.server.ServerPlayNetworking;

/**
 * The server half of command syncing: it hands each client a tree it may use, and answers the
 * completion requests that tree cannot satisfy on its own.
 */
public final class ServerCommandNetworking {
    private ServerCommandNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerListener(RetroCommandsNetworking.SUGGEST_CHANNEL, (context, buffer) -> {
            final int id = buffer.readVarInt();
            final int cursor = buffer.readVarInt();
            final String command = buffer.readString();

            context.ensureOnMainThread();
            respond(context.player(), id, command, cursor);
        });
    }

    private static void respond(final ServerPlayerEntity player, final int id, final String command, final int cursor) {
        final RetroCommandManager manager = RetroCommandManager.getInstance();
        if (manager == null) {
            return;
        }

        final RetroCommandSource source = ServerCommandSources.forPlayer(player);

        // The client sends what it typed, slash and all, and expects ranges it can apply to that
        // same string - so skip the slash rather than removing it, leaving every index aligned.
        final StringReader reader = new StringReader(command);
        if (reader.canRead() && reader.peek() == '/') {
            reader.skip();
        }

        final ParseResults<RetroCommandSource> parse = manager.getDispatcher().parse(reader, source);
        final int clamped = Math.max(0, Math.min(cursor, command.length()));

        // Completions are computed against the player's own permissions, so the answer never
        // reveals a command they could not run.
        final Suggestions suggestions = manager.getDispatcher().getCompletionSuggestions(parse, clamped).join();

        ServerPlayNetworking.send(player, RetroCommandsNetworking.SUGGEST_CHANNEL, buffer -> {
            buffer.writeVarInt(id);
            ClientSuggestions.write(suggestions, buffer);
        });
    }

    /** Sends the player the part of the tree they are allowed to see. */
    public static void sendTree(final ServerPlayerEntity player) {
        final RetroCommandManager manager = RetroCommandManager.getInstance();
        if (manager == null) {
            return;
        }

        // Sent unconditionally, exactly as the op-status and disabled-command packets beside it are.
        // Asking isPlayReady here is a race the tree loses: this runs at the tail of addPlayer, before
        // the channel handshake has necessarily finished, and a "not ready" answer meant the tree was
        // skipped and never sent again - leaving the client parsing against its own guess of a tree
        // while the server happily answered its completion requests from the real one.

        final RetroCommandSource source = ServerCommandSources.forPlayer(player);
        ServerPlayNetworking.send(player, RetroCommandsNetworking.COMMANDS_CHANNEL,
            buffer -> CommandTreeSerializer.write(manager.makeTreeForSource(source), buffer));
    }
}
