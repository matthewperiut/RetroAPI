package com.periut.retroapi.commands.test;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.commands.argument.CoordinateArgument;
import com.periut.retroapi.commands.argument.DefaultPosArgument;
import com.periut.retroapi.commands.argument.GameModeArgumentType;
import com.periut.retroapi.gamemode.RetroGameMode;
import com.periut.retroapi.commands.argument.ItemIds;
import com.periut.retroapi.commands.argument.TimeArgumentType;
import com.periut.retroapi.commands.argument.EntityNames;
import com.periut.retroapi.commands.argument.Snbt;
import com.periut.retroapi.commands.argument.VanillaEntityIds;
import com.periut.retroapi.commands.argument.VanillaIds;
import com.periut.retroapi.commands.network.CommandTreeSerializer;
import net.ornithemc.osl.networking.api.PacketBuffer;
import net.ornithemc.osl.networking.api.PacketBuffers;

import java.util.Arrays;
import java.util.List;

/**
 * Covers the parts of the command framework that stand on their own: identifier resolution,
 * coordinate and unit parsing, and the tree serialization multiplayer depends on.
 *
 * <p>Nothing here touches a world or an entity, which is what lets the suite run outside the game.
 */
public final class CommandTest {
    private CommandTest() {
    }

    public static void run() {
        vanillaIds();
        vanillaEntityIds();
        entityNames();
        snbt();
        coordinates();
        units();
        treeRoundTrip();
        slashOffsets();
        targetSuggestions();
        positionSuggestions();
        identifierSuggestions();
        completedSuggestions();
        gameRuleSuggestions();
        displayNames();
    }

    /**
     * A completion has to replace the token being typed, not follow it. Getting this wrong turns
     * "Pla" plus Tab into "PlaPlayer", and stops the list being filtered by what was typed at all.
     */
    private static void targetSuggestions() {
        Tests.group("target completions");

        final CommandDispatcher<RetroCommandSource> dispatcher = new CommandDispatcher<>();
        dispatcher.register(LiteralArgumentBuilder.<RetroCommandSource>literal("tp")
            .then(RequiredArgumentBuilder.<RetroCommandSource, com.periut.retroapi.commands.selector.EntitySelector>argument(
                "targets", com.periut.retroapi.commands.argument.EntityArgumentType.players())
                .executes(context -> 1)));

        final RetroCommandSource source = sourceWithPlayers("Player", "Placeholder", "Steve");

        Tests.eq("an empty target offers every name and selector",
            List.of("@a", "@e", "@p", "@r", "@s", "Placeholder", "Player", "Steve"),
            suggestionTexts(dispatcher, source, "/tp "));

        // The heart of it: a partial name filters, and applying the result replaces that name.
        final List<String> partial = suggestionTexts(dispatcher, source, "/tp Pla");
        Tests.eq("a partial name filters the list", List.of("Placeholder", "Player"), partial);
        Tests.eq("applying a name replaces what was typed", "/tp Player",
            applyFirstMatching(dispatcher, source, "/tp Pla", "Player"));

        // The same rule applies to a half-typed selector.
        Tests.eq("applying a selector replaces the whole selector", "/tp @a",
            applyFirstMatching(dispatcher, source, "/tp @", "@a"));
    }


    /**
     * Typing the first letter of a rule must NARROW the list, not empty it.
     */
    private static void gameRuleSuggestions() {
        Tests.group("gamerule completions");

        final CommandDispatcher<RetroCommandSource> dispatcher = new CommandDispatcher<>();
        com.periut.retroapi.commands.builtin.GameRuleCommand.register(dispatcher,
            com.periut.retroapi.commands.RegistrationEnvironment.INTEGRATED);

        final RetroCommandSource source = sourceWithPlayers("Player");

        Tests.check("an empty rule offers the whole list",
            suggestionTexts(dispatcher, source, "/gamerule ").contains("sprinting"));
        Tests.eq("typing a letter narrows the list instead of emptying it",
            List.of("sendCommandFeedback", "sprinting", "swimming"),
            suggestionTexts(dispatcher, source, "/gamerule s"));
        Tests.eq("a value is offered once the rule is known",
            List.of("false", "true"),
            suggestionTexts(dispatcher, source, "/gamerule sprinting "));
    }

