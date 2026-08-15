package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.commands.SuggestionHelper;
import com.periut.retroapi.commands.network.ServerCommandNetworking;
import com.periut.retroapi.text.Text;
import com.periut.retroapi.commands.util.ServerUtil;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.PlayerManager;

import java.util.ArrayList;
import java.util.List;

import static com.periut.retroapi.commands.RetroCommandManager.argument;
import static com.periut.retroapi.commands.RetroCommandManager.literal;
import static com.periut.retroapi.commands.argument.MessageArgumentType.getMessage;
import static com.periut.retroapi.commands.argument.MessageArgumentType.message;

/**
 * The operator commands: {@code op}, {@code deop}, {@code kick}, {@code ban}, {@code ban-ip},
 * {@code pardon}, {@code pardon-ip}, {@code whitelist}, {@code save-*} and {@code stop}.
 *
 * <p>All of them are registered only on a dedicated server. In singleplayer there is nobody to
 * moderate and no server to stop, and a command that cannot work is better absent from the
 * completion list than present and failing.
 *
 * <p>Player names here are plain strings rather than selectors, because op and ban lists work on
 * names of players who may be offline - the one place where a selector would be wrong.
 */
public final class AdminCommands {
    private AdminCommands() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        if (!environment.isDedicated()) {
            return;
        }

