package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.periut.retroapi.commands.CommandUtil;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.gamemode.RetroGameMode;
import com.periut.retroapi.gamemode.RetroGameModes;
import com.periut.retroapi.text.Text;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Collections;
import java.util.List;

import static com.periut.retroapi.commands.RetroCommandManager.argument;
import static com.periut.retroapi.commands.RetroCommandManager.literal;
import static com.periut.retroapi.commands.argument.EntityArgumentType.getPlayers;
import static com.periut.retroapi.commands.argument.EntityArgumentType.players;
import static com.periut.retroapi.commands.argument.GameModeArgumentType.gameMode;
import static com.periut.retroapi.commands.argument.GameModeArgumentType.getGameMode;

/**
 * {@code /gamemode <mode> [targets]}, in modern's shape and with all four of modern's modes.
 *
 * <p>This is the only way into creative, and deliberately so: beta has no creative button, no world
 * -creation switch and no hint that any of this exists, exactly as modern hides command blocks
 * behind {@code /give}.
 */
public final class GameModeCommand {
    private GameModeCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("gamemode")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
            .then(argument("gamemode", gameMode())
                .executes(context -> set(context, Collections.singletonList(context.getSource().getPlayerOrThrow())))
                .then(argument("targets", players())
                    .executes(context -> set(context, getPlayers(context, "targets"))))));
    }

    private static int set(final CommandContext<RetroCommandSource> context, final List<PlayerEntity> targets) throws CommandSyntaxException {
        final RetroGameMode mode = getGameMode(context, "gamemode");

        for (final PlayerEntity target : targets) {
            RetroGameModes.set(target.name, mode);
        }

        context.getSource().sendFeedback(Text.literal("Set " + CommandUtil.joinNames(targets)
            + " to " + mode.getName() + " mode"));
        return Command.SINGLE_SUCCESS;
    }
}
