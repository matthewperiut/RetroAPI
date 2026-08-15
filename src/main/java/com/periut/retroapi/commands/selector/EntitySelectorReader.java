package com.periut.retroapi.commands.selector;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.periut.retroapi.text.Text;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityRegistry;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Parses {@code @p}, {@code @a}, {@code @r}, {@code @e}, {@code @s} and their {@code [key=value]}
 * options, or a bare player name.
 *
 * <p>Suggestions are produced the way modern Minecraft does it: the reader keeps a handler that is
 * swapped out as parsing advances, so whatever it was looking at when input ran out is what gets
 * offered. Callers parse into a throwaway reader, ignore the failure, and ask it for suggestions.
 */
public class EntitySelectorReader {
    public static final char SELECTOR_PREFIX = '@';

    public static final DynamicCommandExceptionType UNKNOWN_SELECTOR = new DynamicCommandExceptionType(
        type -> Text.literal("Unknown selector type '" + type + "'"));
    public static final DynamicCommandExceptionType UNKNOWN_OPTION = new DynamicCommandExceptionType(
        option -> Text.literal("Unknown option '" + option + "'"));
    public static final SimpleCommandExceptionType MISSING_SELECTOR_TYPE = new SimpleCommandExceptionType(
        Text.literal("Missing selector type"));
    public static final SimpleCommandExceptionType EXPECTED_END_OF_OPTIONS = new SimpleCommandExceptionType(
        Text.literal("Expected end of options"));
    public static final SimpleCommandExceptionType SELECTOR_NOT_ALLOWED = new SimpleCommandExceptionType(
        Text.literal("Selector not allowed"));
    public static final DynamicCommandExceptionType INVALID_SORT = new DynamicCommandExceptionType(
        sort -> Text.literal("Invalid or unknown sort type '" + sort + "'"));
    public static final DynamicCommandExceptionType INVALID_ENTITY_TYPE = new DynamicCommandExceptionType(
        type -> Text.literal("Unknown entity type '" + type + "'"));

    private static final List<String> OPTIONS = List.of(
        "type", "name", "distance", "limit", "sort", "x", "y", "z", "dx", "dy", "dz");

    private final StringReader reader;
    private final boolean selectorsAllowed;

    private int limit = Integer.MAX_VALUE;
    private boolean playersOnly;
    private boolean senderOnly;
    private String playerName;
    private Sort sort = Sort.ARBITRARY;
    private boolean sortSet;
    private final List<Predicate<Entity>> predicates = new ArrayList<>();
    private Double offsetX;
    private Double offsetY;
    private Double offsetZ;
    private Double minDistance;
    private Double maxDistance;
    private Double boxX;
    private Double boxY;
    private Double boxZ;
    private boolean usesSelector;
    private int startCursor;

    private Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestions = this::suggestSelectorOrName;
    private Function<SuggestionsBuilder, CompletableFuture<Suggestions>> nameSuggestions = SuggestionsBuilder::buildFuture;

    public EntitySelectorReader(final StringReader reader, final boolean selectorsAllowed) {
        this.reader = reader;
        this.selectorsAllowed = selectorsAllowed;
    }

    public EntitySelector read() throws CommandSyntaxException {
        startCursor = reader.getCursor();

        if (reader.canRead() && reader.peek() == SELECTOR_PREFIX) {
            if (!selectorsAllowed) {
                throw SELECTOR_NOT_ALLOWED.createWithContext(reader);
            }
            usesSelector = true;
            reader.skip();
            readSelectorType();
        } else {
            readPlayerName();
        }

        return build();
    }

    private void readSelectorType() throws CommandSyntaxException {
        suggestions = this::suggestSelectorType;

        if (!reader.canRead()) {
            throw MISSING_SELECTOR_TYPE.createWithContext(reader);
        }

        final int start = reader.getCursor();
        final char type = reader.read();
        switch (type) {
            case 'p' -> {
                limit = 1;
                playersOnly = true;
                sort = Sort.NEAREST;
            }
            case 'a' -> playersOnly = true;
            case 'r' -> {
                limit = 1;
                playersOnly = true;
                sort = Sort.RANDOM;
            }
            case 'e' -> predicates.add(entity -> !entity.dead);
            case 's' -> {
                limit = 1;
                senderOnly = true;
            }
            default -> {
                reader.setCursor(start);
                throw UNKNOWN_SELECTOR.createWithContext(reader, "@" + type);
            }
        }

        suggestions = this::suggestOpenOptions;

        if (reader.canRead() && reader.peek() == '[') {
            reader.skip();
            suggestions = this::suggestOptionName;
            readOptions();
        }
    }

