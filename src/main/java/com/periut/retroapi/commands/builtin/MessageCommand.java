package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandManager;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.text.Formatting;
import com.periut.retroapi.text.Text;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;

import static com.periut.retroapi.commands.RetroCommandManager.argument;
import static com.periut.retroapi.commands.RetroCommandManager.literal;
import static com.periut.retroapi.commands.argument.EntityArgumentType.getPlayers;
import static com.periut.retroapi.commands.argument.EntityArgumentType.players;
import static com.periut.retroapi.commands.argument.MessageArgumentType.getMessage;
import static com.periut.retroapi.commands.argument.MessageArgumentType.message;

/** {@code /msg <targets> <message>}, aliased to {@code /tell} and {@code /w} as in modern. */
public final class MessageCommand {
    private MessageCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        final LiteralCommandNode<RetroCommandSource> node = dispatcher.register(literal("msg")
            .then(argument("targets", players())
                .then(argument("message", message())
                    .executes(context -> {
                        final RetroCommandSource source = context.getSource();
                        final List<PlayerEntity> targets = getPlayers(context, "targets");
                        final String text = getMessage(context, "message");

                        for (final PlayerEntity target : targets) {
                            target.sendMessage("§7" + source.getName() + " whispers to you: " + text);
                        }

                        source.sendFeedback(Text.literal("You whisper to "
                            + com.periut.retroapi.commands.CommandUtil.joinNames(targets) + ": " + text)
                            .formatted(Formatting.GRAY));
                        return Command.SINGLE_SUCCESS;
                    }))));

        RetroCommandManager.alias(dispatcher, node, "tell", "w");
    }
}
