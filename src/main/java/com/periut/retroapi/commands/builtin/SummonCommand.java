package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.periut.retroapi.commands.Position;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.commands.argument.EntityAttributes;
import com.periut.retroapi.commands.argument.EntityNames;
import com.periut.retroapi.commands.argument.EntitySummonArgumentType;
import com.periut.retroapi.entity.EntityRegistration;
import com.periut.retroapi.registry.RetroRegistry;
import com.periut.retroapi.text.Text;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

import java.util.concurrent.CompletableFuture;

import static com.periut.retroapi.commands.RetroCommandManager.argument;
import static com.periut.retroapi.commands.RetroCommandManager.literal;
import static com.periut.retroapi.commands.argument.EntitySummonArgumentType.entitySummon;
import static com.periut.retroapi.commands.argument.EntitySummonArgumentType.getEntitySummon;
import static com.periut.retroapi.commands.argument.NbtCompoundArgumentType.getNbtCompound;
import static com.periut.retroapi.commands.argument.NbtCompoundArgumentType.nbtCompound;
import static com.periut.retroapi.commands.argument.Vec3ArgumentType.getVec3;
import static com.periut.retroapi.commands.argument.Vec3ArgumentType.vec3;

/**
 * {@code /summon <entity> [pos] [nbt]}, in modern Minecraft's shape.
 *
 * <p>The third argument is modern's own: {@code /summon minecraft:sheep ~ ~ ~ {Color:14,Sheared:1b}}.
 * The keys are beta's, because they are read straight off the entity rather than from a table - see
 * {@link EntityAttributes}, which is also why a mod's entity is customisable the moment it is
 * registered, with no API to call.
 */
public final class SummonCommand {
    private SummonCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("summon")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
            .then(argument("entity", entitySummon())
                .executes(context -> summon(context, context.getSource().getPosition(), null))
                .then(argument("pos", vec3())
                    .executes(context -> summon(context, getVec3(context, "pos"), null))
                    .then(argument("nbt", nbtCompound())
                        .suggests(SummonCommand::suggestAttributes)
                        .executes(context -> summon(context, getVec3(context, "pos"), getNbtCompound(context, "nbt")))))));
    }

    private static int summon(final CommandContext<RetroCommandSource> context, final Position position,
                              final NbtCompound attributes) throws CommandSyntaxException {
        final RetroCommandSource source = context.getSource();
        final World world = source.getWorld();
        if (world == null) {
            throw RetroCommandSource.REQUIRES_PLAYER.create();
        }

        final String id = getEntitySummon(context, "entity");
        final String name = EntityNames.displayName(id);

        Entity entity = EntityRegistry.create(id, world);
        if (entity == null) {
            entity = fromRegistration(id, world, position);
        }

        if (entity == null) {
            source.sendError(Text.literal("Unable to summon " + name));
            return 0;
        }

        // Position first: the attributes are laid over what the entity currently is, and where it is
        // counts as part of that - so a Pos of the player's own would otherwise win over the one typed.
        entity.setPosition(position.x(), position.y(), position.z());
        if (!EntityAttributes.apply(entity, attributes)) {
            source.sendError(Text.literal("Could not apply those attributes to " + name));
            return 0;
        }

        world.spawnEntity(entity);

        // Modern's wording is "Summoned new %s" and stops there. The position is this mod's own
        // addition, kept because /summon here takes coordinates that can be relative or looked-at and
        // seeing where that landed is worth a few characters.
        source.sendFeedback(Text.literal("Summoned new " + name + " at " + position));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Completes the attribute names of whichever entity was named earlier in the command.
     *
     * <p>Brigadier offers the whole remaining argument, so the offset is moved to just past the last
     * {@code &#123;} or comma - otherwise accepting a completion would replace the compound typed so far
     * rather than the key being typed.
     */
    private static CompletableFuture<Suggestions> suggestAttributes(final CommandContext<RetroCommandSource> context,
                                                                    final SuggestionsBuilder builder) {
        final String remaining = builder.getRemaining();
        if (remaining.isEmpty()) {
            builder.suggest("{");
            return builder.buildFuture();
        }
        if (remaining.charAt(0) != '{') {
            return Suggestions.empty();
        }

        final int lastBreak = Math.max(remaining.lastIndexOf('{'), remaining.lastIndexOf(','));
        final String partial = remaining.substring(lastBreak + 1);
        if (partial.indexOf(':') >= 0) {
            // Past the colon is a value, and only the entity knows what its values mean.
            return Suggestions.empty();
        }

        final String id;
        try {
            id = getEntitySummon(context, "entity");
        } catch (final IllegalArgumentException ignored) {
            return Suggestions.empty();
        }

        final SuggestionsBuilder keys = builder.createOffset(builder.getStart() + lastBreak + 1);
        for (final String key : EntityAttributes.keys(id, context.getSource().getWorld())) {
            if (key.toLowerCase(java.util.Locale.ROOT).startsWith(partial.toLowerCase(java.util.Locale.ROOT))) {
                keys.suggest(key + ":");
            }
        }
        return keys.buildFuture();
    }

    /**
     * The mod's own factory, for a modded entity whose class has no {@code (World)} constructor.
     *
     * <p>{@code EntityRegistry.create} is reflection over exactly that one signature, so an entity
     * that takes its spawn coordinates instead - which {@code EntityFactory} exists for - was
     * offered by the completion, accepted by the parser, and then failed to appear. The registration
     * already knows how to build one; asking it is the whole fix.
     */
    private static Entity fromRegistration(final String id, final World world, final Position position) {
        final EntityRegistration registration = RetroRegistry.getEntityByStringId(id);
        if (registration == null) {
            return null;
        }
        if (registration.getMobFactory() != null) {
            return registration.getMobFactory().create(world);
        }
        if (registration.getSimpleEntityFactory() != null) {
            return registration.getSimpleEntityFactory().create(world);
        }
        if (registration.getEntityFactory() != null) {
            return registration.getEntityFactory().create(world, position.x(), position.y(), position.z());
        }
        return null;
    }
}
