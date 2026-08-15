package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.periut.retroapi.commands.api.PlayerWarps;
import com.periut.retroapi.commands.CommandUtil;
import com.periut.retroapi.commands.Position;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.commands.SuggestionHelper;
import com.periut.retroapi.text.ClickEvent;
import com.periut.retroapi.text.Formatting;
import com.periut.retroapi.text.Text;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.periut.retroapi.commands.RetroCommandManager.argument;
import static com.periut.retroapi.commands.RetroCommandManager.literal;

/**
 * {@code /warp set|tp|remove|list} - per-player saved positions.
 *
 * <p>Stored on the player entity as one space-separated string, the format this mod has always
 * written, so warps saved by an older version still load.
 */
public final class WarpCommand {
    private static final int LINES_PER_PAGE = 5;
    private static final int FIELDS_PER_WARP = 4;

    private WarpCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("warp")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
            .then(literal("set")
                .then(argument("name", StringArgumentType.word())
                    .executes(context -> set(context, StringArgumentType.getString(context, "name")))))
            .then(literal("tp")
                .then(argument("name", StringArgumentType.word())
                    .suggests((context, builder) -> SuggestionHelper.suggestMatching(names(context.getSource()), builder))
                    .executes(context -> teleport(context, StringArgumentType.getString(context, "name")))))
            .then(literal("remove")
                .then(argument("name", StringArgumentType.word())
                    .suggests((context, builder) -> SuggestionHelper.suggestMatching(names(context.getSource()), builder))
                    .executes(context -> remove(context, StringArgumentType.getString(context, "name")))))
            .then(literal("list")
                .executes(context -> list(context, 1))
                .then(argument("page", integer(1))
                    .executes(context -> list(context, getInteger(context, "page"))))));
    }

    private static int set(final CommandContext<RetroCommandSource> context, final String name) throws CommandSyntaxException {
        final RetroCommandSource source = context.getSource();
        final PlayerEntity player = source.getPlayerOrThrow();

        // The store is space-separated, so a name containing one would corrupt every later entry.
        if (name.contains(" ") || name.contains("|")) {
            source.sendError(Text.literal("Warp names cannot contain spaces or '|'"));
            return 0;
        }

        final Map<String, Position> warps = read(player);
        warps.put(name, new Position(player.x, player.y, player.z));
        write(player, warps);

        source.sendFeedback(Text.literal("Set warp " + name));
        return Command.SINGLE_SUCCESS;
    }

    private static int teleport(final CommandContext<RetroCommandSource> context, final String name) throws CommandSyntaxException {
        final RetroCommandSource source = context.getSource();
        final PlayerEntity player = source.getPlayerOrThrow();

        final Position warp = read(player).get(name);
        if (warp == null) {
            source.sendError(Text.literal("No warp named " + name));
            return 0;
        }

        // The small lift keeps a warp saved on a solid block from dropping the player into it.
        CommandUtil.teleport(player, new Position(warp.x(), warp.y() + 0.1, warp.z()));
        source.sendFeedback(Text.literal("Teleported to " + name));
        return Command.SINGLE_SUCCESS;
    }

    private static int remove(final CommandContext<RetroCommandSource> context, final String name) throws CommandSyntaxException {
        final RetroCommandSource source = context.getSource();
        final PlayerEntity player = source.getPlayerOrThrow();

        final Map<String, Position> warps = read(player);
        if (warps.remove(name) == null) {
            source.sendError(Text.literal("No warp named " + name));
            return 0;
        }
        write(player, warps);

        source.sendFeedback(Text.literal("Removed warp " + name));
        return Command.SINGLE_SUCCESS;
    }

    private static int list(final CommandContext<RetroCommandSource> context, final int page) throws CommandSyntaxException {
        final RetroCommandSource source = context.getSource();
        final Map<String, Position> warps = read(source.getPlayerOrThrow());

        if (warps.isEmpty()) {
            source.sendFeedback(Text.literal("No warps set"));
            return 0;
        }

        final List<String> names = new ArrayList<>(warps.keySet());
        final int pages = Math.max(1, (names.size() + LINES_PER_PAGE - 1) / LINES_PER_PAGE);
        final int clamped = Math.min(page, pages);

        source.sendFeedback(Text.literal("--- Warps (page " + clamped + "/" + pages + ") ---").formatted(Formatting.YELLOW));
        for (int i = (clamped - 1) * LINES_PER_PAGE; i < Math.min(names.size(), clamped * LINES_PER_PAGE); i++) {
            final String name = names.get(i);
            source.sendFeedback(Text.literal(name + ": " + warps.get(name))
                .styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/warp tp " + name))));
        }

        return names.size();
    }

    private static List<String> names(final RetroCommandSource source) {
        final PlayerEntity player = source.getPlayer();
        return player == null ? List.of() : new ArrayList<>(read(player).keySet());
    }

    private static Map<String, Position> read(final PlayerEntity player) {
        final Map<String, Position> warps = new LinkedHashMap<>();
        final String stored = ((PlayerWarps) player).spc$getWarpString();
        if (stored == null || stored.isEmpty()) {
            return warps;
        }

        final String[] fields = stored.trim().split(" ");
        for (int i = 0; i + FIELDS_PER_WARP <= fields.length; i += FIELDS_PER_WARP) {
            try {
                warps.put(fields[i], new Position(
                    Double.parseDouble(fields[i + 1]),
                    Double.parseDouble(fields[i + 2]),
                    Double.parseDouble(fields[i + 3])));
            } catch (final NumberFormatException ignored) {
                // One unreadable entry should not cost the player the rest of their warps.
            }
        }
        return warps;
    }

    private static void write(final PlayerEntity player, final Map<String, Position> warps) {
        final StringBuilder builder = new StringBuilder();
        for (final Map.Entry<String, Position> warp : warps.entrySet()) {
            builder.append(warp.getKey()).append(' ')
                .append(String.format("%.1f %.1f %.1f", warp.getValue().x(), warp.getValue().y(), warp.getValue().z()))
                .append(' ');
        }
        ((PlayerWarps) player).spc$setWarpString(builder.toString());
    }
}