    /**
     * A display name has to appear on both sides. The lang file ships with the client and not the
     * server, so the last step of the chain derives the name from the identifier and needs no table.
     */
    private static void displayNames() {
        Tests.group("display names");

        Tests.eq("spelling out an identifier", "Iron Ingot", com.periut.retroapi.commands.argument.ItemNames.spellOut("iron_ingot"));
        Tests.eq("a single word", "Apple", com.periut.retroapi.commands.argument.ItemNames.spellOut("apple"));

        // These agree whether the name came from the lang file or from the identifier, so the
        // assertion holds on a server with no lang file at all.
        Tests.eq("a block", "Stone", com.periut.retroapi.commands.argument.ItemNames.displayName(1, 0));
        Tests.eq("an item", "Apple", com.periut.retroapi.commands.argument.ItemNames.displayName(260, 0));
        Tests.eq("a two-word item", "Iron Ingot", com.periut.retroapi.commands.argument.ItemNames.displayName(265, 0));

        // Beta's lang file has one entry for all sixteen wools, so only the mod can name this.
        Tests.eq("a subtype names itself", "Red Wool", com.periut.retroapi.commands.argument.ItemNames.displayName(35, 14));
        Tests.eq("subtype zero uses the plain name", "Wool", com.periut.retroapi.commands.argument.ItemNames.displayName(35, 0));
        Tests.eq("a dye subtype", "Bone Meal", com.periut.retroapi.commands.argument.ItemNames.displayName(351, 15));

        // Whatever the source, every vanilla id must produce something presentable.
        for (final String name : VanillaIds.names()) {
            final VanillaIds.VanillaItem entry = VanillaIds.byName(name);
            final String display = com.periut.retroapi.commands.argument.ItemNames.displayName(entry.id(), 0);
            Tests.check("id " + entry.id() + " has a readable name",
                display != null && !display.isEmpty() && display.indexOf('_') < 0 && display.indexOf(':') < 0);
        }

        // Step two of the chain: the lang file shipped in the client jar, read directly. It is on
        // this suite's classpath, so its absence here would mean the reader had broken.
        Tests.eq("a lang key resolves when the file is present", "Raw Porkchop",
            com.periut.retroapi.text.Translations.find("item.porkchopRaw.name"));
        Tests.eq("the lang file has one name for every wool", "Wool",
            com.periut.retroapi.text.Translations.find("tile.cloth.name"));

        Tests.check("an unknown key is reported as unknown",
            com.periut.retroapi.text.Translations.find("retroapi.no.such.key") == null);
        Tests.eq("an unknown key falls back", "fallback",
            com.periut.retroapi.text.Translations.get("retroapi.no.such.key", "fallback"));
    }

    /** Namespaces come first so one Tab gets past {@code minecraft:}. */
    private static void identifierSuggestions() {
        Tests.group("identifier completions");

        final List<String> ids = List.of("minecraft:stone", "minecraft:stone_axe", "minecraft:iron_ingot", "somemod:widget");

        Tests.eq("nothing typed offers the namespaces alone",
            List.of("minecraft:", "somemod:"), suggestIdentifiers(ids, ""));
        Tests.eq("a namespace prefix narrows to that namespace",
            List.of("minecraft:"), suggestIdentifiers(ids, "mine"));
        Tests.eq("a colon lists that namespace's ids",
            List.of("minecraft:stone", "minecraft:stone_axe"), suggestIdentifiers(ids, "minecraft:sto"));
        Tests.eq("a bare path still matches without a namespace",
            List.of("minecraft:stone", "minecraft:stone_axe"), suggestIdentifiers(ids, "sto"));
        Tests.eq("a path from another namespace matches too",
            List.of("somemod:widget"), suggestIdentifiers(ids, "widg"));
    }

