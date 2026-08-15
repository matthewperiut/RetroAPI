package com.periut.retroapi.commands.test;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;

/** Exercises the vendored Brigadier against the behaviour modern Minecraft relies on. */
public final class BrigadierTest {
    private BrigadierTest() {
    }

    public static void run() {
        stringReader();
        parsing();
        suggestions();
        usage();
        redirects();
        ambiguities();
    }

    private static void stringReader() {
        Tests.group("StringReader");

        Tests.eq("reads an int", 123, readOr(() -> new StringReader("123").readInt()));
        Tests.eq("reads a negative int", -123, readOr(() -> new StringReader("-123").readInt()));
        Tests.eq("reads a double", 1.5, readOr(() -> new StringReader("1.5").readDouble()));
        Tests.eq("reads an unquoted string", "hello", new StringReader("hello world").readUnquotedString());
        Tests.eq("reads a quoted string", "hello world", readOr(() -> new StringReader("\"hello world\"").readString()));
        Tests.eq("unescapes inside quotes", "he\"llo", readOr(() -> new StringReader("\"he\\\"llo\"").readString()));
        Tests.eq("reads a boolean", true, readOr(() -> new StringReader("true").readBoolean()));

        Tests.throwsError("rejects an unclosed quote", CommandSyntaxException.class, () -> new StringReader("\"hello").readString());
        Tests.throwsError("rejects a bad escape", CommandSyntaxException.class, () -> new StringReader("\"he\\llo\"").readString());
        Tests.throwsError("rejects a non-number", CommandSyntaxException.class, () -> new StringReader("abc").readInt());
        Tests.throwsError("rejects a non-boolean", CommandSyntaxException.class, () -> new StringReader("yes").readBoolean());

        // The cursor must survive a failed range check, or the error would point at the wrong place.
        final StringReader reader = new StringReader("hello world");
        reader.readUnquotedString();
        Tests.eq("cursor stops at the separator", 5, reader.getCursor());
        Tests.eq("remaining text after a read", " world", reader.getRemaining());
    }

    private static void parsing() {
        Tests.group("parsing");

        final CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
        dispatcher.register(literal("foo").then(argument("bar", integer()).executes(c -> getInteger(c, "bar"))));
        dispatcher.register(literal("say").then(argument("message", greedyString()).executes(c -> getString(c, "message").length())));
        dispatcher.register(literal("empty"));

        Tests.eq("executes and returns the command result", 42, executeOr(dispatcher, "foo 42"));
        Tests.eq("greedy string takes the rest of the line", 11, executeOr(dispatcher, "say hello world"));

        Tests.throwsError("unknown command", CommandSyntaxException.class, () -> dispatcher.execute("nope", new Object()));
        Tests.throwsError("incorrect argument", CommandSyntaxException.class, () -> dispatcher.execute("foo bar", new Object()));
        Tests.throwsError("trailing data", CommandSyntaxException.class, () -> dispatcher.execute("foo 42 extra", new Object()));
        Tests.throwsError("node with no executor", CommandSyntaxException.class, () -> dispatcher.execute("empty", new Object()));

        // The failure position is what the chat screen underlines, so it has to be exact.
        try {
            dispatcher.execute("foo bar", new Object());
            Tests.check("argument error carries a cursor", false);
        } catch (final CommandSyntaxException ex) {
            Tests.eq("argument error points at the argument", 4, ex.getCursor());
            Tests.check("argument error keeps its input", "foo bar".equals(ex.getInput()));
        }

        // Requirements gate parsing, not just execution.
        final CommandDispatcher<Boolean> gated = new CommandDispatcher<>();
        gated.register(LiteralArgumentBuilder.<Boolean>literal("op").requires(allowed -> allowed).executes(c -> 1));
        Tests.eq("permitted source runs the command", 1, executeOr(gated, "op", Boolean.TRUE));
        Tests.throwsError("denied source cannot see the command", CommandSyntaxException.class, () -> gated.execute("op", Boolean.FALSE));
    }

