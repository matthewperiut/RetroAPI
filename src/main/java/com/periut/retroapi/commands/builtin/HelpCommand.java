package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.commands.SuggestionHelper;
import com.periut.retroapi.text.ClickEvent;
import com.periut.retroapi.text.Formatting;
import com.periut.retroapi.text.HoverEvent;
import com.periut.retroapi.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.periut.retroapi.commands.RetroCommandManager.argument;
import static com.periut.retroapi.commands.RetroCommandManager.literal;

/**
 * {@code /help [page|command]}.
 *
 * <p>Every line is generated from the command tree rather than maintained by hand, so a command a
 * mod adds is documented the moment it is registered. Pages exist because beta shows ten lines of
 * chat; modern, with a scrollback, prints the lot at once.
 */
public final class HelpCommand {
    private static final int LINES_PER_PAGE = 7;

    public static final SimpleCommandExceptionType FAILED = new SimpleCommandExceptionType(
        Text.literal("Unknown command or insufficient permissions"));

    private HelpCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("help")
            .executes(context -> listPage(context, dispatcher, 1))
            .then(argument("page", integer(1))
                .executes(context -> listPage(context, dispatcher, getInteger(context, "page"))))
            .then(argument("command", StringArgumentType.greedyString())
                .suggests((context, builder) -> suggestCommands(dispatcher, context.getSource(), builder))
                .executes(context -> describe(context, dispatcher, StringArgumentType.getString(context, "command")))));
    }

    private static java.util.concurrent.CompletableFuture<Suggestions> suggestCommands(
        final CommandDispatcher<RetroCommandSource> dispatcher, final RetroCommandSource source, final com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        final List<String> names = new ArrayList<>();
        for (final CommandNode<RetroCommandSource> node : dispatcher.getRoot().getChildren()) {
            if (node.canUse(source)) {
                names.add(node.getName());
            }
        }
        return SuggestionHelper.suggestMatching(names, builder);
    }

    private static int listPage(final CommandContext<RetroCommandSource> context, final CommandDispatcher<RetroCommandSource> dispatcher, final int page) {
        final RetroCommandSource source = context.getSource();
        final Map<CommandNode<RetroCommandSource>, String> usages = dispatcher.getSmartUsage(dispatcher.getRoot(), source);
        final List<String> lines = new ArrayList<>(usages.values());
        lines.sort(String::compareTo);

        final int pages = Math.max(1, (lines.size() + LINES_PER_PAGE - 1) / LINES_PER_PAGE);
        final int clamped = Math.min(page, pages);

        source.sendFeedback(Text.literal("--- Commands (page " + clamped + "/" + pages + ") ---").formatted(Formatting.YELLOW));
        for (int i = (clamped - 1) * LINES_PER_PAGE; i < Math.min(lines.size(), clamped * LINES_PER_PAGE); i++) {
            source.sendFeedback(usageLine(lines.get(i)));
        }
        if (clamped < pages) {
            source.sendFeedback(Text.literal("Use /help " + (clamped + 1) + " for more, or /help <command> for detail").formatted(Formatting.GRAY));
        }

        return lines.size();
    }

    private static int describe(final CommandContext<RetroCommandSource> context, final CommandDispatcher<RetroCommandSource> dispatcher, final String command) throws CommandSyntaxException {
        final RetroCommandSource source = context.getSource();
        final var parsed = dispatcher.parse(command, source);

        if (parsed.getContext().getNodes().isEmpty()) {
            throw FAILED.create();
        }

        final var last = parsed.getContext().getNodes().get(parsed.getContext().getNodes().size() - 1);
        final String[] usages = dispatcher.getAllUsage(last.getNode(), source, true);

        source.sendFeedback(Text.literal("--- /" + command + " ---").formatted(Formatting.YELLOW));
        for (final String usage : usages) {
            source.sendFeedback(usageLine(command + (usage.isEmpty() ? "" : " " + usage)));
        }

        return usages.length;
    }

    /** Clicking a usage line puts it in the chat box, exactly as modern's help does. */
    private static Text usageLine(final String usage) {
        final String command = "/" + usage;
        return Text.literal(command).styled(style -> style
            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, stripPlaceholders(command)))
            .withHoverEvent(HoverEvent.showText(Text.literal("Click to put this in the chat box"))));
    }

    /** Suggesting the literal part is useful; suggesting {@code <targets>} is not. */
    private static String stripPlaceholders(final String command) {
        final int placeholder = firstPlaceholder(command);
        return placeholder < 0 ? command : command.substring(0, placeholder).trim();
    }

    private static int firstPlaceholder(final String command) {
        final List<Integer> candidates = Arrays.asList(command.indexOf('<'), command.indexOf('['), command.indexOf('('));
        int first = -1;
        for (final int candidate : candidates) {
            if (candidate >= 0 && (first < 0 || candidate < first)) {
                first = candidate;
            }
        }
        return first;
    }
}
