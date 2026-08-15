package com.periut.retroapi.commandblock;

import com.periut.retroapi.register.block.RetroBlockAccess;
import com.periut.retroapi.register.blockentity.RetroBlockEntities;
import com.periut.retroapi.storage.RetroBlockData;
import com.periut.retroapi.storage.RetroBlockDataType;
import net.minecraft.block.Block;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers;

/**
 * The three command blocks, registered the way modern has them.
 *
 * <p>Modern does not store the mode on the block: impulse, chain and repeating are three separate
 * blocks, and {@code CommandBlockEntity.getMode()} reads back which one it is sitting in. That is
 * copied here, because it is what makes a chain "walk" work - the walk asks each neighbour whether
 * it <em>is</em> a chain block, not what setting it holds.
 *
 * <p>All three register under {@code minecraft:}, not RetroAPI's own namespace: they are vanilla
 * blocks beta happens not to have yet, and a world that later moves to a version which does have
 * them should find its command blocks already under the right name.
 *
 * <p>The two remaining settings ride on {@link RetroBlockData}, RetroAPI's per-position {@code int}
 * store, because beta's block metadata is entirely spent on which way the block faces:
 * {@link #CONDITIONAL} is modern's {@code CONDITIONAL} block state and {@link #AUTO} is its
 * "Always Active" toggle.
 */
public final class CommandBlocks {
    private CommandBlocks() {
    }

    public static final NamespacedIdentifier IMPULSE_ID = NamespacedIdentifiers.from("minecraft", "command_block");
    public static final NamespacedIdentifier CHAIN_ID = NamespacedIdentifiers.from("minecraft", "chain_command_block");
    public static final NamespacedIdentifier REPEATING_ID = NamespacedIdentifiers.from("minecraft", "repeating_command_block");

    public static Block IMPULSE;
    public static Block CHAIN;
    public static Block REPEATING;

    /** 1 when the block only runs if the one behind it succeeded (modern's CONDITIONAL state). */
    public static RetroBlockDataType CONDITIONAL;
    /** 1 when the block needs no redstone at all (modern's "Always Active"). */
    public static RetroBlockDataType AUTO;

    /** Called from RetroAPI's own init, so command blocks exist with or without any mod asking. */
    public static void register() {
        if (IMPULSE != null) {
            return;
        }

        CONDITIONAL = RetroBlockData.register(NamespacedIdentifiers.from("retroapi", "command_block_conditional"));
        AUTO = RetroBlockData.register(NamespacedIdentifiers.from("retroapi", "command_block_auto"));

        IMPULSE = register(IMPULSE_ID, "command_block");
        CHAIN = register(CHAIN_ID, "chain_command_block");
        REPEATING = register(REPEATING_ID, "repeating_command_block");

        RetroBlockEntities.register(IMPULSE_ID, CommandBlockEntity.class);
    }

    /**
     * @param texture the base name of the block's textures, which follow modern's own naming:
     *                {@code <name>_front}, {@code _side}, {@code _back}. They are modern's textures,
     *                first frame only - beta cannot animate an arbitrary block texture.
     */
    private static Block register(final NamespacedIdentifier id, final String texture) {
        // NOT the AUTO_ID sentinel: BlockWithEntity's constructor writes BLOCKS_WITH_ENTITY[id] using
        // the raw parameter, before Block's constructor has resolved the sentinel into a real slot, so
        // the sentinel reaches an array index and throws. Allocating up front is what a BlockWithEntity
        // subclass has to do.
        return RetroBlockAccess.of(new CommandBlock(RetroBlockAccess.allocateId()))
            .sounds(Block.STONE_SOUND_GROUP)
            // Modern's command block cannot be broken in survival and is only obtainable by command;
            // beta has no game-master concept, so being unbreakable does that job.
            .unbreakable()
            .facingAll()
            // Modern's own faces: the arrow on the front, the ring on the back, the plain body on the
            // sides. The facing state turns them, which is what makes a chain readable at a glance.
            .sided(retroTexture(texture + "_side"), retroTexture(texture + "_side"),
                retroTexture(texture + "_front"), retroTexture(texture + "_back"))
            .register(id);
    }

    private static NamespacedIdentifier retroTexture(final String name) {
        return NamespacedIdentifiers.from("retroapi", name);
    }

    /** Modern's rule: the mode IS the block. */
    public static CommandBlockMode modeOf(final int blockId) {
        if (CHAIN != null && blockId == CHAIN.id) {
            return CommandBlockMode.SEQUENCE;
        }
        if (REPEATING != null && blockId == REPEATING.id) {
            return CommandBlockMode.AUTO;
        }
        return CommandBlockMode.REDSTONE;
    }

    public static Block blockFor(final CommandBlockMode mode) {
        return switch (mode) {
            case SEQUENCE -> CHAIN;
            case AUTO -> REPEATING;
            case REDSTONE -> IMPULSE;
        };
    }

    public static boolean isCommandBlock(final int blockId) {
        return IMPULSE != null
            && (blockId == IMPULSE.id || blockId == CHAIN.id || blockId == REPEATING.id);
    }

    public static boolean isConditional(final BlockView world, final int x, final int y, final int z) {
        return RetroBlockData.get(world, x, y, z, CONDITIONAL) != 0;
    }

    public static void setConditional(final World world, final int x, final int y, final int z, final boolean conditional) {
        RetroBlockData.set(world, x, y, z, CONDITIONAL, conditional ? 1 : 0);
    }

    public static boolean isAuto(final BlockView world, final int x, final int y, final int z) {
        return RetroBlockData.get(world, x, y, z, AUTO) != 0;
    }

    public static void setAuto(final World world, final int x, final int y, final int z, final boolean auto) {
        RetroBlockData.set(world, x, y, z, AUTO, auto ? 1 : 0);
    }
}
