package com.periut.retroapi.commands;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/** The prefix-matching helpers modern Minecraft keeps on {@code CommandSource}. */
public final class SuggestionHelper {
    private SuggestionHelper() {
    }

    public static CompletableFuture<Suggestions> suggestMatching(final Iterable<String> candidates, final SuggestionsBuilder builder) {
        final String remaining = builder.getRemainingLowerCase();
        for (final String candidate : candidates) {
            if (matches(remaining, candidate)) {
                builder.suggest(candidate);
            }
        }
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> suggestMatching(final Iterable<String> candidates, final SuggestionsBuilder builder, final Function<String, Message> tooltips) {
        final String remaining = builder.getRemainingLowerCase();
        for (final String candidate : candidates) {
            if (matches(remaining, candidate)) {
                builder.suggest(candidate, tooltips.apply(candidate));
            }
        }
        return builder.buildFuture();
    }

    /**
     * Completes namespaced ids.
     *
     * <p>With nothing typed yet, the namespaces themselves are offered - one Tab gets you past
     * {@code minecraft:} and the list then narrows to that namespace, rather than making you type
     * the prefix in full every time.
     *
     * <p>Once something is typed, ids also match on their path alone, so {@code sto} finds
     * {@code minecraft:stone} without a namespace at all. Typing a colon switches to matching the
     * whole id, which is what confines the list to one namespace.
     */
    public static CompletableFuture<Suggestions> suggestIdentifiers(final Iterable<String> identifiers, final SuggestionsBuilder builder) {
        final String remaining = builder.getRemainingLowerCase();
        final boolean typedNamespace = remaining.indexOf(':') >= 0;

        if (!typedNamespace) {
            final Set<String> namespaces = new TreeSet<>();
            for (final String identifier : identifiers) {
                final int separator = identifier.indexOf(':');
                if (separator > 0) {
                    final String namespace = identifier.substring(0, separator);
                    if (namespace.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                        namespaces.add(namespace + ":");
                    }
                }
            }
            for (final String namespace : namespaces) {
                builder.suggest(namespace);
            }
        }

        for (final String identifier : identifiers) {
            final String lower = identifier.toLowerCase(Locale.ROOT);
            if (typedNamespace) {
                if (lower.startsWith(remaining)) {
                    builder.suggest(identifier);
                }
            } else if (!remaining.isEmpty()) {
                final int separator = lower.indexOf(':');
                final String path = separator < 0 ? lower : lower.substring(separator + 1);
                if (path.startsWith(remaining)) {
                    builder.suggest(identifier);
                }
            }
        }

        return builder.buildFuture();
    }

    private static boolean matches(final String remaining, final String candidate) {
        return candidate.toLowerCase(Locale.ROOT).startsWith(remaining);
    }

    /**
     * Whether the command as typed already parses in full, with nothing left over and something to
     * run - in other words, whether pressing Enter now would work.
     *
     * <p>The completion window arms its highlighted entry, so while it is open Enter completes
     * instead of sending. Once the command is finished that makes the finished form unreachable, and
     * Brigadier is what sharpens it into a trap: {@link SuggestionsBuilder#suggest} drops any
     * candidate equal to what is already typed, so after {@code minecraft:stick} the only entry left
     * is {@code minecraft:sticky_piston} - Enter would complete to the longer id every time and a
     * plain stick could never be asked for. So the answer cannot come from the suggestions, which no
     * longer mention the finished form; it comes from the parse.
     */
    public static boolean isComplete(final ParseResults<?> parse) {
        return parse != null
            && !parse.getReader().canRead()
            && parse.getContext().getLastChild().getCommand() != null;
    }
}
