package com.periut.retroapi.commands.network;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.periut.retroapi.commands.RetroCommandsNetworking;
import com.periut.retroapi.text.TextCodec;
import net.ornithemc.osl.networking.api.PacketBuffer;
import net.ornithemc.osl.networking.api.client.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Asks the server what could come next, the way modern Minecraft's completion request does.
 *
 * <p>The server knows things the client cannot: who is online, what another mod registered, what
 * this player is allowed to do. Each request carries an id so a slow answer to an old keystroke is
 * recognised and dropped rather than replacing what the player is looking at now.
 */
public final class ClientSuggestions {
    private static final AtomicInteger NEXT_ID = new AtomicInteger();
    private static final Map<Integer, CompletableFuture<Suggestions>> PENDING = new ConcurrentHashMap<>();

    private ClientSuggestions() {
    }

    public static CompletableFuture<Suggestions> request(final String command, final int cursor) {
        if (!ClientPlayNetworking.isPlayReady(RetroCommandsNetworking.SUGGEST_CHANNEL)) {
            return Suggestions.empty();
        }

        final int id = NEXT_ID.incrementAndGet();
        final CompletableFuture<Suggestions> future = new CompletableFuture<>();
        PENDING.put(id, future);

        ClientPlayNetworking.send(RetroCommandsNetworking.SUGGEST_CHANNEL, buffer -> {
            buffer.writeVarInt(id);
            buffer.writeVarInt(cursor);
            buffer.writeString(command);
        });

        return future;
    }

    /** Called by the client listener when an answer arrives. */
    public static void onResponse(final PacketBuffer buffer) {
        final int id = buffer.readVarInt();
        final Suggestions suggestions = read(buffer);

        final CompletableFuture<Suggestions> future = PENDING.remove(id);
        if (future != null) {
            future.complete(suggestions);
        }
    }

    public static void clear() {
        PENDING.clear();
    }

    public static void write(final Suggestions suggestions, final PacketBuffer buffer) {
        buffer.writeVarInt(suggestions.getRange().getStart());
        buffer.writeVarInt(suggestions.getRange().getEnd());
        buffer.writeVarInt(suggestions.getList().size());

        for (final Suggestion suggestion : suggestions.getList()) {
            buffer.writeString(suggestion.getText());
            final Message tooltip = suggestion.getTooltip();
            buffer.writeBoolean(tooltip != null);
            if (tooltip != null) {
                buffer.writeString(TextCodec.toJson(com.periut.retroapi.text.Texts.of(tooltip)));
            }
        }
    }

    private static Suggestions read(final PacketBuffer buffer) {
        final StringRange range = StringRange.between(buffer.readVarInt(), buffer.readVarInt());
        final int count = buffer.readVarInt();

        final List<Suggestion> suggestions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final String text = buffer.readString();
            final Message tooltip = buffer.readBoolean() ? TextCodec.fromJson(buffer.readString()) : null;
            suggestions.add(new Suggestion(range, text, tooltip));
        }

        return new Suggestions(range, suggestions);
    }
}