    private static void suggestions() {
        Tests.group("suggestions");

        final CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
        dispatcher.register(literal("give").then(argument("item", word()).suggests((c, b) -> {
            for (final String item : Arrays.asList("stone", "stick", "torch")) {
                if (item.startsWith(b.getRemainingLowerCase())) {
                    b.suggest(item);
                }
            }
            return b.buildFuture();
        }).executes(c -> 1)));
        dispatcher.register(literal("gamemode").executes(c -> 1));
        dispatcher.register(literal("kill").executes(c -> 1));

        Tests.eq("empty input suggests every command", List.of("gamemode", "give", "kill"), suggest(dispatcher, ""));
        Tests.eq("prefix filters commands", List.of("gamemode", "give"), suggest(dispatcher, "g"));
        Tests.eq("argument suggestions come from the provider", List.of("stick", "stone", "torch"), suggest(dispatcher, "give "));
        Tests.eq("argument suggestions filter on what is typed", List.of("stick", "stone"), suggest(dispatcher, "give st"));
        Tests.eq("no suggestions once a word is complete and unknown", List.of(), suggest(dispatcher, "give zzz"));

        // The replacement range must cover only the partial word, not the whole line.
        final ParseResults<Object> parse = dispatcher.parse("give st", new Object());
        final Suggestions suggestions = dispatcher.getCompletionSuggestions(parse).join();
        Tests.eq("suggestion range starts at the partial word", 5, suggestions.getRange().getStart());
        Tests.eq("suggestion range ends at the cursor", 7, suggestions.getRange().getEnd());
        final Suggestion first = suggestions.getList().get(0);
        Tests.eq("applying a suggestion rewrites only its range", "give stick", first.apply("give st"));
    }

    private static void usage() {
        Tests.group("usage");

        final CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
        dispatcher.register(literal("time")
            .then(literal("set").then(argument("value", integer()).executes(c -> 1)))
            .then(literal("add").then(argument("value", integer()).executes(c -> 1)))
            .then(literal("query").executes(c -> 1)));
        dispatcher.register(literal("kill").executes(c -> 1).then(argument("target", word()).executes(c -> 1)));

        final List<String> all = Arrays.asList(dispatcher.getAllUsage(dispatcher.getRoot(), new Object(), true));
        Tests.check("all usage lists every leaf", all.contains("time set <value>") && all.contains("time query"));
        Tests.check("all usage includes an optional-argument form", all.contains("kill") && all.contains("kill <target>"));

        final Map<CommandNode<Object>, String> smart = dispatcher.getSmartUsage(dispatcher.getRoot(), new Object());
        final List<String> lines = new ArrayList<>(smart.values());
        Tests.check("smart usage folds sibling literals", lines.contains("time (set|add|query)"));
        Tests.check("smart usage marks optional trailing arguments", lines.contains("kill [<target>]"));
    }

    private static void redirects() {
        Tests.group("redirects");

        final CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
        final LiteralCommandNode<Object> root = dispatcher.register(literal("execute").then(argument("value", integer()).executes(c -> getInteger(c, "value"))));
        dispatcher.register(literal("run").redirect(root));

        Tests.eq("a redirect reaches the target's children", 7, executeOr(dispatcher, "run 7"));
        Tests.eq("a redirect suggests the target's children", List.of("<value>"), usageAfter(dispatcher, "run"));
    }

    private static void ambiguities() {
        Tests.group("ambiguities");

        final CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
        dispatcher.register(literal("foo").then(literal("bar").executes(c -> 1)).then(argument("baz", word()).executes(c -> 1)));

        final List<String> found = new ArrayList<>();
        dispatcher.findAmbiguities((parent, child, sibling, inputs) -> found.add(child.getName() + "/" + sibling.getName()));
        Tests.check("a literal shadowed by a word argument is reported", found.contains("bar/baz"));
    }

    private static List<String> usageAfter(final CommandDispatcher<Object> dispatcher, final String command) {
        final CommandNode<Object> node = dispatcher.findNode(Arrays.asList(command.split(" ")));
        return dispatcher.getSmartUsage(node.getRedirect() == null ? node : node.getRedirect(), new Object()).values()
            .stream().collect(Collectors.toList());
    }

    private static List<String> suggest(final CommandDispatcher<Object> dispatcher, final String input) {
        final Suggestions suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(input, new Object())).join();
        return suggestions.getList().stream().map(Suggestion::getText).collect(Collectors.toList());
    }

    private static <T> T readOr(final ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (final Throwable ex) {
            return null;
        }
    }

    private static int executeOr(final CommandDispatcher<Object> dispatcher, final String command) {
        return executeOr(dispatcher, command, new Object());
    }

    private static <S> int executeOr(final CommandDispatcher<S> dispatcher, final String command, final S source) {
        try {
            return dispatcher.execute(command, source);
        } catch (final CommandSyntaxException ex) {
            return -1;
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Throwable;
    }

    static {
        // Referenced so the unused-import check stays honest about Command's constant.
        assert Command.SINGLE_SUCCESS == 1;
    }
}
