package com.periut.retroapi.commandblock;

import com.periut.retroapi.register.blockentity.RetroSyncedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;

/**
 * A command block's command and the state that decides whether it may run.
 *
 * <p>Modern splits this between {@code BaseCommandBlock} (the command, the output, the success
 * count) and {@code CommandBlockEntity} (powered, automatic, conditionMet); beta has no reason for
 * two classes, so the fields are here under modern's names and with modern's meanings.
 *
 * <p>{@code lastExecution} is modern's guard against a block running twice in one tick, which is
 * what stops a chain that loops back on itself from executing forever inside a single tick.
 */
public class CommandBlockEntity extends BlockEntity implements RetroSyncedBlockEntity {

    private String command = "";
    private String lastOutput = "";
    private int successCount;
    private boolean trackOutput = true;

    private boolean powered;
    private boolean automatic;
    private boolean conditionMet;

    private long lastExecution = -1L;

    public String getCommand() {
        return command;
    }

    /** Setting a new command clears the success count, exactly as {@code BaseCommandBlock} does. */
    public void setCommand(final String command) {
        this.command = command == null ? "" : command;
        this.successCount = 0;
        markDirty();
    }

    public String getLastOutput() {
        return lastOutput;
    }

    public void setLastOutput(final String lastOutput) {
        this.lastOutput = lastOutput == null ? "" : lastOutput;
        markDirty();
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(final int successCount) {
        this.successCount = successCount;
        markDirty();
    }

    public boolean isTrackOutput() {
        return trackOutput;
    }

    public void setTrackOutput(final boolean trackOutput) {
        this.trackOutput = trackOutput;
        markDirty();
    }

    public boolean isPowered() {
        return powered;
    }

    public void setPowered(final boolean powered) {
        this.powered = powered;
        markDirty();
    }

    public boolean isAutomatic() {
        return automatic;
    }

    public void setAutomatic(final boolean automatic) {
        this.automatic = automatic;
        markDirty();
    }

    public boolean wasConditionMet() {
        return conditionMet;
    }

    /**
     * Works out whether the block may run, and remembers the answer.
     *
     * <p>Modern's rule: an unconditional block always may; a conditional one may only when the block
     * <em>behind</em> it - opposite the way it faces - is a command block that succeeded.
     *
     * @return the same value {@link #wasConditionMet()} will now report
     */
    public boolean markConditionMet() {
        conditionMet = true;

        if (CommandBlocks.isConditional(world, x, y, z)) {
            final int[] behind = CommandBlockFacing.behind(world, x, y, z);
            conditionMet = behind != null
                && world.getBlockEntity(behind[0], behind[1], behind[2]) instanceof CommandBlockEntity previous
                && previous.getSuccessCount() > 0;
        }

        return conditionMet;
    }

    /** @return false when this block has already run on this game tick */
    boolean beginExecution(final long gameTime) {
        if (lastExecution == gameTime) {
            return false;
        }
        lastExecution = gameTime;
        return true;
    }

    public CommandBlockMode getMode() {
        return CommandBlocks.modeOf(world == null ? 0 : world.getBlockId(x, y, z));
    }

    @Override
    public void readNbt(final NbtCompound nbt) {
        super.readNbt(nbt);
        command = nbt.getString("Command");
        successCount = nbt.getInt("SuccessCount");
        trackOutput = !nbt.contains("TrackOutput") || nbt.getBoolean("TrackOutput");
        lastOutput = trackOutput ? nbt.getString("LastOutput") : "";
        powered = nbt.getBoolean("powered");
        automatic = nbt.getBoolean("auto");
        conditionMet = nbt.getBoolean("conditionMet");
    }

    @Override
    public void writeNbt(final NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString("Command", command);
        nbt.putInt("SuccessCount", successCount);
        nbt.putBoolean("TrackOutput", trackOutput);
        if (trackOutput) {
            nbt.putString("LastOutput", lastOutput);
        }
        nbt.putBoolean("powered", powered);
        nbt.putBoolean("auto", automatic);
        nbt.putBoolean("conditionMet", conditionMet);
    }
}
