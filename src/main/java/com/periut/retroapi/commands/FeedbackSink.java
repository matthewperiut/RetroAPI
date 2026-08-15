package com.periut.retroapi.commands;

import com.periut.retroapi.text.Text;

/**
 * Where a command's output goes: a player's chat, the server console, or the local client's HUD.
 *
 * <p>Kept as an interface so {@link RetroCommandSource} says nothing about which side it is on -
 * the same command class runs in singleplayer, from a server player, and from the console.
 */
@FunctionalInterface
public interface FeedbackSink {
    void send(Text message);

    /** Discards everything - used by selectors that run a command for each of many sources. */
    FeedbackSink SILENT = message -> {
    };
}
