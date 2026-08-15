package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.periut.retroapi.commands.CommandUtil;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.text.Text;
import net.minecraft.entity.Entity;

import java.util.Collections;
import java.util.List;

import static com.periut.retroapi.commands.RetroCommandManager.argument;
import static com.periut.retroapi.commands.RetroCommandManager.literal;
import static com.periut.retroapi.commands.argument.EntityArgumentType.entities;
import static com.periut.retroapi.commands.argument.EntityArgumentType.getEntities;

/**
 * {@code /kill [targets]}, defaulting to the caller as modern does.
 *
 * <p>The old {@code /killall} is kept as a separate command rather than dropped, because
 * {@code /kill @e[type=!player]} - the modern way to say it - is a lot to type for something beta
 * players reach for often.
 */
public final class KillCommand {
    private static final int LETHAL_DAMAGE = 1000;

    private KillCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("kill")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
            .executes(context -> kill(context, Collections.singletonList(context.getSource().getEntityOrThrow())))
            .then(argument("targets", entities())
                .executes(context -> kill(context, getEntities(context, "targets")))));

        dispatcher.register(literal("killall")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
            .executes(KillCommand::killAll));
    }

    private static int kill(final CommandContext<RetroCommandSource> context, final List<? extends Entity> targets) throws CommandSyntaxException {
        for (final Entity target : targets) {
            target.damage(null, LETHAL_DAMAGE);
        }

        final RetroCommandSource source = context.getSource();
        if (targets.size() == 1) {
            source.sendFeedback(Text.literal("Killed " + CommandUtil.joinNames(targets)));
        } else {
            source.sendFeedback(Text.literal("Killed " + targets.size() + " entities"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int killAll(final CommandContext<RetroCommandSource> context) throws CommandSyntaxException {
        final RetroCommandSource source = context.getSource();
        final List<Entity> entities = source.getWorldEntities();

        int killed = 0;
        for (final Entity entity : entities) {
            // Iterating a copy, so removing the caller's own entity mid-loop is not a concern - but
            // killing the player who typed it would be a surprise.
            if (entity == source.getEntity()) {
                continue;
            }
            entity.damage(null, LETHAL_DAMAGE);
            killed++;
        }

        source.sendFeedback(Text.literal("Killed " + killed + " entities"));
        return Command.SINGLE_SUCCESS;
    }
}
