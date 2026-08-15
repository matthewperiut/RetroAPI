package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.commands.SuggestionHelper;
import com.periut.retroapi.gamerule.RetroGameRule;
import com.periut.retroapi.gamerule.RetroGameRules;
import com.periut.retroapi.text.Formatting;
import com.periut.retroapi.text.Text;

import java.util.List;

import static com.periut.retroapi.commands.RetroCommandManager.argument;
import static com.periut.retroapi.commands.RetroCommandManager.literal;

/**
 * {@code /gamerule [rule] [value]}, in modern's three shapes: no arguments lists everything, a rule
 * on its own reports its value, and a rule with a value sets it.
 *
 * <p>Listing every rule is not something modern does - it prints usage instead - but beta has no
 * F3 screen and no wiki tab, and a rule nobody can enumerate is a rule nobody finds.
 */
public final class GameRuleCommand {
    private GameRuleCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("gamerule")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
            .executes(GameRuleCommand::list)
            .then(argument("rule", StringArgumentType.word())
                .suggests((context, builder) -> SuggestionHelper.suggestMatching(RetroGameRules.keys(), builder))
                .executes(GameRuleCommand::query)
                .then(argument("value", StringArgumentType.greedyString())
                    .suggests(GameRuleCommand::suggestValues)
                    .executes(GameRuleCommand::set))));
    }

    private static int list(final CommandContext<RetroCommandSource> context) {
        final RetroCommandSource source = context.getSource();
        source.sendFeedback(Text.literal("Game rules:").formatted(Formatting.YELLOW));
        for (final RetroGameRule rule : RetroGameRules.all()) {
            source.sendFeedback(Text.literal(" " + rule.getKey() + " = ")
                .append(Text.literal(RetroGameRules.getString(rule)).formatted(Formatting.GREEN)));
        }
        return RetroGameRules.all().size();
    }

    private static int query(final CommandContext<RetroCommandSource> context) {
        final RetroCommandSource source = context.getSource();
        final String key = StringArgumentType.getString(context, "rule");
        final RetroGameRule rule = RetroGameRules.get(key);
        if (rule == null) {
            source.sendError(Text.literal("No such game rule: " + key));
            return 0;
        }

        final String value = RetroGameRules.getString(rule);
        source.sendFeedback(Text.literal(key + " = ").append(Text.literal(value).formatted(Formatting.GREEN)));
        return Command.SINGLE_SUCCESS;
    }

    private static int set(final CommandContext<RetroCommandSource> context) {
        final RetroCommandSource source = context.getSource();
        final String key = StringArgumentType.getString(context, "rule");
        final String value = StringArgumentType.getString(context, "value").trim();

        final RetroGameRule rule = RetroGameRules.get(key);
        if (rule == null) {
            source.sendError(Text.literal("No such game rule: " + key));
            return 0;
        }
        if (!rule.accepts(value)) {
            source.sendError(Text.literal(rule.getType() == RetroGameRule.Type.BOOLEAN
                ? "Expected true or false"
                : "Expected a whole number"));
            return 0;
        }
        if (!RetroGameRules.set(key, value)) {
            // The only other way set() refuses: this game is a client and the server owns the rules.
            source.sendError(Text.literal("Game rules are set by the server"));
            return 0;
        }

        source.sendFeedback(Text.literal("Game rule " + key + " is now ")
            .append(Text.literal(RetroGameRules.getString(rule)).formatted(Formatting.GREEN)));
        return Command.SINGLE_SUCCESS;
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestValues(
            final CommandContext<RetroCommandSource> context, final com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        final RetroGameRule rule = RetroGameRules.get(StringArgumentType.getString(context, "rule"));
        if (rule != null && rule.getType() == RetroGameRule.Type.BOOLEAN) {
            return SuggestionHelper.suggestMatching(List.of("true", "false"), builder);
        }
        return builder.buildFuture();
    }
}
