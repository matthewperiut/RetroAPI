package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Punching primed TNT defuses it back into a dropped TNT item. From UniTweaks - and ONLY that: this
 * class is skipped wholesale when UniTweaks is installed, so nothing that must survive UniTweaks may
 * live here. The explosion-source marker used to, and died with the skip (breaking "Disable TNT
 * Block Breaking" for TNT only); it now lives in {@link TntExplosionSourceMixin}. Its sibling,
 * Punch TNT To Ignite, lives in TntBlockMixin.
 */
@Mixin(TntEntity.class)
public abstract class TntEntityMixin extends Entity {

	public TntEntityMixin(World world) {
		super(world);
	}

	@Override
	public boolean damage(Entity damageSource, int amount) {
		if (!world.isRemote) {
			if (Config.GAMEPLAY.oldFeatures.punchTntToDefuse && !dead) {
				super.damage(damageSource, amount);
				if (damageSource instanceof PlayerEntity) {
					markDead();
					world.spawnEntity(new ItemEntity(this.world, this.x, this.y, this.z, new ItemStack(Block.TNT)));
					return true;
				}
			}
		}
		return false;
	}
}
