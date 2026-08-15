package com.periut.retroapi.commands.util;

import com.periut.retroapi.commands.RetroCommands;
import com.periut.retroapi.commands.RetroCommandsNetworking;
import com.periut.retroapi.commands.client.ClientCommands;
import com.periut.retroapi.commands.client.gui.RetroChatHud;
import com.periut.retroapi.commands.network.ClientSuggestions;
import com.periut.retroapi.commands.network.CommandTreeSerializer;
import com.periut.retroapi.text.TextCodec;
import com.periut.retroapi.text.Translations;
import net.minecraft.client.resource.language.I18n;
import net.ornithemc.osl.networking.api.client.ClientPlayNetworking;

import java.util.List;

/**
 * The client's command-channel listeners. Called from {@code RetroAPIClient}, so every reference to a
 * client-only class here is only ever loaded on a client.
 */
public final class NetworkingUtil {
    private NetworkingUtil() {
    }

    public static void registerClient() {
        // The game's translation table exists only on a client, and only this side may touch the
        // class that holds it. Beta echoes the key back for an unknown one, so map that to null -
        // Translations treats null as "ask the next source".
        Translations.setResolver(key -> {
            final String translated = I18n.getTranslation(key);
            return translated == null || translated.equals(key) ? null : translated;
        });

        ClientPlayNetworking.registerListener(RetroCommandsNetworking.OP_CHANNEL, (ctx, buffer) -> {
            ctx.ensureOnMainThread();
            RetroCommands.mp_op = buffer.readBoolean();
            RetroCommands.mp_rc = true;
        });

        ClientPlayNetworking.registerListener(RetroCommandsNetworking.PLAYERS_CHANNEL, (ctx, buffer) -> {
            ctx.ensureOnMainThread();
            RetroCommands.player_names = buffer.readString().split(",");
        });

        ClientPlayNetworking.registerListener(RetroCommandsNetworking.DISABLED_CHANNEL, (ctx, buffer) -> {
            ctx.ensureOnMainThread();
            RetroCommands.disabled_commands = List.of(buffer.readString().split(","));
        });

        // The server's command tree: what this player may run, described well enough to parse,
        // colour and complete locally.
        ClientPlayNetworking.registerListener(RetroCommandsNetworking.COMMANDS_CHANNEL, (ctx, buffer) -> {
            ctx.ensureOnMainThread();
            ClientCommands.setServerDispatcher(CommandTreeSerializer.read(buffer));
            RetroCommands.mp_rc = true;
        });

        ClientPlayNetworking.registerListener(RetroCommandsNetworking.SUGGEST_CHANNEL, (ctx, buffer) -> {
            ctx.ensureOnMainThread();
            ClientSuggestions.onResponse(buffer);
        });

        // Command output as a component, so hover text and click actions survive the trip.
        ClientPlayNetworking.registerListener(RetroCommandsNetworking.MESSAGE_CHANNEL, (ctx, buffer) -> {
            ctx.ensureOnMainThread();
            RetroChatHud.getInstance().addMessage(TextCodec.fromJson(buffer.readString()));
        });
    }
}
