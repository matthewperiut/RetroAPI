package com.periut.retroapi.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.periut.retroapi.commands.RetroCommands;
import com.periut.retroapi.commands.api.CommandRegistrationCallback;
import com.periut.retroapi.commands.builtin.AdminCommands;
import com.periut.retroapi.commands.builtin.ClearChatCommand;
import com.periut.retroapi.commands.builtin.ClearCommand;
import com.periut.retroapi.commands.builtin.FlyCommand;
import com.periut.retroapi.commands.builtin.GameModeCommand;
import com.periut.retroapi.commands.builtin.FillCommand;
import com.periut.retroapi.commands.builtin.GameRuleCommand;
import com.periut.retroapi.commands.builtin.SetBlockCommand;
import com.periut.retroapi.commands.builtin.GiveCommand;
import com.periut.retroapi.commands.builtin.GodCommand;
import com.periut.retroapi.commands.builtin.HatCommand;
import com.periut.retroapi.commands.builtin.HealCommand;
import com.periut.retroapi.commands.builtin.HelpCommand;
import com.periut.retroapi.commands.builtin.KillCommand;
import com.periut.retroapi.commands.builtin.ListCommand;
import com.periut.retroapi.commands.builtin.MeCommand;
import com.periut.retroapi.commands.builtin.MessageCommand;
import com.periut.retroapi.commands.builtin.MiscCommands;
import com.periut.retroapi.commands.builtin.NoclipCommand;
import com.periut.retroapi.commands.builtin.RideCommand;
import com.periut.retroapi.commands.builtin.SayCommand;
import com.periut.retroapi.commands.builtin.SeedCommand;
import com.periut.retroapi.commands.builtin.SummonCommand;
import com.periut.retroapi.commands.builtin.TeleportCommand;
import com.periut.retroapi.commands.builtin.TimeCommand;
import com.periut.retroapi.commands.builtin.TpaCommand;
import com.periut.retroapi.commands.builtin.WarpCommand;
import com.periut.retroapi.commands.builtin.WeatherCommand;
import com.periut.retroapi.text.Formatting;
import com.periut.retroapi.text.MutableText;
import com.periut.retroapi.text.ClickEvent;
import com.periut.retroapi.text.Text;
import com.periut.retroapi.text.Texts;

import java.util.HashMap;
import java.util.Map;

/**
 * Owns the command tree: builds it, runs commands against it, and trims copies of it for clients.
 *
 * <p>One instance exists per side. The client builds its own when a world loads (singleplayer runs
 * commands locally, as this mod always has); a dedicated server builds one at startup and sends
 * trimmed copies to whichever clients can understand them.
 */
public class RetroCommandManager {
    private static RetroCommandManager instance;

    private final CommandDispatcher<RetroCommandSource> dispatcher = new CommandDispatcher<>();
    private final RegistrationEnvironment environment;

    public RetroCommandManager(final RegistrationEnvironment environment) {
        this.environment = environment;

        HelpCommand.register(dispatcher, environment);
        GiveCommand.register(dispatcher, environment);
        TeleportCommand.register(dispatcher, environment);
        KillCommand.register(dispatcher, environment);
        ClearCommand.register(dispatcher, environment);
        TimeCommand.register(dispatcher, environment);
        WeatherCommand.register(dispatcher, environment);
        SummonCommand.register(dispatcher, environment);
        SeedCommand.register(dispatcher, environment);
        GameRuleCommand.register(dispatcher, environment);
        SetBlockCommand.register(dispatcher, environment);
        FillCommand.register(dispatcher, environment);
        SayCommand.register(dispatcher, environment);
        MeCommand.register(dispatcher, environment);
        MessageCommand.register(dispatcher, environment);
        ListCommand.register(dispatcher, environment);

        ClearChatCommand.register(dispatcher, environment);

        GodCommand.register(dispatcher, environment);
        HealCommand.register(dispatcher, environment);
        HatCommand.register(dispatcher, environment);
        RideCommand.register(dispatcher, environment);
        WarpCommand.register(dispatcher, environment);
        NoclipCommand.register(dispatcher, environment);

        MiscCommands.registerWhoAmI(dispatcher);
        MiscCommands.registerClock(dispatcher);
        MiscCommands.registerId(dispatcher);
        MiscCommands.registerMobs(dispatcher);
        MiscCommands.registerMods(dispatcher);

        AdminCommands.register(dispatcher, environment);
        TpaCommand.register(dispatcher, environment);

        // Not gated on anything any more: RetroAPI implements the game modes itself.
        GameModeCommand.register(dispatcher, environment);
        FlyCommand.register(dispatcher, environment);
        if (RetroCommands.cryConfig) {
            MiscCommands.registerReloadConfig(dispatcher);
        }

        for (final CommandRegistrationCallback callback : CommandRegistrationCallback.CALLBACKS) {
            callback.register(dispatcher, environment);
        }
    }

