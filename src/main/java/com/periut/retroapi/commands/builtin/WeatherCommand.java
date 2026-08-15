package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.text.Text;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;

import static com.periut.retroapi.commands.RetroCommandManager.argument;
import static com.periut.retroapi.commands.RetroCommandManager.literal;
import static com.periut.retroapi.commands.argument.TimeArgumentType.getTime;
import static com.periut.retroapi.commands.argument.TimeArgumentType.time;

/**
 * {@code /weather clear|rain|thunder [duration]}, plus the {@code /toggledownfall} this mod has
 * always had - beta's own way of saying the same thing, and shorter.
 */
public final class WeatherCommand {
    /** What vanilla beta picks when it rolls new weather; used when no duration is given. */
    private static final int DEFAULT_DURATION = 6000;

    private WeatherCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("weather")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
            .then(literal("clear")
                .executes(context -> set(context, false, false, DEFAULT_DURATION))
                .then(argument("duration", time(1)).executes(context -> set(context, false, false, getTime(context, "duration")))))
            .then(literal("rain")
                .executes(context -> set(context, true, false, DEFAULT_DURATION))
                .then(argument("duration", time(1)).executes(context -> set(context, true, false, getTime(context, "duration")))))
            .then(literal("thunder")
                .executes(context -> set(context, true, true, DEFAULT_DURATION))
                .then(argument("duration", time(1)).executes(context -> set(context, true, true, getTime(context, "duration"))))));

        dispatcher.register(literal("toggledownfall")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
            .executes(WeatherCommand::toggle));
    }

    private static int set(final CommandContext<RetroCommandSource> context, final boolean raining, final boolean thundering, final int duration) throws CommandSyntaxException {
        final WorldProperties properties = properties(context);

        properties.setRaining(raining);
        properties.setThundering(thundering);
        // Beta counts the timer down and then flips the weather, so a duration is set on whichever
        // state is now running.
        properties.setRainTime(duration);
        properties.setThunderTime(duration);

        context.getSource().sendFeedback(Text.literal("Set the weather to " + (thundering ? "thunder" : raining ? "rain" : "clear")));
        return Command.SINGLE_SUCCESS;
    }

    private static int toggle(final CommandContext<RetroCommandSource> context) throws CommandSyntaxException {
        final WorldProperties properties = properties(context);
        final boolean raining = !properties.getRaining();

        properties.setRaining(raining);
        properties.setThundering(raining && properties.getThundering());

        context.getSource().sendFeedback(Text.literal("Toggled downfall " + (raining ? "on" : "off")));
        return Command.SINGLE_SUCCESS;
    }

    private static WorldProperties properties(final CommandContext<RetroCommandSource> context) throws CommandSyntaxException {
        final World world = context.getSource().getWorld();
        if (world == null) {
            throw RetroCommandSource.REQUIRES_PLAYER.create();
        }
        return world.getProperties();
    }
}
