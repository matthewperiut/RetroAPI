/*
 * Ported from WhatAreYouScoring by Telvarost. The scoring rules are unchanged; the achievement
 * pages it registered are not carried over, as they need an item/achievement registry API.
 */
package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.config.Enums;
import com.periut.retrotweaks.feature.scoring.Score;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.mob.MonsterEntity;
import net.minecraft.entity.mob.PigZombieEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityScoringMixin extends Entity {

    public LivingEntityScoringMixin(World arg) {
        super(arg);
    }

    @Inject(method = "onKilledBy", at = @At("HEAD"), cancellable = true)
    public void retrotweaks$onKilledBy(Entity entity, CallbackInfo ci) {
        // Only a player's kill counts, and it counts towards that player.
        if (!(entity instanceof PlayerEntity retrotweaks$killer)) return;
        Score.Fields retrotweaks$s = Score.of(retrotweaks$killer);
        if (Config.SCORING.basic.enabled) {
            if (entity instanceof PlayerEntity) {
                LivingEntity instance = ((LivingEntity) (Object)this);

                if (  (Config.SCORING.basic.enabled)
                   && (Config.SCORING.basic.onMonsterKilled)
                   && (  (instance instanceof MonsterEntity)
                      || (instance instanceof GhastEntity)
                      || (instance instanceof SlimeEntity)
                      )
                ) {
                    if (0 == retrotweaks$s.MONSTER_MOBS_KILLED) {
                    }

                    retrotweaks$s.MONSTER_MOBS_KILLED++;
                    retrotweaks$s.CurrentBasicScore = Score.calculateBasicScore(retrotweaks$s);
                }

                if (  (Config.SCORING.basic.enabled)
                   && (Config.SCORING.basic.onPassiveKilled)
                   && (  (instance instanceof AnimalEntity)
                      || (instance instanceof SquidEntity)
                      )
                ) {
                    if (0 == retrotweaks$s.PASSIVE_MOBS_KILLED) {
                    }

                    retrotweaks$s.PASSIVE_MOBS_KILLED++;
                    retrotweaks$s.CurrentBasicScore = Score.calculateBasicScore(retrotweaks$s);
                }
            }
        }

        if (Config.SCORING.challenge404.enabled) {
            if (entity instanceof PlayerEntity) {
                LivingEntity instance = ((LivingEntity) (Object) this);

                if (Config.SCORING.challenge404.scoreMobKills) {
                    if (instance instanceof ZombieEntity) {
                        if (instance instanceof PigZombieEntity) {
                            if (0 == retrotweaks$s.ZOMBIE_PIGMAN_KILLED) {
                            }
                            retrotweaks$s.ZOMBIE_PIGMAN_KILLED++;
                        } else {
                            if (0 == retrotweaks$s.ZOMBIE_KILLED) {
                            }
                            retrotweaks$s.ZOMBIE_KILLED++;
                        }
                    } else if (instance instanceof SkeletonEntity) {
                        if (0 == retrotweaks$s.SKELETON_KILLED) {
                        }
                        retrotweaks$s.SKELETON_KILLED++;
                    } else if (instance instanceof SpiderEntity) {
                        if (0 == retrotweaks$s.SPIDER_KILLED) {
                        }
                        retrotweaks$s.SPIDER_KILLED++;
                    } else if (instance instanceof CreeperEntity) {
                        if (0 == retrotweaks$s.CREEPER_KILLED) {
                        }
                        retrotweaks$s.CREEPER_KILLED++;
                    } else if (instance instanceof GhastEntity) {
                        if (0 == retrotweaks$s.GHAST_KILLED) {
                        }
                        retrotweaks$s.GHAST_KILLED++;
                    }

                    retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, world);
                }
            }
        }
    }
}
