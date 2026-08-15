package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.periut.retroapi.commands.client.gui.RetroChatHud;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.text.Text;

import static com.periut.retroapi.commands.RetroCommandManager.literal;

/**
 * {@code /clearchat} - what {@code /clear} used to do here before modern's inventory-clearing
 * {@code /clear} took the name.
 */
public final class ClearChatCommand {
    private ClearChatCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        // Chat only exists on a client; on a server there is nothing to clear.
        if (environment.isDedicated()) {
            return;
        }

        dispatcher.register(literal("clearchat")
            .executes(context -> {
                RetroChatHud.getInstance().clear();
                context.getSource().sendFeedback(Text.literal("Cleared chat"));
                return Command.SINGLE_SUCCESS;
            }));
    }
}
