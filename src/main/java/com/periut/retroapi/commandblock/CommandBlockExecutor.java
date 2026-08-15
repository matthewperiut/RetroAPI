package com.periut.retroapi.commandblock;

import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.commands.Position;
import com.periut.retroapi.commands.RetroCommandManager;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.gamerule.RetroGameRules;
import com.periut.retroapi.util.RetroDirection;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.world.World;

/**
 * When a command block runs - a port of modern's {@code CommandBlock.neighborChanged/tick} and
 * {@code executeChain}.
 *
 * <p>The three modes behave as they do there:
 *
 * <ul>
 *   <li><b>Impulse</b> ({@code REDSTONE}) marks its condition and schedules a tick on the rising
 *       edge of a signal; the tick runs it.</li>
 *   <li><b>Repeating</b> ({@code AUTO}) marks its condition every tick and reschedules itself for as
 *       long as it is powered or always-active.</li>
 *   <li><b>Chain</b> ({@code SEQUENCE}) is never driven by redstone: the block pointing into it runs
 *       it, in the same tick, and the walk continues while each next block is also a chain block.</li>
 * </ul>
 *
 * <p>A conditional block whose condition is not met sets its success count to zero rather than
 * running - which is what makes the block after it in a chain stop too.
 */
public final class CommandBlockExecutor {
    private CommandBlockExecutor() {
    }

    public static void onNeighborUpdate(final World world, final int x, final int y, final int z) {
        if (world == null || world.isRemote) {
            return;
        }
        if (!(world.getBlockEntity(x, y, z) instanceof CommandBlockEntity block)) {
            return;
        }

        setPoweredAndUpdate(world, x, y, z, block, world.isPowered(x, y, z));
    }

    /** Modern's {@code setPoweredAndUpdate}: only the RISING edge does anything. */
    private static void setPoweredAndUpdate(final World world, final int x, final int y, final int z,
            final CommandBlockEntity block, final boolean powered) {
        if (powered == block.isPowered()) {
            return;
        }

        block.setPowered(powered);
        if (!powered) {
            return;
        }
        // A repeating block is already ticking, and a chain block is driven by its neighbour.
        if (block.isAutomatic() || block.getMode() == CommandBlockMode.SEQUENCE) {
            return;
        }

        block.markConditionMet();
        world.scheduleBlockUpdate(x, y, z, world.getBlockId(x, y, z), 1);
    }

    public static void onScheduledTick(final World world, final int x, final int y, final int z) {
        if (world == null || world.isRemote) {
            return;
        }
        if (!(world.getBlockEntity(x, y, z) instanceof CommandBlockEntity block)) {
            return;
        }

        final boolean hasCommand = block.getCommand() != null && !block.getCommand().isEmpty();
        final CommandBlockMode mode = block.getMode();
        final boolean wasConditionMet = block.wasConditionMet();

        if (mode == CommandBlockMode.AUTO) {
            block.markConditionMet();
            if (wasConditionMet) {
                execute(world, x, y, z, block, hasCommand);
            } else if (CommandBlocks.isConditional(world, x, y, z)) {
                block.setSuccessCount(0);
            }

            if (block.isPowered() || block.isAutomatic()) {
                world.scheduleBlockUpdate(x, y, z, world.getBlockId(x, y, z), 1);
            }
        } else if (mode == CommandBlockMode.REDSTONE) {
            if (wasConditionMet) {
                execute(world, x, y, z, block, hasCommand);
            } else if (CommandBlocks.isConditional(world, x, y, z)) {
                block.setSuccessCount(0);
            }
        }
    }