    /**
     * The case that made {@code minecraft:stick} unreachable. Brigadier drops a suggestion equal to
     * what is typed, so once the id is complete the only entry left is {@code minecraft:sticky_piston}
     * - with the window open Enter completed to that instead of sending, and a stick could not be
     * asked for at all. Whether the window should still be open therefore has to come from the parse.
     */
    private static void completedSuggestions() {
        Tests.group("completion is finished");

        // An argument that rejects what it does not recognise, which is what makes a half-typed id
        // parse differently from a finished one - StringArgumentType would accept either.
        final CommandDispatcher<RetroCommandSource> dispatcher = new CommandDispatcher<>();
        dispatcher.register(LiteralArgumentBuilder.<RetroCommandSource>literal("give")
            .then(RequiredArgumentBuilder.<RetroCommandSource, String>argument("item", new KnownWord("stick", "sticky_piston"))
                .executes(context -> 1)));

        Tests.check("a finished command is complete", isComplete(dispatcher, "give stick"));
        Tests.check("the longer id is complete too", isComplete(dispatcher, "give sticky_piston"));
        Tests.check("a half-typed argument is not", !isComplete(dispatcher, "give sti"));
        Tests.check("the literal alone is not, it cannot run", !isComplete(dispatcher, "give"));
        Tests.check("trailing text left over is not", !isComplete(dispatcher, "give stick extra"));

        // The premise the rule rests on: Brigadier really does drop the finished form, so asking the
        // suggestion list "is one of these what I typed" could only ever answer no.
        final com.mojang.brigadier.suggestion.SuggestionsBuilder builder =
            new com.mojang.brigadier.suggestion.SuggestionsBuilder("minecraft:stick", 0);
        Tests.eq("brigadier drops the suggestion equal to what is typed",
            List.of("minecraft:sticky_piston"),
            com.periut.retroapi.commands.SuggestionHelper.suggestIdentifiers(
                List.of("minecraft:stick", "minecraft:sticky_piston"), builder).join()
                .getList().stream().map(com.mojang.brigadier.suggestion.Suggestion::getText)
                .collect(java.util.stream.Collectors.toList()));
    }

