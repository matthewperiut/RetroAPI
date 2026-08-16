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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * {@code <entity>} for {@code /summon}: an entity identifier such as {@code minecraft:creeper} or
 * {@code mymod:moa}.
 *
 * <p>The value the argument yields is beta's own registry word ({@code Creeper}), because that is what
 * spawns something; the identifier is the spelling, not the storage. See {@link EntityIds}.
 */
public class EntitySummonArgumentType implements ArgumentType<String> {
    private static final Collection<String> EXAMPLES = Arrays.asList("minecraft:pig", "minecraft:creeper");

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
        final String token = readToken(reader);
        final String resolved = EntityIds.resolve(token);
        if (resolved == null) {
            reader.setCursor(start);
            throw UNKNOWN_ENTITY.createWithContext(reader, token);
        }
        return resolved;
    }

    /**
     * Brigadier's own unquoted-string reader stops at a colon, which is what made a suggested
     * {@code mymod:moa} unusable the moment it was accepted: the parse saw {@code mymod}, found no
     * such entity, and failed on an id the completion had just offered. Every other namespaced
     * argument here reads its token the same way (see {@link ItemArgumentType}).
     */
    private static String readToken(final StringReader reader) {
        final int start = reader.getCursor();
        while (reader.canRead() && (StringReader.isAllowedInUnquotedString(reader.peek()) || reader.peek() == ':')) {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    public static Class<? extends Entity> classOf(final String betaId) {
        return EntityIds.classOf(betaId);
    }

    public static List<String> summonableIds() {
        return EntityIds.allIdentifiers();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return SuggestionHelper.suggestIdentifiers(EntityIds.allIdentifiers(), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
