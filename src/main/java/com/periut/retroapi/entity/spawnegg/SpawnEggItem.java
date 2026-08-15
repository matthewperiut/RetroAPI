package com.periut.retroapi.entity.spawnegg;

import com.periut.retroapi.gamemode.RetroGameMode;
import com.periut.retroapi.gamemode.RetroGameModes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityRegistry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * A spawn egg, ported from modern's {@code SpawnEggItem}.
 *
 * <p>Modern has one item per mob rather than one item with the entity in its damage value, and this
 * follows that: {@link SpawnEggs} registers thirteen of these, each holding the vanilla
 * {@code EntityRegistry} string of the mob it makes. Nothing about the entity is copied here - the egg
 * asks the registry for one, which is how a mod's own mob gets an egg by doing nothing at all.
 *
 * <p>What it does on use is modern's {@code useOn}: place the mob in the block <em>against</em> the
 * face that was clicked, facing a random way, and take one off the stack unless the player is in
 * creative. Everything happens on the server only, as spawning always must.
 */
public class SpawnEggItem extends Item {

    private final String entityId;

    public SpawnEggItem(final int id, final String entityId) {
        super(id);
        this.entityId = entityId;
    }

    /** The vanilla {@code EntityRegistry} id this egg spawns, e.g. {@code "Pig"}. */
    public String getEntityId() {
        return entityId;
    }

    @Override
    public boolean useOnBlock(final ItemStack stack, final PlayerEntity user, final World world,
                              int x, int y, int z, final int side) {
        if (world == null || world.isRemote) {
            // The client says "used" so the swing animation plays; the server does the spawning.
            return true;
        }

        // Modern: an egg used ON a spawner re-points it instead of spawning anything, which is the
        // only way to set one without commands. Spawners tells every client watching.
        if (Spawners.setMob(world, x, y, z, entityId)) {
            if (!RetroGameModes.is(user, RetroGameMode.CREATIVE)) {
                stack.count--;
            }
            return true;
        }

        // Beta's own face offsets, the ones BlockItem places against.
        switch (side) {
            case 0 -> y--;
            case 1 -> y++;
            case 2 -> z--;
            case 3 -> z++;
            case 4 -> x--;
            case 5 -> x++;
            default -> {
            }
        }

        if (!spawn(world, x + 0.5, y, z + 0.5)) {
            return false;
        }

        if (!RetroGameModes.is(user, RetroGameMode.CREATIVE)) {
            stack.count--;
        }
        return true;
    }

    /** @return whether a mob was actually made and added to the world */
    public boolean spawn(final World world, final double x, final double y, final double z) {
        final Entity entity = EntityRegistry.create(entityId, world);
        if (entity == null) {
            return false;
        }

        // The same call beta's own spawner and mob spawner block use, random facing included.
        if (entity instanceof LivingEntity living) {
            living.setPositionAndAnglesKeepPrevAngles(x, y, z, world.random.nextFloat() * 360.0F, 0.0F);
        } else {
            entity.setPositionAndAngles(x, y, z, world.random.nextFloat() * 360.0F, 0.0F);
        }
        return world.spawnEntity(entity);
    }
}
