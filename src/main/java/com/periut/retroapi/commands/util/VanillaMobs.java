package com.periut.retroapi.commands.util;

import com.periut.retroapi.commands.api.SummonRegistry;
import com.periut.retroapi.mixin.commands.access.EntityAccessor;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.SheepEntity;

/**
 * Summon-time options for the vanilla mobs that have any.
 *
 * <p>Every argument is optional: {@code /summon Sheep} spawns a plain white sheep, and the extra
 * words only refine that.
 */
public class VanillaMobs {
    public static void setupSummons() {
        SummonRegistry.add(CreeperEntity.class, (world, pos, args) -> {
            final CreeperEntity creeper = new CreeperEntity(world);
            if (flag(args, 0)) {
                ((EntityAccessor) creeper).getDataTracker().set(17, (byte) 1);
            }
            return creeper;
        }, "[charged (0 or 1)]");

        SummonRegistry.add(SheepEntity.class, (world, pos, args) -> {
            final SheepEntity sheep = new SheepEntity(world);
            sheep.setColor(number(args, 0, 0));
            sheep.setSheared(args.length > 1 && !flag(args, 1));
            return sheep;
        }, "[colour] [has wool (0 or 1)]");

        SummonRegistry.add(PigEntity.class, (world, pos, args) -> {
            final PigEntity pig = new PigEntity(world);
            if (flag(args, 0)) {
                ((EntityAccessor) pig).getDataTracker().set(16, (byte) 1);
            }
            return pig;
        }, "[saddle (0 or 1)]");

        SummonRegistry.add(SlimeEntity.class, (world, pos, args) -> {
            final SlimeEntity slime = new SlimeEntity(world);
            final int size = number(args, 0, 0);
            if (size > 0) {
                ((EntityAccessor) slime).getDataTracker().set(16, (byte) size);
            }
            return slime;
        }, "[size]");

        SummonRegistry.add(TntEntity.class, (world, pos, args) -> {
            final TntEntity tnt = new TntEntity(world);
            final int fuse = number(args, 0, 0);
            if (fuse > 0) {
                tnt.fuse = fuse;
            }
            return tnt;
        }, "[fuse ticks]");
    }

    private static int number(final String[] args, final int index, final int fallback) {
        if (index >= args.length) {
            return fallback;
        }
        try {
            return Integer.parseInt(args[index]);
        } catch (final NumberFormatException ignored) {
            return fallback;
        }
    }

    /** Treats anything that is not zero or absent as set, which is how the old syntax behaved. */
    private static boolean flag(final String[] args, final int index) {
        return index < args.length && !args[index].isEmpty() && args[index].charAt(0) != '0';
    }
}
