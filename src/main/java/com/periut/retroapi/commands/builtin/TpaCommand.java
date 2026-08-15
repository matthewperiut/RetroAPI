package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.periut.retroapi.commands.CommandUtil;
import com.periut.retroapi.commands.Position;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.text.ClickEvent;
import com.periut.retroapi.text.Formatting;
import com.periut.retroapi.text.Text;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

import static com.periut.retroapi.commands.RetroCommandManager.argument;
import static com.periut.retroapi.commands.RetroCommandManager.literal;
import static com.periut.retroapi.commands.argument.EntityArgumentType.getPlayer;
import static com.periut.retroapi.commands.argument.EntityArgumentType.player;

/**
 * {@code /tpa <player>} and {@code /tpa accept} - a teleport a player can ask for rather than take.
 *
 * <p>Not a vanilla command in any version, so there is no modern shape to follow; the request
 * message is now clickable, which is the one thing modern chat would have done differently.
 */
public final class TpaCommand {
    private static final long EXPIRY_TICKS = 2400;

    private static final List<Request> REQUESTS = new ArrayList<>();

    private TpaCommand() {
    }

    private record Request(String from, String to, long time) {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        if (!environment.isDedicated()) {
            return;
        }

        dispatcher.register(literal("tpa")
            .then(literal("accept").executes(TpaCommand::accept))
            .then(argument("player", player())
                .executes(TpaCommand::request)));
    }

    private static int request(final CommandContext<RetroCommandSource> context) throws CommandSyntaxException {
        final RetroCommandSource source = context.getSource();
        final PlayerEntity self = source.getPlayerOrThrow();
        final PlayerEntity target = getPlayer(context, "player");

        if (self == target) {
            source.sendError(Text.literal("You are already there"));
            return 0;
        }

        target.sendMessage("§7" + self.name + " wants to teleport to you.");
        target.sendMessage("§7Type \"/tpa accept\" to allow it. (expires in 2 minutes)");

        REQUESTS.add(new Request(self.name, target.name, self.world.getTime()));
        source.sendFeedback(Text.literal("Sent a teleport request to " + target.name).formatted(Formatting.GRAY));
        return Command.SINGLE_SUCCESS;
    }

    private static int accept(final CommandContext<RetroCommandSource> context) throws CommandSyntaxException {
        final RetroCommandSource source = context.getSource();
        final PlayerEntity self = source.getPlayerOrThrow();
        final long now = self.world.getTime();

        // Newest first, so accepting twice in a row answers the two most recent requests.
        for (int i = REQUESTS.size() - 1; i >= 0; i--) {
            final Request request = REQUESTS.get(i);
            if (!request.to().equals(self.name)) {
                continue;
            }

            REQUESTS.remove(i);

            if (now - request.time() > EXPIRY_TICKS) {
                source.sendError(Text.literal("That teleport request has expired"));
                return 0;
            }

            final PlayerEntity from = findPlayer(source, request.from());
            if (from == null) {
                source.sendError(Text.literal(request.from() + " is no longer online"));
                return 0;
            }

            CommandUtil.teleport(from, new Position(self.x, self.y, self.z));
            from.sendMessage("§7Teleported to " + self.name);
            source.sendFeedback(Text.literal("Teleported " + from.name + " to you"));
            return Command.SINGLE_SUCCESS;
        }

        source.sendError(Text.literal("You have no pending teleport requests"));
        return 0;
    }

    private static PlayerEntity findPlayer(final RetroCommandSource source, final String name) {
        for (final PlayerEntity player : source.getPlayers()) {
            if (player.name.equals(name)) {
                return player;
            }
        }
        return null;
    }
}
