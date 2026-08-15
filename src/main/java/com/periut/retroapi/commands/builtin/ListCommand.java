package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.CommandDispatcher;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.text.Text;

import java.util.List;

import static com.periut.retroapi.commands.RetroCommandManager.literal;

/** {@code /list} - who is online. */
public final class ListCommand {
    private ListCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("list")
            .executes(context -> {
                final RetroCommandSource source = context.getSource();
                final List<String> names = source.getPlayerNames();

                source.sendFeedback(Text.literal("There are " + names.size() + " players online:"));
                source.sendFeedback(Text.literal(String.join(", ", names)));
                return names.size();
            }));
    }
}