    private void readOptions() throws CommandSyntaxException {
        reader.skipWhitespace();

        while (reader.canRead() && reader.peek() != ']') {
            reader.skipWhitespace();
            final int optionStart = reader.getCursor();
            final String option = reader.readString();

            if (!OPTIONS.contains(option)) {
                reader.setCursor(optionStart);
                throw UNKNOWN_OPTION.createWithContext(reader, option);
            }

            reader.skipWhitespace();
            if (!reader.canRead() || reader.peek() != '=') {
                reader.setCursor(optionStart);
                suggestions = this::suggestOptionName;
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(reader, "=");
            }
            suggestions = builder -> suggestOptionValue(builder, option);
            reader.expect('=');
            reader.skipWhitespace();
            readOptionValue(option);
            reader.skipWhitespace();

            suggestions = this::suggestOptionName;
            if (reader.canRead() && reader.peek() == ',') {
                reader.skip();
                continue;
            }
            break;
        }

        if (!reader.canRead() || reader.peek() != ']') {
            throw EXPECTED_END_OF_OPTIONS.createWithContext(reader);
        }
        reader.skip();
        suggestions = builder -> Suggestions.empty();
    }

    private void readOptionValue(final String option) throws CommandSyntaxException {
        switch (option) {
            case "type" -> readType();
            case "name" -> readName();
            case "distance" -> readDistance();
            case "limit" -> limit = reader.readInt();
            case "sort" -> readSort();
            case "x" -> offsetX = reader.readDouble();
            case "y" -> offsetY = reader.readDouble();
            case "z" -> offsetZ = reader.readDouble();
            case "dx" -> boxX = reader.readDouble();
            case "dy" -> boxY = reader.readDouble();
            case "dz" -> boxZ = reader.readDouble();
            default -> throw UNKNOWN_OPTION.createWithContext(reader, option);
        }
    }

    private void readType() throws CommandSyntaxException {
        final boolean negated = readNegation();
        final int start = reader.getCursor();
        final String type = reader.readString();

        if ("player".equals(type)) {
            addPredicate(negated, entity -> entity instanceof PlayerEntity);
            if (!negated) {
                playersOnly = true;
            }
            return;
        }

        final Class<? extends Entity> entityClass = EntityRegistry.idToClass.get(type);
        if (entityClass == null) {
            reader.setCursor(start);
            throw INVALID_ENTITY_TYPE.createWithContext(reader, type);
        }
        addPredicate(negated, entityClass::isInstance);
    }

    private void readName() throws CommandSyntaxException {
        final boolean negated = readNegation();
        final String name = reader.readString();
        addPredicate(negated, entity -> name.equals(nameOf(entity)));
    }

    /** Modern's range syntax: {@code n}, {@code a..b}, {@code a..}, {@code ..b}. */
    private void readDistance() throws CommandSyntaxException {
        if (reader.canRead(2) && reader.peek() == '.' && reader.peek(1) == '.') {
            reader.skip();
            reader.skip();
            maxDistance = reader.readDouble();
            return;
        }

        final double first = reader.readDouble();
        if (reader.canRead(2) && reader.peek() == '.' && reader.peek(1) == '.') {
            reader.skip();
            reader.skip();
            minDistance = first;
            if (reader.canRead() && StringReader.isAllowedNumber(reader.peek())) {
                maxDistance = reader.readDouble();
            }
        } else {
            minDistance = first;
            maxDistance = first;
        }
    }

    private void readSort() throws CommandSyntaxException {
        final int start = reader.getCursor();
        final String value = reader.readUnquotedString();
        final Sort parsed = Sort.byName(value);
        if (parsed == null) {
            reader.setCursor(start);
            throw INVALID_SORT.createWithContext(reader, value);
        }
        sort = parsed;
        sortSet = true;
    }

    private boolean readNegation() {
        if (reader.canRead() && reader.peek() == '!') {
            reader.skip();
            return true;
        }
        return false;
    }

    private void addPredicate(final boolean negated, final Predicate<Entity> predicate) {
        predicates.add(negated ? predicate.negate() : predicate);
    }

    private void readPlayerName() throws CommandSyntaxException {
        final int start = reader.getCursor();
        final String name = reader.readString();
        if (name.isEmpty()) {
            reader.setCursor(start);
            throw EntitySelector.NOT_FOUND_PLAYER.createWithContext(reader);
        }
        playerName = name;
    }