    public static RetroCommandManager getInstance() {
        return instance;
    }

    public static void setInstance(final RetroCommandManager manager) {
        instance = manager;
    }

    public CommandDispatcher<RetroCommandSource> getDispatcher() {
        return dispatcher;
    }

    public RegistrationEnvironment getEnvironment() {
        return environment;
    }

    /**
     * Runs a command, reporting any syntax error the way modern Minecraft does: the message, then
     * the input up to the failure with the rest underlined in red and {@code <--[HERE]} after it.
     *
     * @param command the command without its leading slash
     * @return the command's own result, or 0 if it failed
     */
    public int execute(final RetroCommandSource source, final String command) {
        try {
            return dispatcher.execute(command, source);
        } catch (final CommandSyntaxException ex) {
            source.sendError(Texts.of(ex.getRawMessage()));

            if (ex.getInput() != null && ex.getCursor() >= 0) {
                source.sendError(describeFailurePosition(ex));
            }
            return 0;
        } catch (final RuntimeException ex) {
            // A command throwing is a bug in that command, not input the player can fix; say so
            // rather than letting it escape into the chat handler and disconnect them.
            source.sendError(Text.literal("Command failed: " + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage())));
            com.periut.retroapi.RetroAPI.LOGGER.warn("Command '" + command + "' threw " + ex);
            return 0;
        }
    }

    private static Text describeFailurePosition(final CommandSyntaxException ex) {
        final String input = ex.getInput();
        final int cursor = Math.min(input.length(), ex.getCursor());

        final MutableText context = Text.empty().formatted(Formatting.GRAY)
            .styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + input)));

        if (cursor > 10) {
            context.append("...");
        }
        context.append(Text.literal(input.substring(Math.max(0, cursor - 10), cursor)));
        if (cursor < input.length()) {
            context.append(Text.literal(input.substring(cursor)).formatted(Formatting.RED, Formatting.UNDERLINE));
        }
        context.append(Text.literal("<--[HERE]").formatted(Formatting.RED, Formatting.ITALIC));

        return context;
    }

    public ParseResults<RetroCommandSource> parse(final RetroCommandSource source, final String command) {
        return dispatcher.parse(command, source);
    }

    /**
     * A copy of the tree containing only what this source may use.
     *
     * <p>Brigadier itself does not consult {@code requires} when listing completions - modern
     * Minecraft relies on the server having already sent each client a trimmed tree, and this does
     * the same job for the client-side dispatcher.
     */
    public RootCommandNode<RetroCommandSource> makeTreeForSource(final RetroCommandSource source) {
        final RootCommandNode<RetroCommandSource> result = new RootCommandNode<>();
        makeTreeForSource(dispatcher.getRoot(), result, source, new HashMap<>());
        return result;
    }

    private static void makeTreeForSource(final CommandNode<RetroCommandSource> from,
                                          final CommandNode<RetroCommandSource> to,
                                          final RetroCommandSource source,
                                          final Map<CommandNode<RetroCommandSource>, CommandNode<RetroCommandSource>> copies) {
        for (final CommandNode<RetroCommandSource> child : from.getChildren()) {
            if (!child.canUse(source)) {
                continue;
            }

            final var builder = child.createBuilder();
            // The requirement has already been checked; keeping it would make the copy re-test a
            // source it will never see again.
            builder.requires(ignored -> true);

            if (builder.getRedirect() != null) {
                builder.redirect(copies.get(builder.getRedirect()));
            }

            final CommandNode<RetroCommandSource> copy = builder.build();
            copies.put(child, copy);
            to.addChild(copy);

            if (!child.getChildren().isEmpty()) {
                makeTreeForSource(child, copy, source, copies);
            }
        }
    }

    /** Shorthand so command classes read like modern Minecraft's. */
    public static LiteralArgumentBuilder<RetroCommandSource> literal(final String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public static <T> RequiredArgumentBuilder<RetroCommandSource, T> argument(final String name, final ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    /** Registers {@code name} and points every alias at it, so they share one implementation. */
    public static void alias(final CommandDispatcher<RetroCommandSource> dispatcher, final LiteralCommandNode<RetroCommandSource> target, final String... aliases) {
        for (final String alias : aliases) {
            dispatcher.register(literal(alias).requires(target.getRequirement()).redirect(target));
        }
    }

    /** Convenience for commands that only differ in which node they hang from. */
    public static boolean isArgument(final CommandNode<?> node) {
        return node instanceof ArgumentCommandNode;
    }
}
