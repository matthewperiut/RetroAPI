package com.periut.retroapi.commandblock;

import com.periut.retroapi.gamemode.RetroGameMode;
import com.periut.retroapi.gamemode.RetroGameModes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

/**
 * Saving an edited command block.
 *
 * <p>Modern sends {@code ServerboundSetCommandBlockPacket} and the server re-checks that the sender
 * may use game-master blocks; this is the same half of that exchange - the checking half, which runs
 * on whichever side owns the world.
 *
 * <p>Common code, so it must stay free of client classes: the sending half lives with the screen that
 * does the sending. Reaching {@code Minecraft} from here made a dedicated server load
 * {@code ClientPlayerEntity} the first time an edit arrived, and refuse.
 */
public final class CommandBlockNetworking {
    private CommandBlockNetworking() {
    }

    /**
     * Applies an edit, checking first that whoever asked is allowed to.
     *
     * <p>Shared by the singleplayer path and the server's packet handler, so the permission check
     * cannot be true in one and forgotten in the other.
     */
    public static void apply(final World world, final PlayerEntity player, final int x, final int y, final int z,
            final String command, final CommandBlockMode mode, final boolean conditional, final boolean automatic,
            final boolean trackOutput) {
        if (player == null || RetroGameModes.get(player) != RetroGameMode.CREATIVE) {
            return;
        }
        if (!(world.getBlockEntity(x, y, z) instanceof CommandBlockEntity block)) {
            return;
        }

        block.setCommand(command);
        block.setTrackOutput(trackOutput);
        block.setAutomatic(automatic);
        CommandBlocks.setConditional(world, x, y, z, conditional);

        // The mode IS the block in modern, so changing it swaps the block - keeping the facing, which
        // lives in the metadata, so a chain does not lose its direction when it is re-typed.
        final net.minecraft.block.Block wanted = CommandBlocks.blockFor(mode);
        if (wanted != null && world.getBlockId(x, y, z) != wanted.id) {
            final int meta = world.getBlockMeta(x, y, z);
            world.setBlock(x, y, z, wanted.id, meta);
            if (world.getBlockEntity(x, y, z) instanceof CommandBlockEntity replaced) {
                replaced.setCommand(command);
                replaced.setTrackOutput(trackOutput);
                replaced.setAutomatic(automatic);
            }
        }

        // Tell the client what it just saved. markDirty only writes the block entity to disk and offers
        // it to vanilla's block-entity packet, which has nothing to say about anything but a sign - so
        // on a server the edit landed, ran, and was invisible: reopening the block showed the empty
        // command the client had made up locally, which looks exactly like the save having failed.
        if (world.getBlockEntity(x, y, z) instanceof CommandBlockEntity saved) {
            com.periut.retroapi.register.blockentity.RetroBlockEntities.sync(saved);
        }

        CommandBlockExecutor.onPlaced(world, x, y, z);
    }
}