    private EntitySelector build() {
        if (playerName != null) {
            return EntitySelector.ofName(playerName);
        }

        // An explicit limit with no explicit sort means "nearest first", as in modern.
        final Sort effectiveSort = !sortSet && limit != Integer.MAX_VALUE && !senderOnly && sort == Sort.ARBITRARY
            ? Sort.NEAREST
            : sort;

        Predicate<Entity> combined = entity -> true;
        for (final Predicate<Entity> predicate : predicates) {
            combined = combined.and(predicate);
        }

        return new EntitySelector(limit, playersOnly, senderOnly, null, combined, effectiveSort,
            offsetX, offsetY, offsetZ, minDistance, maxDistance, boxX, boxY, boxZ);
    }

    public boolean usesSelector() {
        return usesSelector;
    }

    /**
     * @param nameSuggestions how to offer plain player names, which only the caller knows how to
     *                        collect; used at the one point where a name is still a possibility
     */
    public CompletableFuture<Suggestions> listSuggestions(final SuggestionsBuilder builder,
                                                          final Function<SuggestionsBuilder, CompletableFuture<Suggestions>> nameSuggestions) {
        this.nameSuggestions = nameSuggestions;
        // The reader indexes the whole command, so its cursor is already an absolute offset.
        return suggestions.apply(builder.createOffset(reader.getCursor()));
    }

    private CompletableFuture<Suggestions> suggestSelectorOrName(final SuggestionsBuilder builder) {
        // Whatever has been typed so far is part of the name or selector, so completions replace
        // it rather than following it - otherwise "Pla" plus Tab would read "PlaPlayer".
        final SuggestionsBuilder offset = builder.createOffset(startCursor);
        if (selectorsAllowed) {
            suggestSelectors(offset);
        }
        return nameSuggestions.apply(offset);
    }

    private CompletableFuture<Suggestions> suggestSelectorType(final SuggestionsBuilder builder) {
        // The '@' and its letter are already consumed; offering from the '@' lets one selector
        // replace another.
        final SuggestionsBuilder offset = builder.createOffset(startCursor);
        suggestSelectors(offset);
        return offset.buildFuture();
    }

    private static void suggestSelectors(final SuggestionsBuilder builder) {
        suggest(builder, "@p", "Nearest player");
        suggest(builder, "@a", "All players");
        suggest(builder, "@r", "Random player");
        suggest(builder, "@s", "Current entity");
        suggest(builder, "@e", "All entities");
    }

    private static void suggest(final SuggestionsBuilder builder, final String text, final String tooltip) {
        if (text.startsWith(builder.getRemainingLowerCase())) {
            builder.suggest(text, Text.literal(tooltip));
        }
    }

    private CompletableFuture<Suggestions> suggestOpenOptions(final SuggestionsBuilder builder) {
        if (builder.getRemaining().isEmpty()) {
            builder.suggest("[");
        }
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestOptionName(final SuggestionsBuilder builder) {
        for (final String option : OPTIONS) {
            if (option.startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(option + "=");
            }
        }
        if ("]".startsWith(builder.getRemaining())) {
            builder.suggest("]");
        }
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestOptionValue(final SuggestionsBuilder builder, final String option) {
        switch (option) {
            case "sort" -> {
                for (final Sort value : Sort.values()) {
                    if (value.getName().startsWith(builder.getRemainingLowerCase())) {
                        builder.suggest(value.getName());
                    }
                }
            }
            case "type" -> {
                for (final String type : entityTypeIds()) {
                    if (type.toLowerCase(Locale.ROOT).startsWith(builder.getRemainingLowerCase())) {
                        builder.suggest(type);
                    }
                }
            }
            default -> {
            }
        }
        return builder.buildFuture();
    }

    /** Every summonable id plus {@code player}, which beta does not keep in the registry. */
    public static List<String> entityTypeIds() {
        final List<String> ids = new ArrayList<>(EntityRegistry.idToClass.keySet());
        ids.add("player");
        ids.sort(String::compareTo);
        return ids;
    }

    public static String nameOf(final Entity entity) {
        if (entity instanceof PlayerEntity player) {
            return player.name;
        }
        final String id = EntityRegistry.getId(entity);
        return id == null ? entity.getClass().getSimpleName() : id;
    }

    public enum Sort {
        ARBITRARY("arbitrary"),
        NEAREST("nearest"),
        FURTHEST("furthest"),
        RANDOM("random");

        private final String name;

        Sort(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static Sort byName(final String name) {
            for (final Sort sort : values()) {
                if (sort.name.equals(name)) {
                    return sort;
                }
            }
            return null;
        }
    }
}
