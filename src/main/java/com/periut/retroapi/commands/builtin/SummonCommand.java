package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.periut.retroapi.commands.api.SummonRegistry;
import com.periut.retroapi.commands.Position;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.commands.argument.EntitySummonArgumentType;
import com.periut.retroapi.text.Text;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityRegistry;
import net.minecraft.world.World;

import static com.periut.retroapi.commands.RetroCommandManager.argument;
import static com.periut.retroapi.commands.RetroCommandManager.literal;
import static com.periut.retroapi.commands.argument.EntitySummonArgumentType.entitySummon;
import static com.periut.retroapi.commands.argument.EntitySummonArgumentType.getEntitySummon;
import static com.periut.retroapi.commands.argument.Vec3ArgumentType.getVec3;
import static com.periut.retroapi.commands.argument.Vec3ArgumentType.vec3;

/**
 * {@code /summon <entity> [pos] [options]}.
 *
 * <p>Modern's third argument is NBT, which beta has no parser or writer for; instead the trailing
 * words are handed to whatever {@link SummonRegistry} factory is registered for that entity type -
 * the mechanism this mod has always used to make a creeper charged or a sheep pink.
 */
public final class SummonCommand {
    private SummonCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("summon")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
            .then(argument("entity", entitySummon())
                .executes(context -> summon(context, context.getSource().getPosition(), new String[0]))
                .then(argument("pos", vec3())
                    .executes(context -> summon(context, getVec3(context, "pos"), new String[0]))
                    .then(argument("options", StringArgumentType.greedyString())
                        .executes(context -> summon(context, getVec3(context, "pos"), StringArgumentType.getString(context, "options").split(" ")))))));
    }

    private static int summon(final CommandContext<RetroCommandSource> context, final Position position, final String[] options) throws CommandSyntaxException {
        final RetroCommandSource source = context.getSource();
        final World world = source.getWorld();
        if (world == null) {
            throw RetroCommandSource.REQUIRES_PLAYER.create();
        }

        final String id = getEntitySummon(context, "entity");
        final Class<? extends Entity> type = EntitySummonArgumentType.classOf(id);

        Entity entity = null;
        if (SummonRegistry.hasFactory(type)) {
            entity = SummonRegistry.create(type, world, position, options);
            if (entity == null) {
                source.sendError(Text.literal("Invalid options for " + id + ". Usage: " + SummonRegistry.usageFor(type)));
                return 0;
            }
        }

        if (entity == null) {
            entity = EntityRegistry.create(id, world);
        }

        if (entity == null) {
            source.sendError(Text.literal("Unable to summon " + id));
            return 0;
        }

        entity.setPosition(position.x(), position.y(), position.z());
        world.spawnEntity(entity);

        source.sendFeedback(Text.literal("Summoned " + id + " at " + position));
        return Command.SINGLE_SUCCESS;
    }
}
