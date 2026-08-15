package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.text.Text;
import com.periut.retroapi.commands.util.ServerUtil;

import static com.periut.retroapi.commands.RetroCommandManager.argument;
import static com.periut.retroapi.commands.RetroCommandManager.literal;
import static com.periut.retroapi.commands.argument.MessageArgumentType.getMessage;
import static com.periut.retroapi.commands.argument.MessageArgumentType.message;

/** {@code /me <action>} - available to everyone, as in modern. */
public final class MeCommand {
    private MeCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("me")
            .then(argument("action", message())
                .executes(context -> {
                    final RetroCommandSource source = context.getSource();
                    final String text = "* " + source.getName() + " " + getMessage(context, "action");

                    if (source.getServer() != null) {
                        ServerUtil.getConnectionManager().broadcast(text);
                    } else {
                        source.sendFeedback(Text.literal(text));
                    }
                    return Command.SINGLE_SUCCESS;
                })));
    }
}
