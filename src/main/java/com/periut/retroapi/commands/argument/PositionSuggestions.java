package com.periut.retroapi.commands.argument;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.periut.retroapi.commands.Position;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.commands.SuggestionHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Completions for a three-coordinate argument, following modern Minecraft's rule exactly: offer the
 * position one coordinate at a time, growing as each is filled in.
 *
 * <p>Which position is offered is the same choice modern's {@code CommandSource} makes. Looking at a
 * block completes to that block, so {@code /tp } offers the coordinates under the crosshair and
 * teleporting somewhere you can see costs no typing at all; looking at nothing - or at an entity,
 * which is not a position - falls back to {@code ~}, meaning where you already are.
 *
 * <p>Local ({@code ^}) coordinates are accepted by the parser but never suggested, which is also
 * what modern does - they are a specialist tool and would double the size of the list.
 */
final class PositionSuggestions {
    private static final String[] RELATIVE = {"~", "~", "~"};

    private PositionSuggestions() {
    }

    /**
     * The crosshair block belonging to whoever is completing. A server source never has one, and on
     * a dedicated server this runs for the console, so the absence has to be ordinary rather than
     * exceptional.
     */
    static Position lookedAtBlock(final CommandContext<?> context) {
        return context.getSource() instanceof RetroCommandSource source ? source.getLookedAtBlock() : null;
    }

    static CompletableFuture<Suggestions> suggest(final SuggestionsBuilder builder, final boolean blockPos, final Position lookedAtBlock) {
        final String[] target = coordinates(lookedAtBlock);
        final String remaining = builder.getRemaining();
        final List<String> candidates = new ArrayList<>(3);

        if (remaining.isEmpty()) {
            candidates.add(target[0]);
            candidates.add(target[0] + " " + target[1]);
            candidates.add(target[0] + " " + target[1] + " " + target[2]);
        } else {
            // Splitting without a negative limit drops the trailing empty piece, so "~ " counts as
            // one coordinate typed rather than two - joining an empty one back in would offer "~  ~".
            final String[] typed = remaining.split(" ");
            if (typed.length == 1) {
                candidates.add(typed[0] + " " + target[1]);
                candidates.add(typed[0] + " " + target[1] + " " + target[2]);
            } else if (typed.length == 2) {
                candidates.add(typed[0] + " " + typed[1] + " " + target[2]);
            }
        }

        return SuggestionHelper.suggestMatching(candidates, builder);
    }

    /** The looked-at block as whole numbers, or {@code ~ ~ ~} when there is nothing to look at. */
    private static String[] coordinates(final Position lookedAtBlock) {
        if (lookedAtBlock == null) {
            return RELATIVE;
        }
        return new String[]{
            String.valueOf(lookedAtBlock.blockX()),
            String.valueOf(lookedAtBlock.blockY()),
            String.valueOf(lookedAtBlock.blockZ()),
        };
    }
}