        dispatcher.register(literal("op")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_OWNER))
            .then(playerName("player")
                .executes(context -> op(context, StringArgumentType.getString(context, "player")))));

        dispatcher.register(literal("deop")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_OWNER))
            .then(playerName("player")
                .executes(context -> deop(context, StringArgumentType.getString(context, "player")))));

        dispatcher.register(literal("kick")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_OWNER))
            .then(playerName("player")
                .executes(context -> kick(context, StringArgumentType.getString(context, "player"), "Kicked by admin"))
                .then(argument("reason", message())
                    .executes(context -> kick(context, StringArgumentType.getString(context, "player"), getMessage(context, "reason"))))));

        dispatcher.register(literal("ban")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_OWNER))
            .then(playerName("player")
                .executes(context -> ban(context, StringArgumentType.getString(context, "player")))));

        dispatcher.register(literal("ban-ip")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_OWNER))
            .then(argument("address", StringArgumentType.word())
                .executes(context -> banIp(context, StringArgumentType.getString(context, "address")))));

        dispatcher.register(literal("pardon")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_OWNER))
            .then(argument("player", StringArgumentType.word())
                .executes(context -> pardon(context, StringArgumentType.getString(context, "player")))));

        dispatcher.register(literal("pardon-ip")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_OWNER))
            .then(argument("address", StringArgumentType.word())
                .executes(context -> pardonIp(context, StringArgumentType.getString(context, "address")))));

        registerWhitelist(dispatcher);
        registerSave(dispatcher);

        dispatcher.register(literal("stop")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_OWNER))
            .executes(context -> {
                announce(context, "Stopping the server..");
                ServerUtil.getServer().stop();
                return Command.SINGLE_SUCCESS;
            }));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<RetroCommandSource, String> playerName(final String name) {
        return argument(name, StringArgumentType.word())
            .suggests((context, builder) -> SuggestionHelper.suggestMatching(context.getSource().getPlayerNames(), builder));
    }

    private static int op(final CommandContext<RetroCommandSource> context, final String player) {
        ServerUtil.getConnectionManager().addToOperators(player);
        announce(context, "Opping " + player);
        ServerUtil.getConnectionManager().messagePlayer(player, "§eYou are now op!");

        refreshClient(player);
        return Command.SINGLE_SUCCESS;
    }

    private static int deop(final CommandContext<RetroCommandSource> context, final String player) {
        ServerUtil.getConnectionManager().removeFromOperators(player);
        announce(context, "De-opping " + player);
        ServerUtil.getConnectionManager().messagePlayer(player, "§eYou are no longer op!");

        refreshClient(player);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * A change of rank changes which commands exist for that player, so the client is told its new
     * level and handed a freshly trimmed tree - otherwise its completions would describe the
     * permissions it had when it joined.
     */
    private static void refreshClient(final String name) {
        final ServerPlayerEntity player = ServerUtil.getConnectionManager().getPlayer(name);
        if (player == null) {
            return;
        }
        ServerUtil.informPlayerOpStatus(name);
        ServerCommandNetworking.sendTree(player);
    }

    private static int kick(final CommandContext<RetroCommandSource> context, final String name, final String reason) {
        final ServerPlayerEntity player = findPlayer(name);
        if (player == null) {
            context.getSource().sendError(Text.literal("Can't find player " + name));
            return 0;
        }

        player.networkHandler.disconnect(reason);
        announce(context, "Kicking " + player.name);
        return Command.SINGLE_SUCCESS;
    }

    private static int ban(final CommandContext<RetroCommandSource> context, final String name) {
        ServerUtil.getConnectionManager().banPlayer(name);
        announce(context, "Banning " + name);

        final ServerPlayerEntity player = findPlayer(name);
        if (player != null) {
            player.networkHandler.disconnect("Banned by admin");
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int banIp(final CommandContext<RetroCommandSource> context, final String address) {
        ServerUtil.getConnectionManager().banIp(address);
        announce(context, "Banning ip " + address);
        return Command.SINGLE_SUCCESS;
    }

    private static int pardon(final CommandContext<RetroCommandSource> context, final String name) {
        ServerUtil.getConnectionManager().unbanPlayer(name);
        announce(context, "Pardoning " + name);
        return Command.SINGLE_SUCCESS;
    }

    private static int pardonIp(final CommandContext<RetroCommandSource> context, final String address) {
        ServerUtil.getConnectionManager().unbanIp(address);
        announce(context, "Pardoning ip " + address);
        return Command.SINGLE_SUCCESS;
    }

    private static void registerWhitelist(final CommandDispatcher<RetroCommandSource> dispatcher) {
        dispatcher.register(literal("whitelist")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_OWNER))
            .then(literal("on").executes(context -> setWhitelist(context, true)))
            .then(literal("off").executes(context -> setWhitelist(context, false)))
            .then(literal("list").executes(context -> {
                final List<String> names = new ArrayList<>(ServerUtil.getConnectionManager().getWhitelist());
                names.sort(String.CASE_INSENSITIVE_ORDER);
                context.getSource().sendFeedback(Text.literal("White-listed players: " + String.join(", ", names)));
                return names.size();
            }))
            .then(literal("add").then(playerName("player").executes(context -> {
                final String player = StringArgumentType.getString(context, "player");
                ServerUtil.getConnectionManager().addToWhitelist(player);
                announce(context, "Added " + player + " to white-list");
                return Command.SINGLE_SUCCESS;
            })))
            .then(literal("remove").then(argument("player", StringArgumentType.word())
                .suggests((context, builder) -> SuggestionHelper.suggestMatching(ServerUtil.getConnectionManager().getWhitelist(), builder))
                .executes(context -> {
                    final String player = StringArgumentType.getString(context, "player");
                    ServerUtil.getConnectionManager().removeFromWhitelist(player);
                    announce(context, "Removed " + player + " from white-list");
                    return Command.SINGLE_SUCCESS;
                })))
            .then(literal("reload").executes(context -> {
                ServerUtil.getConnectionManager().reloadWhitelist();
                announce(context, "Reloaded white-list");
                return Command.SINGLE_SUCCESS;
            })));
    }

    private static int setWhitelist(final CommandContext<RetroCommandSource> context, final boolean enabled) {
        ServerUtil.getServer().properties.setProperty("white-list", enabled);
        announce(context, "Turned " + (enabled ? "on" : "off") + " white-listing");
        return Command.SINGLE_SUCCESS;
    }

    private static void registerSave(final CommandDispatcher<RetroCommandSource> dispatcher) {
        dispatcher.register(literal("save-all")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_OWNER))
            .executes(context -> {
                announce(context, "Forcing save..");
                final PlayerManager players = ServerUtil.getConnectionManager();
                if (players != null) {
                    players.savePlayers();
                }
                for (int i = 0; i < ServerUtil.getServer().worlds.length; i++) {
                    ServerUtil.getServer().worlds[i].saveWithLoadingDisplay(true, null);
                }
                announce(context, "Save complete.");
                return Command.SINGLE_SUCCESS;
            }));

        dispatcher.register(literal("save-off")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_OWNER))
            .executes(context -> setSaving(context, false)));

        dispatcher.register(literal("save-on")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_OWNER))
            .executes(context -> setSaving(context, true)));
    }

    private static int setSaving(final CommandContext<RetroCommandSource> context, final boolean enabled) {
        for (int i = 0; i < ServerUtil.getServer().worlds.length; i++) {
            ServerUtil.getServer().worlds[i].savingDisabled = !enabled;
        }
        announce(context, enabled ? "Enabled level saving.." : "Disabled level saving..");
        return Command.SINGLE_SUCCESS;
    }

    private static ServerPlayerEntity findPlayer(final String name) {
        for (final ServerPlayerEntity player : ServerUtil.getConnectionManager().players) {
            if (player.name.equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }

    /** Operator actions are announced to every other operator and written to the log, as in vanilla. */
    private static void announce(final CommandContext<RetroCommandSource> context, final String message) {
        ServerUtil.sendFeedbackAndLog(context.getSource().getName(), message);
    }
}
