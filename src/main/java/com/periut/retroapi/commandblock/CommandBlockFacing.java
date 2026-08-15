package com.periut.retroapi.commandblock;

import com.periut.retroapi.state.RetroBlockState;
import com.periut.retroapi.state.RetroStates;
import com.periut.retroapi.util.RetroDirection;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Which way a command block points, and what is in front of and behind it.
 *
 * <p>Modern keeps this in a {@code FACING} block state; here it is RetroAPI's six-way
 * {@link RetroDirection} state, which is the same thing with the same six values. Chains walk
 * forwards along it and conditional blocks look backwards along it, exactly as in modern.
 */
public final class CommandBlockFacing {
    private CommandBlockFacing() {
    }

    public static RetroDirection of(final BlockView world, final int x, final int y, final int z) {
        if (!(world instanceof World level)) {
            return RetroDirection.NORTH;
        }
        final RetroBlockState state = RetroStates.get(level, x, y, z);
        if (state == null) {
            return RetroDirection.NORTH;
        }
        final RetroDirection facing = state.get(RetroDirection.PROPERTY);
        return facing == null ? RetroDirection.NORTH : facing;
    }

    /** The position this block points at - where a chain continues. */
    public static int[] frontOf(final World world, final int x, final int y, final int z) {
        return offset(of(world, x, y, z), x, y, z);
    }

    /** The position behind it - what a conditional block checks succeeded. */
    public static int[] behind(final World world, final int x, final int y, final int z) {
        return offset(of(world, x, y, z).opposite(), x, y, z);
    }

    public static int[] offset(final RetroDirection direction, final int x, final int y, final int z) {
        return new int[]{x + direction.offsetX, y + direction.offsetY, z + direction.offsetZ};
    }
}
