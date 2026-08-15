package com.periut.retroapi.commands.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.commands.SuggestionHelper;
import com.periut.retroapi.commands.selector.EntitySelector;
import com.periut.retroapi.commands.selector.EntitySelectorReader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * {@code <targets>} - a selector or a player name.
 *
 * <p>The four factories mirror modern Minecraft's: {@code player()} and {@code entity()} refuse a
 * selector that could match more than one, {@code players()} and {@code entities()} allow many.
 */
public class EntityArgumentType implements ArgumentType<EntitySelector> {
    private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "@e", "@e[type=cow]");

    private final boolean singleTarget;
    private final boolean playersOnly;

    private EntityArgumentType(final boolean singleTarget, final boolean playersOnly) {
        this.singleTarget = singleTarget;
        this.playersOnly = playersOnly;
    }

    public static EntityArgumentType entity() {
        return new EntityArgumentType(true, false);
    }

    public static EntityArgumentType entities() {
        return new EntityArgumentType(false, false);
    }

    public static EntityArgumentType player() {
        return new EntityArgumentType(true, true);
    }

    public static EntityArgumentType players() {
        return new EntityArgumentType(false, true);
    }

    public boolean isSingleTarget() {
        return singleTarget;
    }

    public boolean isPlayersOnly() {
        return playersOnly;
    }

    public static Entity getEntity(final CommandContext<RetroCommandSource> context, final String name) throws CommandSyntaxException {
        return context.getArgument(name, EntitySelector.class).getEntity(context.getSource());
    }

    public static List<? extends Entity> getEntities(final CommandContext<RetroCommandSource> context, final String name) throws CommandSyntaxException {
        return context.getArgument(name, EntitySelector.class).getEntities(context.getSource());
    }

    public static PlayerEntity getPlayer(final CommandContext<RetroCommandSource> context, final String name) throws CommandSyntaxException {
        return context.getArgument(name, EntitySelector.class).getPlayer(context.getSource());
    }

    public static List<PlayerEntity> getPlayers(final CommandContext<RetroCommandSource> context, final String name) throws CommandSyntaxException {
        return context.getArgument(name, EntitySelector.class).getPlayers(context.getSource());
    }

    @Override
    public EntitySelector parse(final StringReader reader) throws CommandSyntaxException {
        final EntitySelectorReader selectorReader = new EntitySelectorReader(reader, true);
        final EntitySelector selector = selectorReader.read();

        if (singleTarget && !selector.isSingleTarget()) {
            throw playersOnly ? EntitySelector.TOO_MANY_PLAYERS.createWithContext(reader) : EntitySelector.TOO_MANY_ENTITIES.createWithContext(reader);
        }
        if (playersOnly && selector.includesNonPlayers()) {
            throw EntitySelector.PLAYER_SELECTOR_HAS_ENTITIES.createWithContext(reader);
        }

        return selector;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        if (!(context.getSource() instanceof RetroCommandSource source)) {
            return Suggestions.empty();
        }

        // Parse a throwaway copy purely to find out what the reader was looking at when it ran out
        // of input; the failure itself is expected and uninteresting.
        final StringReader reader = new StringReader(builder.getInput());
        reader.setCursor(builder.getStart());
        final EntitySelectorReader selectorReader = new EntitySelectorReader(reader, true);
        try {
            selectorReader.read();
        } catch (final CommandSyntaxException ignored) {
        }

        return selectorReader.listSuggestions(builder, names -> SuggestionHelper.suggestMatching(source.getPlayerNames(), names));
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
