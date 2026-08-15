package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.text.Text;
import net.minecraft.world.World;

import static com.periut.retroapi.commands.RetroCommandManager.argument;
import static com.periut.retroapi.commands.RetroCommandManager.literal;
import static com.periut.retroapi.commands.argument.TimeArgumentType.getTime;
import static com.periut.retroapi.commands.argument.TimeArgumentType.time;

/** {@code /time set|add|query}, with modern's named times and unit suffixes. */
public final class TimeCommand {
    private static final int DAY_LENGTH = 24000;

    private TimeCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("time")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
            .then(literal("set")
                .then(literal("day").executes(context -> set(context, 1000)))
                .then(literal("noon").executes(context -> set(context, 6000)))
                .then(literal("night").executes(context -> set(context, 13000)))
                .then(literal("midnight").executes(context -> set(context, 18000)))
                .then(argument("time", time()).executes(context -> set(context, getTime(context, "time")))))
            .then(literal("add")
                .then(argument("time", time()).executes(context -> add(context, getTime(context, "time")))))
            .then(literal("query")
                .then(literal("daytime").executes(context -> query(context, timeOfDay(world(context)))))
                .then(literal("gametime").executes(context -> query(context, (int) (world(context).getTime() % Integer.MAX_VALUE))))
                .then(literal("day").executes(context -> query(context, (int) (world(context).getTime() / DAY_LENGTH))))));
    }

    private static int set(final CommandContext<RetroCommandSource> context, final int target) throws CommandSyntaxException {
        final World world = world(context);
        // Beta's clock only counts up, so setting the time means advancing to the next occurrence.
        final long time = world.getTime();
        world.setTime(time + (target - time % DAY_LENGTH + DAY_LENGTH) % DAY_LENGTH);

        context.getSource().sendFeedback(Text.literal("Set the time to " + target));
        return target;
    }

    private static int add(final CommandContext<RetroCommandSource> context, final int amount) throws CommandSyntaxException {
        final World world = world(context);
        world.setTime(world.getTime() + amount);

        final int now = timeOfDay(world);
        context.getSource().sendFeedback(Text.literal("Added " + amount + " to the time, now " + now));
        return now;
    }

    private static int query(final CommandContext<RetroCommandSource> context, final int value) {
        context.getSource().sendFeedback(Text.literal("The time is " + value));
        return value;
    }

    private static int timeOfDay(final World world) {
        return (int) (world.getTime() % DAY_LENGTH);
    }

    private static World world(final CommandContext<RetroCommandSource> context) throws CommandSyntaxException {
        final World world = context.getSource().getWorld();
        if (world == null) {
            throw RetroCommandSource.REQUIRES_PLAYER.create();
        }
        return world;
    }
}
