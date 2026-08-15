package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.gamemode.RetroGameMode;
import com.periut.retroapi.gamemode.RetroGameModes;
import com.periut.retroapi.text.Text;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.periut.retroapi.commands.RetroCommandManager.argument;
import static com.periut.retroapi.commands.RetroCommandManager.literal;
import static com.periut.retroapi.commands.argument.EntityArgumentType.getPlayers;
import static com.periut.retroapi.commands.argument.EntityArgumentType.players;

/**
 * {@code /fly [targets] [true|false]} - who may fly, regardless of game mode.
 *
 * <p>This is modern's {@code mayfly} ability rather than a mode of its own, so a survival player given
 * it flies exactly as a creative one does - the same acceleration, friction and double-tap to take off -
 * and takes no fall damage, which is the rule modern applies to anyone who may fly. Taking it away puts
 * them straight back on survival's physics, creative included.
 *
 * <p>Spectator ignores it: a spectator has nothing to stand on and always flies.
 *
 * <p>The permission is permanent player data, so it survives death and reconnection, but changing game
 * mode resets it to that mode's default - creative and spectator fly, survival and adventure walk.
 */
public final class FlyCommand {
    private FlyCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("fly")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
            .executes(context -> toggleSelf(context.getSource()))
            .then(argument("targets", players())
                .executes(context -> set(context, getPlayers(context, "targets"), null))
                .then(argument("allowed", bool())
                    .executes(context -> set(context, getPlayers(context, "targets"), getBool(context, "allowed"))))));
    }

    private static int toggleSelf(final RetroCommandSource source) throws CommandSyntaxException {
        final PlayerEntity player = source.getPlayerOrThrow();
        return apply(source, List.of(player), null);
    }

    private static int set(final CommandContext<RetroCommandSource> context, final List<PlayerEntity> targets,
                           final Boolean allowed) {
        return apply(context.getSource(), targets, allowed);
    }

    /** @param allowed null to toggle each target individually */
    private static int apply(final RetroCommandSource source, final List<PlayerEntity> targets, final Boolean allowed) {
        int changed = 0;

        for (final PlayerEntity target : targets) {
            if (RetroGameModes.get(target.name) == RetroGameMode.SPECTATOR) {
                source.sendError(Text.literal(target.name + " is spectating and always flies"));
                continue;
            }

            final boolean next = allowed == null ? !RetroGameModes.mayFly(target.name) : allowed;
            // setMayFly tells the player's client itself, on the side that has one to tell.
            RetroGameModes.setMayFly(target.name, next);
            changed++;

            if (targets.size() > 1 || target != source.getPlayer()) {
                source.sendFeedback(Text.literal((next ? "Allowed " : "Disallowed ") + target.name + " to fly"));
            } else {
                source.sendFeedback(Text.literal("Flight " + (next ? "enabled" : "disabled")));
            }
        }

        return changed == 0 ? 0 : Command.SINGLE_SUCCESS;
    }
}
