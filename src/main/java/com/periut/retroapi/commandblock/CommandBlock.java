package com.periut.retroapi.commandblock;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

/**
 * The command block itself.
 *
 * <p>Everything that decides <em>when</em> it runs lives in {@link CommandBlockExecutor}; this is the
 * block: it holds a {@link CommandBlockEntity}, it opens the editing screen when an operator uses it,
 * and it reacts to redstone.
 */
public class CommandBlock extends BlockWithEntity {

    public CommandBlock(final int id) {
        super(id, Material.STONE);
    }

    @Override
    protected BlockEntity createBlockEntity() {
        return new CommandBlockEntity();
    }

    @Override
    public void neighborUpdate(final World world, final int x, final int y, final int z, final int neighborId) {
        CommandBlockExecutor.onNeighborUpdate(world, x, y, z);
    }

    @Override
    public boolean onUse(final World world, final int x, final int y, final int z, final PlayerEntity player) {
        // Modern gates this on canUseGameMasterBlocks(): creative only, which is also the only way to
        // get one of these in the first place.
        if (player == null
            || com.periut.retroapi.gamemode.RetroGameModes.get(player)
                != com.periut.retroapi.gamemode.RetroGameMode.CREATIVE) {
            return false;
        }
        // Two separate reasons for this shape.
        //
        // NOT gated on world.isRemote: in beta singleplayer the client's own world is not remote, so
        // that check opened the editor on a server and nowhere else.
        //
        // Gated on the ENVIRONMENT, and reaching the screen through a class this one never names:
        // CommandBlockScreens touches Minecraft, whose fields name ClientPlayerEntity, and a dedicated
        // server refuses to load that class at all. A static call here would resolve it while verifying
        // onUse and take the server down with "Cannot load class ... in environment type SERVER".
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            openEditor(player, x, y, z);
        }
        return true;
    }

    @Override
    public void onPlaced(final World world, final int x, final int y, final int z,
            final net.minecraft.entity.LivingEntity placer) {
        super.onPlaced(world, x, y, z, placer);
        CommandBlockExecutor.onPlaced(world, x, y, z);
    }

    /**
     * Reflective on purpose: naming {@code CommandBlockScreens} here would put a client-only class in
     * this class's constant pool, and this class is loaded on every dedicated server.
     */
    private static void openEditor(final PlayerEntity player, final int x, final int y, final int z) {
        try {
            Class.forName("com.periut.retroapi.client.screen.CommandBlockScreens")
                .getMethod("openFor", PlayerEntity.class, int.class, int.class, int.class)
                .invoke(null, player, x, y, z);
        } catch (final ReflectiveOperationException | LinkageError ignored) {
            // No screen in this environment; the block still works, it just cannot be edited here.
        }
    }

    /** Repeat blocks need a tick of their own; the executor decides whether this one does anything. */
    @Override
    public void onTick(final World world, final int x, final int y, final int z, final java.util.Random random) {
        CommandBlockExecutor.onScheduledTick(world, x, y, z);
    }

    @Override
    public boolean isOpaque() {
        return true;
    }
}
