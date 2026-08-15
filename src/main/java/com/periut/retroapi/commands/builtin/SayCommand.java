package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.text.Text;
import com.periut.retroapi.commands.util.ServerUtil;

import static com.periut.retroapi.commands.RetroCommandManager.argument;
import static com.periut.retroapi.commands.RetroCommandManager.literal;
import static com.periut.retroapi.commands.argument.MessageArgumentType.getMessage;
import static com.periut.retroapi.commands.argument.MessageArgumentType.message;

/** {@code /say <message>} - broadcast, with selectors in the message resolved to names. */
public final class SayCommand {
    private SayCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("say")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
            .then(argument("message", message())
                .executes(context -> {
                    final RetroCommandSource source = context.getSource();
                    final String text = "[" + source.getName() + "] " + getMessage(context, "message");

                    if (source.getServer() != null) {
                        ServerUtil.getConnectionManager().broadcast(text);
                    } else {
                        source.sendFeedback(Text.literal(text));
                    }
                    return Command.SINGLE_SUCCESS;
                })));
    }
}
