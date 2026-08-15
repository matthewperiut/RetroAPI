/*
 * Ported from WhatAreYouScoring by Telvarost. The scoring rules are unchanged; the achievement
 * page it registered ('place 8 types of wool' / 'place all 16 types') is not carried over, as it
 * needs an item/achievement registry API.
 */
package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.feature.scoring.Score;
import com.periut.retrotweaks.util.Players;

import net.minecraft.block.Block;
import net.minecraft.block.WoolBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Tracks which wool colours have been placed, for the 404 challenge's 'place 8 types of wool'
 * (+10) and 'place all 16 types' (+15) bonuses. From WhatAreYouScoring.
 *
 * <p>An override, not an inject: {@code WoolBlock} inherits {@code onPlaced} from {@code Block}
 * rather than declaring it, so there is nothing on {@code WoolBlock} itself to inject into.
 */
@Mixin(WoolBlock.class)
public abstract class WoolBlockMixin extends Block {

	protected WoolBlockMixin(int id, Material material) {
		super(id, material);
	}

	@Override
	public void onPlaced(World world, int x, int y, int z, LivingEntity arg2) {
		if (Config.SCORING.challenge404.enabled) {
			if (null != arg2) {
				if (arg2 instanceof PlayerEntity) {
					Score.Fields retrotweaks$s = Score.of((PlayerEntity) arg2);
					if (0xFFFF != retrotweaks$s.WOOL_PLACED_BITFIELD) {
						if (this.id == Block.WOOL.id) {
							int tileMeta = world.getBlockMeta(x, y, z);
							int valueToCheck = (0x0001 << tileMeta);
							if (valueToCheck != (valueToCheck & retrotweaks$s.WOOL_PLACED_BITFIELD)) {
								retrotweaks$s.WOOL_TYPES_PLACED++;
								retrotweaks$s.WOOL_PLACED_BITFIELD |= valueToCheck;
								retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, world);
							}

							if (8 == retrotweaks$s.WOOL_TYPES_PLACED) {
								PlayerEntity player = Players.local();
								if (null != player) {
								}
							}

							if (16 == retrotweaks$s.WOOL_TYPES_PLACED) {
								PlayerEntity player = Players.local();
								if (null != player) {
								}
							}
							return;
						}
					}
				}
			}
		}
	}
}