    private static void execute(final World world, final int x, final int y, final int z,
            final CommandBlockEntity block, final boolean hasCommand) {
        // A command block sits inside World.tick, so anything thrown here takes the world down with
        // it - which is far too high a price for one bad command. Modern catches around its own
        // execute for the same reason; this catches the walk as well, because the block that throws
        // is not always the one the player typed into.
        try {
            if (hasCommand) {
                performCommand(world, x, y, z, block);
            } else {
                block.setSuccessCount(0);
            }

            executeChain(world, x, y, z, CommandBlockFacing.of(world, x, y, z));
        } catch (final Throwable thrown) {
            block.setSuccessCount(0);
            RetroAPI.LOGGER.error("Command block at {},{},{} failed: {}", x, y, z, thrown.toString(), thrown);
        }
    }

    /**
     * Runs one block's command.
     *
     * @return true when the command reported success, which is what the next chain block waits for
     */
    public static boolean performCommand(final World world, final int x, final int y, final int z,
            final CommandBlockEntity block) {
        // Modern's once-per-tick guard. Without it a chain that loops back on itself never returns.
        if (!block.beginExecution(world.getTime())) {
            return false;
        }

        block.setSuccessCount(0);

        final String command = block.getCommand();
        if (command == null || command.isEmpty()) {
            return false;
        }

        final RetroCommandManager manager = RetroCommandManager.getInstance();
        if (manager == null) {
            return false;
        }

        final CommandBlockFeedback feedback = new CommandBlockFeedback();
        final RetroCommandSource source = CommandBlockSources
            .forBlock(world, new Position(x + 0.5, y, z + 0.5))
            .withSink(feedback);

        final int result = manager.execute(source, command.startsWith("/") ? command.substring(1) : command);
        block.setSuccessCount(result);

        if (block.isTrackOutput()) {
            block.setLastOutput(feedback.getText());
        }
        if (RetroGameRules.getBoolean(RetroGameRules.COMMAND_BLOCK_OUTPUT) && !feedback.getText().isEmpty()) {
            RetroAPI.LOGGER.info("[CommandBlock] {}", feedback.getText());
        }

        return result > 0;
    }

    /**
     * Walks forward from a block that just ran, firing chain blocks - modern's {@code executeChain},
     * including the way the direction is re-read from each block so a chain can turn corners.
     */
    private static void executeChain(final World world, final int startX, final int startY, final int startZ,
            RetroDirection direction) {
        int x = startX;
        int y = startY;
        int z = startZ;
        int remaining = RetroGameRules.getInt(RetroGameRules.MAX_COMMAND_CHAIN_LENGTH);

        while (remaining-- > 0) {
            x += direction.offsetX;
            y += direction.offsetY;
            z += direction.offsetZ;

            if (CommandBlocks.modeOf(world.getBlockId(x, y, z)) != CommandBlockMode.SEQUENCE
                || !CommandBlocks.isCommandBlock(world.getBlockId(x, y, z))
                || !(world.getBlockEntity(x, y, z) instanceof CommandBlockEntity block)) {
                break;
            }

            if (block.isPowered() || block.isAutomatic()) {
                if (block.markConditionMet()) {
                    if (!performCommand(world, x, y, z, block)) {
                        break;
                    }
                } else if (CommandBlocks.isConditional(world, x, y, z)) {
                    block.setSuccessCount(0);
                }
            }

            direction = CommandBlockFacing.of(world, x, y, z);
        }

        if (remaining <= 0) {
            RetroAPI.LOGGER.warn("Command Block chain tried to execute more than {} steps!",
                Math.max(0, RetroGameRules.getInt(RetroGameRules.MAX_COMMAND_CHAIN_LENGTH)));
        }
    }

    /** Placing one: modern seeds trackOutput from the sendCommandFeedback rule and syncs power. */
    public static void onPlaced(final World world, final int x, final int y, final int z) {
        if (world.isRemote || !(world.getBlockEntity(x, y, z) instanceof CommandBlockEntity block)) {
            return;
        }
        block.setTrackOutput(RetroGameRules.getBoolean(RetroGameRules.SEND_COMMAND_FEEDBACK));
        setPoweredAndUpdate(world, x, y, z, block, world.isPowered(x, y, z));
    }
}