    /** Accepts only the words it was given, so an unfinished id fails to parse the way a real one does. */
    private record KnownWord(String... known) implements com.mojang.brigadier.arguments.ArgumentType<String> {
        @Override
        public String parse(final StringReader reader) throws CommandSyntaxException {
            final String word = reader.readUnquotedString();
            if (!Arrays.asList(known).contains(word)) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader);
            }
            return word;
        }
    }

    private static boolean isComplete(final CommandDispatcher<RetroCommandSource> dispatcher, final String command) {
        return com.periut.retroapi.commands.SuggestionHelper.isComplete(
            dispatcher.parse(new StringReader(command), sourceWithPlayers("Player")));
    }

    private static List<String> suggestIdentifiers(final List<String> ids, final String typed) {
        final com.mojang.brigadier.suggestion.SuggestionsBuilder builder =
            new com.mojang.brigadier.suggestion.SuggestionsBuilder(typed, 0);
        return com.periut.retroapi.commands.SuggestionHelper.suggestIdentifiers(ids, builder).join()
            .getList().stream().map(com.mojang.brigadier.suggestion.Suggestion::getText).sorted()
            .collect(java.util.stream.Collectors.toList());
    }

    private static void positionSuggestions() {
        Tests.group("position completions");

        final CommandDispatcher<RetroCommandSource> dispatcher = new CommandDispatcher<>();
        dispatcher.register(LiteralArgumentBuilder.<RetroCommandSource>literal("tp")
            .then(RequiredArgumentBuilder.<RetroCommandSource, com.periut.retroapi.commands.argument.PosArgument>argument(
                "location", com.periut.retroapi.commands.argument.Vec3ArgumentType.vec3())
                .executes(context -> 1)));

        final RetroCommandSource source = sourceWithPlayers();

        // Modern grows the position one coordinate at a time, and never offers local coordinates.
        Tests.eq("an empty position offers one, two and three coordinates",
            List.of("~", "~ ~", "~ ~ ~"), suggestionTexts(dispatcher, source, "/tp "));
        Tests.eq("one coordinate typed offers the rest",
            List.of("10 ~", "10 ~ ~"), suggestionTexts(dispatcher, source, "/tp 10"));
        Tests.eq("two coordinates typed offer the last",
            List.of("10 20 ~"), suggestionTexts(dispatcher, source, "/tp 10 20"));
        Tests.eq("applying keeps what was already typed", "/tp 10 ~ ~",
            applyFirstMatching(dispatcher, source, "/tp 10", "10 ~ ~"));

        // A trailing space must not count as an empty coordinate, or the offer reads "~  ~".
        Tests.eq("a trailing space offers the next coordinate",
            List.of("~ ~", "~ ~ ~"), suggestionTexts(dispatcher, source, "/tp ~ "));
        Tests.eq("two coordinates and a space offer the last",
            List.of("10 20 ~"), suggestionTexts(dispatcher, source, "/tp 10 20 "));
        Tests.check("no offer ever contains a double space",
            suggestionTexts(dispatcher, source, "/tp ~ ").stream().noneMatch(s -> s.contains("  ")));

        // Looking at a block completes to that block instead, so teleporting somewhere you can see
        // costs no typing - the same swap modern's ClientCommandSource makes.
        final RetroCommandSource looking = source.withLookedAtBlock(
            new com.periut.retroapi.commands.Position(10.0, 64.0, -30.0));

        Tests.eq("looking at a block offers its coordinates",
            List.of("10", "10 64", "10 64 -30"), suggestionTexts(dispatcher, looking, "/tp "));
        Tests.eq("a typed coordinate keeps the rest from the block",
            List.of("5 64", "5 64 -30"), suggestionTexts(dispatcher, looking, "/tp 5"));
        Tests.eq("the block's coordinates are whole numbers, not 10.0",
            List.of("10 64 -30"), suggestionTexts(dispatcher, looking, "/tp 10 64"));

        // Not looking at a block is the ordinary case, and has to stay ~.
        Tests.eq("looking at nothing still offers relative coordinates",
            List.of("~", "~ ~", "~ ~ ~"), suggestionTexts(dispatcher, source.withLookedAtBlock(null), "/tp "));
    }

    private static List<String> suggestionTexts(final CommandDispatcher<RetroCommandSource> dispatcher,
                                                final RetroCommandSource source, final String typed) {
        return suggestionsFor(dispatcher, source, typed).getList().stream()
            .map(com.mojang.brigadier.suggestion.Suggestion::getText).sorted().collect(java.util.stream.Collectors.toList());
    }

    private static String applyFirstMatching(final CommandDispatcher<RetroCommandSource> dispatcher,
                                             final RetroCommandSource source, final String typed, final String wanted) {
        for (final com.mojang.brigadier.suggestion.Suggestion suggestion : suggestionsFor(dispatcher, source, typed).getList()) {
            if (suggestion.getText().equals(wanted)) {
                return suggestion.apply(typed);
            }
        }
        return "<no suggestion named " + wanted + ">";
    }

    private static com.mojang.brigadier.suggestion.Suggestions suggestionsFor(
        final CommandDispatcher<RetroCommandSource> dispatcher, final RetroCommandSource source, final String typed) {
        final StringReader reader = new StringReader(typed);
        reader.skip();
        return dispatcher.getCompletionSuggestions(dispatcher.parse(reader, source), typed.length()).join();
    }

    /** A source with no world behind it; only the player names matter for completions. */
    private static RetroCommandSource sourceWithPlayers(final String... names) {
        return new RetroCommandSource(message -> {
        }, null, null, com.periut.retroapi.commands.Position.ORIGIN, 0.0f, 0.0f, "test",
            RetroCommandSource.LEVEL_OWNER, null, true) {
            @Override
            public List<String> getPlayerNames() {
                return List.of(names);
            }
        };
    }

    /**
     * The chat screen parses the text with its leading slash still attached, moving the reader past
     * it rather than cutting it off, so that every range Brigadier reports can be applied straight
     * back to what the player typed. Getting this wrong shifts completions by one character.
     */
    private static void slashOffsets() {
        Tests.group("slash offsets");

        final CommandDispatcher<RetroCommandSource> dispatcher = new CommandDispatcher<>();
        dispatcher.register(LiteralArgumentBuilder.<RetroCommandSource>literal("gamemode")
            .then(RequiredArgumentBuilder.<RetroCommandSource, String>argument("mode", com.mojang.brigadier.arguments.StringArgumentType.word())
                .suggests((context, builder) -> {
                    for (final String mode : List.of("survival", "creative")) {
                        if (mode.startsWith(builder.getRemainingLowerCase())) {
                            builder.suggest(mode);
                        }
                    }
                    return builder.buildFuture();
                })
                .executes(context -> 1)));

        final String typed = "/gamemode cr";
        final StringReader reader = new StringReader(typed);
        reader.skip();
        final var parse = dispatcher.parse(reader, null);

        final var suggestions = dispatcher.getCompletionSuggestions(parse, typed.length()).join();
        Tests.eq("one completion offered", 1, suggestions.getList().size());
        Tests.eq("the range covers the partial word only", 10, suggestions.getRange().getStart());
        Tests.eq("applying it rebuilds the whole command", "/gamemode creative", suggestions.getList().get(0).apply(typed));

        // The command word itself must be completable too, with the slash left in place.
        final String partial = "/gamem";
        final StringReader partialReader = new StringReader(partial);
        partialReader.skip();
        final var commandSuggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(partialReader, null), partial.length()).join();
        Tests.eq("the command name completes", "/gamemode", commandSuggestions.getList().get(0).apply(partial));
        Tests.eq("its range starts after the slash", 1, commandSuggestions.getRange().getStart());

        // An argument's range is what the highlighter colours; it must index the typed text.
        final var arguments = parse.getContext().getLastChild().getArguments();
        Tests.check("the argument range indexes the typed text",
            arguments.isEmpty() || "cr".equals(typed.substring(
                arguments.values().iterator().next().getRange().getStart(),
                arguments.values().iterator().next().getRange().getEnd())));
    }

    private static void vanillaIds() {
        Tests.group("item identifiers");

        Tests.eq("namespaced name resolves", 1, ItemIds.resolve("minecraft:stone").id());
        Tests.eq("bare name resolves", 1, ItemIds.resolve("stone").id());
        Tests.eq("resolution ignores case", 1, ItemIds.resolve("MINECRAFT:Stone").id());
        Tests.eq("a raw beta id still works", 264, ItemIds.resolve("264").id());
        Tests.check("an unknown name resolves to nothing", ItemIds.resolve("minecraft:elytra") == null);
        Tests.check("an unknown namespace resolves to nothing", ItemIds.resolve("nosuchmod:thing") == null);

        // Items win a name they share with a block, because that is the id that yields a usable stack.
        Tests.eq("wheat is the item, not the crop", 296, ItemIds.resolve("wheat").id());
        Tests.eq("the crop is still reachable", 59, ItemIds.resolve("wheat_crop").id());
        Tests.eq("bed is the item", 355, ItemIds.resolve("bed").id());

        // Modern names for subtypes beta stores as damage values.
        Tests.eq("red wool is wool", 35, ItemIds.resolve("red_wool").id());
        Tests.eq("red wool carries its subtype", 14, ItemIds.resolve("red_wool").meta());
        Tests.eq("bone meal is dye 15", 15, ItemIds.resolve("bone_meal").meta());
        Tests.eq("spruce log is a log subtype", 1, ItemIds.resolve("spruce_log").meta());

        // Older beta names still resolve, so a player's muscle memory keeps working.
        Tests.eq("the old name for planks", 5, ItemIds.resolve("planks").id());
        Tests.eq("the modern name for planks", 5, ItemIds.resolve("oak_planks").id());
        Tests.eq("rose is the beta name for poppy", 38, ItemIds.resolve("rose").id());

        Tests.eq("names are reported namespaced", "minecraft:stone", ItemIds.nameOf(1));

        final List<String> identifiers = ItemIds.allIdentifiers();
        Tests.check("suggestions are namespaced", identifiers.contains("minecraft:stone"));
        Tests.check("aliases are not suggested", !identifiers.contains("minecraft:planks"));
        Tests.check("every canonical name is suggested", identifiers.size() >= VanillaIds.names().size());
    }

    /**
     * The entity table, which is only worth having if the modern names in it are the ones 26.2
     * actually registers - these four are the ones that have moved since beta.
     */
    private static void vanillaEntityIds() {
        Tests.group("entity identifiers");

        Tests.eq("a name that never changed", "Creeper", VanillaEntityIds.byName("creeper"));
        Tests.eq("primed tnt is minecraft:tnt", "PrimedTnt", VanillaEntityIds.byName("tnt"));
        Tests.eq("falling sand is minecraft:falling_block", "FallingSand", VanillaEntityIds.byName("falling_block"));
        // Pre-1.16, so it is still a pigman here; the 1.16 name resolves but is not what gets offered.
        Tests.eq("the pigman keeps its pre-1.16 id", "PigZombie", VanillaEntityIds.byName("zombie_pigman"));
        Tests.eq("the pigman is named as one", "zombie_pigman", VanillaEntityIds.nameOf("PigZombie"));
        Tests.eq("the 1.16 name still resolves", "PigZombie", VanillaEntityIds.byName("zombified_piglin"));
        Tests.check("the 1.16 name is not suggested", !VanillaEntityIds.names().contains("zombified_piglin"));
        // Pre-1.21.2, because beta has one boat and no wood variants to pick between.
        Tests.eq("the boat keeps its pre-1.21.2 id", "Boat", VanillaEntityIds.byName("boat"));
        Tests.eq("the boat is named as one", "boat", VanillaEntityIds.nameOf("Boat"));
        Tests.eq("the per-wood name still resolves", "Boat", VanillaEntityIds.byName("oak_boat"));
        Tests.check("the per-wood name is not suggested", !VanillaEntityIds.names().contains("oak_boat"));

        Tests.eq("resolution ignores case", "Creeper", VanillaEntityIds.byName("CREEPER"));
        Tests.eq("beta's own name for the pigman still resolves", "PigZombie", VanillaEntityIds.byName("pigman"));
        Tests.check("an entity beta does not have resolves to nothing", VanillaEntityIds.byName("enderman") == null);

        Tests.eq("names are reported namespaced", "tnt", VanillaEntityIds.nameOf("PrimedTnt"));
        Tests.check("an unknown beta id has no modern name", VanillaEntityIds.nameOf("Moa") == null);

        // Every one of beta's registrations must be named, or /summon would offer an id it cannot
        // complete - which is exactly the failure this table was added to fix.
        for (final String path : VanillaEntityIds.names()) {
            final String betaId = VanillaEntityIds.byName(path);
            Tests.check(path + " round-trips", betaId != null && path.equals(VanillaEntityIds.nameOf(betaId)));
        }
        Tests.eq("beta has twenty-four entity types", 24, VanillaEntityIds.names().size());
        Tests.check("aliases are not suggested", !VanillaEntityIds.names().contains("pigman"));
    }

    /**
     * Entity display names: what a lang entry keys on, and what is produced when there is none - which
     * is every entity that has not been written down, modded ones included.
     */
    private static void entityNames() {
        Tests.group("entity names");

        // The key is built from the identifier, not from beta's word, so it stays modern-shaped even
        // where the two disagree. This is the string an end user writes to rename something.
        Tests.eq("a vanilla key", "entity.minecraft.creeper", EntityNames.translationKey("Creeper"));
        Tests.eq("a renamed mob keys on its identifier", "entity.minecraft.zombie_pigman",
            EntityNames.translationKey("PigZombie"));
        Tests.eq("a modded key uses the mod's namespace", "entity.mymod.big_dog",
            EntityNames.translationKey("mymod:big_dog"));

        // With no entry, the identifier is spelled out - capitalised, spaced, no namespace.
        Tests.eq("a one-word name", "Creeper", EntityNames.displayName("Creeper"));
        Tests.eq("a two-word name", "Falling Block", EntityNames.displayName("FallingSand"));
        Tests.eq("a renamed mob reads as its identifier", "Zombie Pigman", EntityNames.displayName("PigZombie"));
        Tests.eq("a modded entity needs no lang file", "Big Dog", EntityNames.displayName("mymod:big_dog"));
        Tests.eq("nothing still names something", "Entity", EntityNames.displayName((String) null));

        // The one name spelling-out cannot reach, which is the whole reason the lang file has a line
        // in it at all. If this ever stops being true the line can go.
        Tests.eq("an acronym cannot be derived", "Tnt", EntityNames.displayName("PrimedTnt"));
        Tests.check("so it is written down instead",
            langFileContains(EntityNames.translationKey("PrimedTnt") + "=TNT"));
    }

    /** Reads RetroAPI's own lang file off the classpath, to check code and file still agree. */
    private static boolean langFileContains(final String line) {
        try (var stream = CommandTest.class.getResourceAsStream("/assets/retroapi/lang/en_US.lang")) {
            if (stream == null) {
                return false;
            }
            return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).contains(line);
        } catch (final java.io.IOException ignored) {
            return false;
        }
    }

    /**
     * The SNBT parser. What matters is that a value lands as the TAG TYPE its suffix claims: beta
     * reads {@code Color} with getByte and {@code Health} with getShort, so an int where a byte was
     * meant is silently zero rather than an error.
     */
    private static void snbt() {
        Tests.group("snbt");

        // Type ids: 1 byte, 2 short, 3 int, 4 long, 5 float, 6 double, 8 string, 9 list, 10 compound.
        Tests.eq("a bare number is an int", (byte) 3, typeOf("{Color:14}", "Color"));
        Tests.eq("...with its value", 14, parse("{Color:14}").getInt("Color"));
        Tests.eq("a b suffix is a byte", (byte) 1, typeOf("{Sheared:1b}", "Sheared"));
        Tests.eq("an s suffix is a short", (byte) 2, typeOf("{Health:20s}", "Health"));
        Tests.eq("an L suffix is a long", (byte) 4, typeOf("{Seed:123L}", "Seed"));
        Tests.eq("an f suffix is a float", (byte) 5, typeOf("{Fall:1.5f}", "Fall"));
        Tests.eq("a d suffix is a double", (byte) 6, typeOf("{Speed:1.5d}", "Speed"));
        Tests.eq("a bare decimal is a double", (byte) 6, typeOf("{Speed:1.5}", "Speed"));
        Tests.eq("a negative number still parses", -5, parse("{Depth:-5}").getInt("Depth"));

        // true/false are bytes, which is what every beta boolean field actually is.
        Tests.eq("true is a byte", (byte) 1, typeOf("{powered:true}", "powered"));
        Tests.eq("...set to one", true, parse("{powered:true}").getBoolean("powered"));
        Tests.eq("false is zero", false, parse("{powered:false}").getBoolean("powered"));

        // An unquoted word is a string, so a name-valued field needs no quotes.
        Tests.eq("an unquoted word is a string", (byte) 8, typeOf("{Motive:Kebab}", "Motive"));
        Tests.eq("...keeping its text", "Kebab", parse("{Motive:Kebab}").getString("Motive"));
        Tests.eq("a quoted string keeps its spaces", "Some Player",
            parse("{Owner:\"Some Player\"}").getString("Owner"));
        Tests.eq("a namespaced value needs no quotes", "mymod:moa",
            parse("{Type:mymod:moa}").getString("Type"));

        // Motion and Rotation are lists, and a compound can nest.
        Tests.eq("a list parses", (byte) 9, typeOf("{Motion:[0.0d,1.0d,0.0d]}", "Motion"));
        Tests.eq("...with every element", 3, parse("{Motion:[0.0d,1.0d,0.0d]}").getList("Motion").size());
        Tests.eq("a compound nests", (byte) 10, typeOf("{Item:{id:1}}", "Item"));
        Tests.eq("...and is readable", 1, parse("{Item:{id:1}}").getCompound("Item").getInt("id"));

        // Shape: several keys, free whitespace, and the empty compound.
        Tests.eq("several keys", 14, parse("{Color:14,Sheared:1b}").getInt("Color"));
        Tests.eq("...both of them", true, parse("{Color:14,Sheared:1b}").getBoolean("Sheared"));
        Tests.eq("whitespace is free", 14, parse("{ Color : 14 , Sheared : 1b }").getInt("Color"));
        Tests.check("an empty compound is legal", parse("{}").values().isEmpty());

        // Straight to the parser, not through the helper below, so the real exception propagates.
        Tests.throwsError("a missing colon is an error", CommandSyntaxException.class,
            () -> Snbt.parseCompound(new StringReader("{Color}")));
        Tests.throwsError("an unterminated compound is an error", CommandSyntaxException.class,
            () -> Snbt.parseCompound(new StringReader("{Color:14")));
        Tests.throwsError("a missing value is an error", CommandSyntaxException.class,
            () -> Snbt.parseCompound(new StringReader("{Color:}")));
        Tests.throwsError("a missing name is an error", CommandSyntaxException.class,
            () -> Snbt.parseCompound(new StringReader("{:14}")));
    }

    /** For the cases that are expected to parse; a failure here is a test failure, not an outcome. */
    private static net.minecraft.nbt.NbtCompound parse(final String text) {
        try {
            return Snbt.parseCompound(new StringReader(text));
        } catch (final CommandSyntaxException thrown) {
            throw new RuntimeException(thrown);
        }
    }

    /** The tag type id stored under a key, which is the thing a suffix is there to decide. */
    private static byte typeOf(final String text, final String key) {
        for (final net.minecraft.nbt.NbtElement value : parse(text).values()) {
            if (key.equals(value.getKey())) {
                return value.getType();
            }
        }
        return -1;
    }

    private static void coordinates() {
        Tests.group("coordinates");

        Tests.eq("an absolute coordinate is taken as written", 10.0, parseCoordinate("10", false).toAbsolute(5.0));
        Tests.eq("a whole number can be centred in its block", 10.5, parseCoordinate("10", true).toAbsolute(5.0));
        Tests.eq("a decimal is never centred", 10.2, parseCoordinate("10.2", true).toAbsolute(5.0));
        Tests.eq("a bare tilde means no offset", 5.0, parseCoordinate("~", false).toAbsolute(5.0));
        Tests.eq("a tilde with a number offsets", 8.0, parseCoordinate("~3", false).toAbsolute(5.0));
        Tests.eq("a negative offset works", 2.0, parseCoordinate("~-3", false).toAbsolute(5.0));

        Tests.check("a relative coordinate says so", parseCoordinate("~1", false).relative());
        Tests.check("an absolute coordinate says so", !parseCoordinate("1", false).relative());

        Tests.throwsError("a position needs three coordinates", CommandSyntaxException.class,
            () -> DefaultPosArgument.parse(new StringReader("1 2"), false));
        Tests.throwsError("local and world coordinates cannot mix", CommandSyntaxException.class,
            () -> DefaultPosArgument.parse(new StringReader("1 ^2 3"), false));
    }

    private static void units() {
        Tests.group("units");

        Tests.eq("a bare number is ticks", 100, parseTime("100"));
        Tests.eq("t is ticks", 100, parseTime("100t"));
        Tests.eq("s is seconds", 100, parseTime("5s"));
        Tests.eq("d is days", 24000, parseTime("1d"));
        Tests.throwsError("an unknown unit is rejected", CommandSyntaxException.class,
            () -> TimeArgumentType.time().parse(new StringReader("5y")));

        // The argument answers with the mode itself, and adventure and spectator are modes now, so
        // "adventure" is a valid answer rather than the rejection this used to check for.
        Tests.eq("game modes parse by name", RetroGameMode.CREATIVE, parseGameMode("creative"));
        Tests.eq("game modes parse by initial", RetroGameMode.SURVIVAL, parseGameMode("s"));
        Tests.eq("game modes parse by number", RetroGameMode.CREATIVE, parseGameMode("1"));
        Tests.eq("game modes parse adventure", RetroGameMode.ADVENTURE, parseGameMode("adventure"));
        Tests.eq("game modes parse spectator by initial", RetroGameMode.SPECTATOR, parseGameMode("sp"));
        Tests.throwsError("an unknown game mode is rejected", CommandSyntaxException.class,
            () -> GameModeArgumentType.gameMode().parse(new StringReader("wandering")));
    }

    /** The tree a server sends has to come back the same shape, or completions would drift from it. */
    private static void treeRoundTrip() {
        Tests.group("command tree sync");

        final CommandDispatcher<RetroCommandSource> original = new CommandDispatcher<>();
        final LiteralCommandNode<RetroCommandSource> teleport = original.register(
            LiteralArgumentBuilder.<RetroCommandSource>literal("teleport")
                .then(RequiredArgumentBuilder.<RetroCommandSource, Integer>argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 64))
                    .executes(context -> 1)));
        original.register(LiteralArgumentBuilder.<RetroCommandSource>literal("tp").redirect(teleport));
        original.register(LiteralArgumentBuilder.<RetroCommandSource>literal("time")
            .then(LiteralArgumentBuilder.<RetroCommandSource>literal("set")
                .then(RequiredArgumentBuilder.<RetroCommandSource, Integer>argument("value", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                    .executes(context -> 1))));

        final PacketBuffer buffer = PacketBuffers.make();
        CommandTreeSerializer.write(original.getRoot(), buffer);
        final CommandDispatcher<RetroCommandSource> copy = CommandTreeSerializer.read(buffer);

        final List<String> before = Arrays.asList(original.getAllUsage(original.getRoot(), null, false));
        final List<String> after = Arrays.asList(copy.getAllUsage(copy.getRoot(), null, false));
        Tests.eq("usage survives the round trip", before, after);

        // The bounds have to survive too, or the client would accept what the server rejects.
        Tests.check("an out-of-range argument is still rejected",
            copy.parse("teleport 99", null).getReader().canRead());
        Tests.check("an in-range argument still parses",
            !copy.parse("teleport 5", null).getReader().canRead());
        Tests.check("a redirect still reaches its target",
            !copy.parse("tp 5", null).getReader().canRead());
    }

    private static CoordinateArgument parseCoordinate(final String input, final boolean centerIntegers) {
        try {
            return CoordinateArgument.parse(new StringReader(input), centerIntegers);
        } catch (final CommandSyntaxException ex) {
            return new CoordinateArgument(false, Double.NaN);
        }
    }

    private static int parseTime(final String input) {
        try {
            return TimeArgumentType.time().parse(new StringReader(input));
        } catch (final CommandSyntaxException ex) {
            return -1;
        }
    }

    private static RetroGameMode parseGameMode(final String input) {
        try {
            return GameModeArgumentType.gameMode().parse(new StringReader(input));
        } catch (final CommandSyntaxException ex) {
            return null;
        }
    }
}
