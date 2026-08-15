package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.periut.retroapi.commands.Position;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.commands.argument.BlockArgumentType;
import com.periut.retroapi.text.Text;
import net.minecraft.world.World;

import static com.periut.retroapi.commands.RetroCommandManager.argument;
import static com.periut.retroapi.commands.RetroCommandManager.literal;
import static com.periut.retroapi.commands.argument.BlockPosArgumentType.blockPos;
import static com.periut.retroapi.commands.argument.BlockPosArgumentType.getBlockPos;

/**
 * {@code /setblock <pos> <block> [destroy|keep|replace]}, ported from modern's
 * {@code SetBlockCommand}.
 *
 * <p>All three of modern's modes: {@code replace} (the default) overwrites whatever is there,
 * {@code destroy} breaks the old block first so it drops, and {@code keep} refuses unless the space
 * is empty. Modern's fourth, {@code strict}, is about skipping neighbour updates on a
 * data-generation path beta has no equivalent of, so it is not here.
 */
public final class SetBlockCommand {
    private SetBlockCommand() {
    }

    public static final SimpleCommandExceptionType ERROR_FAILED =
        new SimpleCommandExceptionType(Text.literal("Could not set the block"));

    private enum Mode {
        REPLACE,
        DESTROY,
        KEEP
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("setblock")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
            .then(argument("pos", blockPos())
                .then(argument("block", BlockArgumentType.block())
                    .executes(context -> setBlock(context, Mode.REPLACE))
                    .then(literal("replace").executes(context -> setBlock(context, Mode.REPLACE)))
                    .then(literal("destroy").executes(context -> setBlock(context, Mode.DESTROY)))
                    .then(literal("keep").executes(context -> setBlock(context, Mode.KEEP))))));
    }

    private static int setBlock(final CommandContext<RetroCommandSource> context, final Mode mode) throws CommandSyntaxException {
        final RetroCommandSource source = context.getSource();
        final World world = source.getWorld();
        if (world == null) {
            throw RetroCommandSource.REQUIRES_PLAYER.create();
        }

        final Position position = getBlockPos(context, "pos");
        final int x = (int) Math.floor(position.x());
        final int y = (int) Math.floor(position.y());
        final int z = (int) Math.floor(position.z());

        final BlockArgumentType.BlockInput block = BlockArgumentType.getBlock(context, "block");

        if (mode == Mode.KEEP && world.getBlockId(x, y, z) != 0) {
            throw ERROR_FAILED.create();
        }
        if (mode == Mode.DESTROY) {
            // Modern's destroy mode breaks the old block properly, so it drops. Beta has no single
            // "destroyBlock" call, so this is what that call does: tell the block it is going, then
            // drop its items at full chance.
            final int oldId = world.getBlockId(x, y, z);
            final net.minecraft.block.Block old = oldId > 0 && oldId < net.minecraft.block.Block.BLOCKS.length
                ? net.minecraft.block.Block.BLOCKS[oldId] : null;
            if (old != null) {
                final int oldMeta = world.getBlockMeta(x, y, z);
                old.onBreak(world, x, y, z);
                old.dropStacks(world, x, y, z, oldMeta, 1.0F);
            }
        }

        if (!world.setBlock(x, y, z, block.blockId(), block.meta())) {
            throw ERROR_FAILED.create();
        }

        source.sendFeedback(Text.literal("Changed the block at " + x + ", " + y + ", " + z));
        return Command.SINGLE_SUCCESS;
    }
}
