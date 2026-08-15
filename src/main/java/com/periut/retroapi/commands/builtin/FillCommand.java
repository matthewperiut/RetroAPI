package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.periut.retroapi.commands.Position;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.commands.argument.BlockArgumentType;
import com.periut.retroapi.gamerule.RetroGameRules;
import com.periut.retroapi.text.Text;
import net.minecraft.block.Block;
import net.minecraft.world.World;

import static com.periut.retroapi.commands.RetroCommandManager.argument;
import static com.periut.retroapi.commands.RetroCommandManager.literal;
import static com.periut.retroapi.commands.argument.BlockPosArgumentType.blockPos;
import static com.periut.retroapi.commands.argument.BlockPosArgumentType.getBlockPos;

/**
 * {@code /fill <from> <to> <block> [destroy|hollow|keep|outline|replace]}, ported from modern's
 * {@code FillCommand}.
 *
 * <p>All five of modern's modes, with modern's meanings: {@code replace} (the default) fills
 * everything, {@code destroy} breaks what it replaces so it drops, {@code keep} only fills air,
 * {@code hollow} fills the shell and clears the inside, and {@code outline} fills the shell and
 * leaves the inside alone.
 *
 * <p>The area limit is modern's {@code maxBlockModifications} rule, refusing before it starts rather
 * than freezing partway through.
 */
public final class FillCommand {
    private FillCommand() {
    }

    public static final SimpleCommandExceptionType ERROR_FAILED =
        new SimpleCommandExceptionType(Text.literal("No blocks were filled"));
    public static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType(
        (limit, count) -> Text.literal("Too many blocks in the specified area (maximum " + limit + ", specified " + count + ")"));

    private enum Mode {
        REPLACE,
        DESTROY,
        KEEP,
        HOLLOW,
        OUTLINE
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("fill")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
            .then(argument("from", blockPos())
                .then(argument("to", blockPos())
                    .then(argument("block", BlockArgumentType.block())
                        .executes(context -> fill(context, Mode.REPLACE))
                        .then(literal("replace").executes(context -> fill(context, Mode.REPLACE)))
                        .then(literal("destroy").executes(context -> fill(context, Mode.DESTROY)))
                        .then(literal("keep").executes(context -> fill(context, Mode.KEEP)))
                        .then(literal("hollow").executes(context -> fill(context, Mode.HOLLOW)))
                        .then(literal("outline").executes(context -> fill(context, Mode.OUTLINE)))))));
    }

    private static int fill(final CommandContext<RetroCommandSource> context, final Mode mode) throws CommandSyntaxException {
        final RetroCommandSource source = context.getSource();
        final World world = source.getWorld();
        if (world == null) {
            throw RetroCommandSource.REQUIRES_PLAYER.create();
        }

        final Position from = getBlockPos(context, "from");
        final Position to = getBlockPos(context, "to");
        final BlockArgumentType.BlockInput block = BlockArgumentType.getBlock(context, "block");

        final int minX = (int) Math.floor(Math.min(from.x(), to.x()));
        final int minY = (int) Math.floor(Math.min(from.y(), to.y()));
        final int minZ = (int) Math.floor(Math.min(from.z(), to.z()));
        final int maxX = (int) Math.floor(Math.max(from.x(), to.x()));
        final int maxY = (int) Math.floor(Math.max(from.y(), to.y()));
        final int maxZ = (int) Math.floor(Math.max(from.z(), to.z()));

        final long area = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        final int limit = RetroGameRules.getInt(RetroGameRules.MAX_BLOCK_MODIFICATIONS);
        if (area > limit) {
            throw ERROR_AREA_TOO_LARGE.create(limit, area);
        }

        int filled = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    final boolean onShell = x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ;

                    if ((mode == Mode.HOLLOW || mode == Mode.OUTLINE) && !onShell) {
                        // Hollow clears the inside; outline leaves it exactly as it was.
                        if (mode == Mode.HOLLOW && world.setBlock(x, y, z, 0, 0)) {
                            filled++;
                        }
                        continue;
                    }
                    if (mode == Mode.KEEP && world.getBlockId(x, y, z) != 0) {
                        continue;
                    }
                    if (mode == Mode.DESTROY) {
                        destroy(world, x, y, z);
                    }

                    if (world.setBlock(x, y, z, block.blockId(), block.meta())) {
                        filled++;
                    }
                }
            }
        }

        if (filled == 0) {
            throw ERROR_FAILED.create();
        }

        final int total = filled;
        source.sendFeedback(Text.literal("Successfully filled " + total + " block" + (total == 1 ? "" : "s")));
        return filled;
    }

    /** What modern's {@code destroyBlock} does, spelled out: tell the block, then drop its items. */
    private static void destroy(final World world, final int x, final int y, final int z) {
        final int id = world.getBlockId(x, y, z);
        final Block old = id > 0 && id < Block.BLOCKS.length ? Block.BLOCKS[id] : null;
        if (old == null) {
            return;
        }
        final int meta = world.getBlockMeta(x, y, z);
        old.onBreak(world, x, y, z);
        old.dropStacks(world, x, y, z, meta, 1.0F);
    }
}
