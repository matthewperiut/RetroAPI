package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.commands.argument.EntityNames;
import com.periut.retroapi.text.Text;
import net.minecraft.entity.Entity;

import static com.periut.retroapi.commands.RetroCommandManager.argument;
import static com.periut.retroapi.commands.RetroCommandManager.literal;
import static com.periut.retroapi.commands.argument.EntityArgumentType.entity;
import static com.periut.retroapi.commands.argument.EntityArgumentType.getEntity;

/**
 * {@code /ride <rider> <vehicle>} and {@code /ride dismount}, named after modern's own
 * {@code /ride} rather than the mod's old positional form.
 */
public final class RideCommand {
    private RideCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("ride")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
            .then(literal("dismount")
                .executes(context -> dismount(context.getSource(), context.getSource().getEntityOrThrow())))
            .then(argument("rider", entity())
                .then(literal("dismount")
                    .executes(context -> dismount(context.getSource(), getEntity(context, "rider"))))
                .then(argument("vehicle", entity())
                    .executes(context -> {
                        final RetroCommandSource source = context.getSource();
                        final Entity rider = getEntity(context, "rider");
                        final Entity vehicle = getEntity(context, "vehicle");

                        if (rider == vehicle) {
                            source.sendError(Text.literal("An entity cannot ride itself"));
                            return 0;
                        }

                        rider.setVehicle(vehicle);
                        source.sendFeedback(Text.literal(EntityNames.displayName(rider)
                            + " is now riding " + EntityNames.displayName(vehicle)));
                        return Command.SINGLE_SUCCESS;
                    }))));
    }

    private static int dismount(final RetroCommandSource source, final Entity rider) {
        if (rider.vehicle == null) {
            source.sendError(Text.literal(EntityNames.displayName(rider) + " is not riding anything"));
            return 0;
        }

        rider.setVehicle(null);
        source.sendFeedback(Text.literal(EntityNames.displayName(rider) + " dismounted"));
        return Command.SINGLE_SUCCESS;
    }
}
