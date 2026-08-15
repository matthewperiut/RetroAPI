package com.periut.retroapi.commands.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.periut.retroapi.commands.SuggestionHelper;
import com.periut.retroapi.text.Text;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** {@code <entity>} for {@code /summon}: a registered entity id such as {@code Creeper}. */
public class EntitySummonArgumentType implements ArgumentType<String> {
    private static final Collection<String> EXAMPLES = Arrays.asList("Pig", "Creeper");

    public static final DynamicCommandExceptionType UNKNOWN_ENTITY = new DynamicCommandExceptionType(
        id -> Text.literal("Unknown entity type '" + id + "'"));

    private EntitySummonArgumentType() {
    }

    public static EntitySummonArgumentType entitySummon() {
        return new EntitySummonArgumentType();
    }

    public static String getEntitySummon(final CommandContext<?> context, final String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(final StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        final String id = reader.readUnquotedString();
        final String resolved = resolve(id);
        if (resolved == null) {
            reader.setCursor(start);
            throw UNKNOWN_ENTITY.createWithContext(reader, id);
        }
        return resolved;
    }

    /**
     * Beta's entity ids are capitalised ({@code Creeper}, {@code PrimedTnt}); matching them
     * case-insensitively lets a player type them the way every other id in the game is typed.
     */
    public static String resolve(final String id) {
        if (EntityRegistry.idToClass.containsKey(id)) {
            return id;
        }
        for (final String candidate : EntityRegistry.idToClass.keySet()) {
            if (candidate.equalsIgnoreCase(id)) {
                return candidate;
            }
        }
        return null;
    }

    public static Class<? extends Entity> classOf(final String id) {
        return EntityRegistry.idToClass.get(id);
    }

    public static List<String> summonableIds() {
        final List<String> ids = new ArrayList<>(EntityRegistry.idToClass.keySet());
        ids.sort(String.CASE_INSENSITIVE_ORDER);
        return ids;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return SuggestionHelper.suggestMatching(summonableIds(), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
