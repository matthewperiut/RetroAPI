/*
 * Ported from WhatAreYouScoring by Telvarost. The scoring rules are unchanged; the achievement
 * pages it registered are not carried over, as they need an item/achievement registry API.
 */
package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.config.Enums;
import com.periut.retrotweaks.feature.scoring.Score;
import com.periut.retrotweaks.util.Players;

import net.minecraft.block.Block;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public class BlockScoringMixin {

    @Shadow @Final public int id;

    @Inject(method = "afterBreak", at = @At("HEAD"), cancellable = true)
    public void retrotweaks$afterBreakBlock(World world, PlayerEntity playerEntity, int x, int y, int z, int meta, CallbackInfo ci) {
        if (null != playerEntity) {
            Score.Fields retrotweaks$s = Score.of(playerEntity);
            if (  Config.SCORING.basic.enabled
               && Config.SCORING.basic.onBlockRemoved
            ) {
                if (0 == retrotweaks$s.BLOCKS_REMOVED) {
                }

                retrotweaks$s.BLOCKS_REMOVED++;
                retrotweaks$s.CurrentBasicScore = Score.calculateBasicScore(retrotweaks$s);
            }

            if (Config.SCORING.challenge404.enabled) {
                if (3 > retrotweaks$s.PUMPKINS_BROKEN) {
                    if (this.id == Block.PUMPKIN.id) {
                        retrotweaks$s.PUMPKINS_BROKEN++;
                        retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, world);

                        if (3 == retrotweaks$s.PUMPKINS_BROKEN) {
                            PlayerEntity player = Players.local();
                            if (null != player) {
                            }
                        }
                        return;
                    }
                }

                if (20 > retrotweaks$s.SUGAR_CANES_BROKEN) {
                    if (this.id == Block.SUGAR_CANE.id) {
                        retrotweaks$s.SUGAR_CANES_BROKEN++;
                        retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, world);

                        if (20 == retrotweaks$s.SUGAR_CANES_BROKEN) {
                            PlayerEntity player = Players.local();
                            if (null != player) {
                            }
                        }
                        return;
                    }
                }

                if (32 > retrotweaks$s.CACTI_BROKEN) {
                    if (this.id == Block.CACTUS.id) {
                        retrotweaks$s.CACTI_BROKEN++;
                        retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, world);

                        if (32 == retrotweaks$s.CACTI_BROKEN) {
                            PlayerEntity player = Players.local();
                            if (null != player) {
                            }
                        }
                        return;
                    }
                }

                if (retrotweaks$s.HAS_OBSIDIAN_BEEN_BROKEN != (retrotweaks$s.HAS_OBSIDIAN_BEEN_BROKEN & retrotweaks$s.OTHER_BITFIELD)) {
                    if (this.id == Block.OBSIDIAN.id) {
                        retrotweaks$s.OTHER_BITFIELD |= retrotweaks$s.HAS_OBSIDIAN_BEEN_BROKEN;
                        retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, world);
                        PlayerEntity player = Players.local();
                        if (null != player) {
                        }
                        return;
                    }
                }
            }
        }
    }

    @Inject(method = "onPlaced(Lnet/minecraft/world/World;IIILnet/minecraft/entity/LivingEntity;)V", at = @At("HEAD"), cancellable = true)
    public void retrotweaks$afterPlaced(World world, int x, int y, int z, LivingEntity placer, CallbackInfo ci) {
        if (null != placer) {
            if (placer instanceof PlayerEntity) {
                Score.Fields retrotweaks$s = Score.of((PlayerEntity) placer);
                if (  Config.SCORING.basic.enabled
                   && Config.SCORING.basic.onBlockPlaced
                ) {
                    if (0 == retrotweaks$s.BLOCKS_PLACED) {
                    }
                    retrotweaks$s.BLOCKS_PLACED++;
                    retrotweaks$s.CurrentBasicScore = Score.calculateBasicScore(retrotweaks$s);
                }

                if (Config.SCORING.challenge404.enabled) {
                    if (32 > retrotweaks$s.GLASS_PLACED) {
                        if (this.id == Block.GLASS.id) {
                            retrotweaks$s.GLASS_PLACED++;
                            retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, world);
                            if (32 == retrotweaks$s.GLASS_PLACED) {
                            }
                            return;
                        }
                    }

                    if (20 > retrotweaks$s.BRICKS_PLACED) {
                        if (this.id == Block.BRICKS.id) {
                            retrotweaks$s.BRICKS_PLACED++;
                            retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, world);
                            if (20 == retrotweaks$s.BRICKS_PLACED) {
                            }
                            return;
                        }
                    }

                    if (retrotweaks$s.HAS_CRASH_SLAB_BEEN_PLACED != (retrotweaks$s.HAS_CRASH_SLAB_BEEN_PLACED & retrotweaks$s.OTHER_BITFIELD)) {
                        if (world.getBlockId(x, y - 1, z) == Block.DOUBLE_SLAB.id) {
                            if (3 < world.getBlockMeta(x, y - 1, z)) {
                                retrotweaks$s.OTHER_BITFIELD |= retrotweaks$s.HAS_CRASH_SLAB_BEEN_PLACED;
                                retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, world);
                            }
                            return;
                        }
                    }

                    if (retrotweaks$s.HAS_OVERWORLD_GLOWSTONE_BEEN_PLACED != (retrotweaks$s.HAS_OVERWORLD_GLOWSTONE_BEEN_PLACED & retrotweaks$s.OTHER_BITFIELD)) {
                        if (this.id == Block.GLOWSTONE.id) {
                            retrotweaks$s.OTHER_BITFIELD |= retrotweaks$s.HAS_OVERWORLD_GLOWSTONE_BEEN_PLACED;
                            retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, world);
                            if (0 == ((PlayerEntity) placer).dimensionId) {
                            }
                            return;
                        }
                    }
                }
            }
        }
    }
}
