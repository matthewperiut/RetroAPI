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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.stat.Stat;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerScoringMixin extends LivingEntity {

    @Shadow public abstract void incrementStat(Stat stat);

    public PlayerScoringMixin(World world) {
        super(world);
    }

    @Inject(method = "onKilledBy", at = @At("HEAD"))
    public void retrotweaks$onKilledBy(Entity adversary, CallbackInfo ci) {
        Score.Fields retrotweaks$s = Score.of((PlayerEntity) (Object) this);

        /** - Calculate score and reset score fields */
        Score.resetFieldsOnDeath(retrotweaks$s, this.world, false);

        if (Config.SCORING.difficultyDeathMultiplier) {
            /** - Get basic score */
            int currentScoreBasic = retrotweaks$s.CurrentBasicScore;
            /** - Get days survived */
            int currentScoreDays = retrotweaks$s.CurrentDaysScore;
            /** - Get 404 challenge score */
            int currentScore404 = retrotweaks$s.Current404Score;

            double saveScoreMultiplier;

            if (0 == this.world.difficulty) {
                saveScoreMultiplier = 1;
            } else if (1 == this.world.difficulty) {
                saveScoreMultiplier = 0.75;
            } else if (2 == this.world.difficulty) {
                saveScoreMultiplier = 0.5;
            } else {
                saveScoreMultiplier = 0;
            }

            retrotweaks$s.PrevCumulativeBasicScore = retrotweaks$s.CumulativeBasicScore;
            retrotweaks$s.PrevCumulativeDaysScore = retrotweaks$s.CumulativeDaysScore;
            retrotweaks$s.PrevCumulative404Score = retrotweaks$s.Cumulative404Score;
            retrotweaks$s.CumulativeBasicScore = (int)((currentScoreBasic + retrotweaks$s.CumulativeBasicScore) * saveScoreMultiplier);
            retrotweaks$s.CumulativeDaysScore = (int)((currentScoreDays + retrotweaks$s.CumulativeDaysScore) * saveScoreMultiplier);
            retrotweaks$s.Cumulative404Score = (int)((currentScore404 + retrotweaks$s.Cumulative404Score) * saveScoreMultiplier);
        }
    }

    @Inject(method = "writeNbt", at = @At("HEAD"))
    private void retrotweaks$writeCustomDataToTag(NbtCompound tag, CallbackInfo info) {
        Score.Fields retrotweaks$s = Score.of((PlayerEntity) (Object) this);
        if (Config.SCORING.difficultyDeathMultiplier) {
            tag.putInt("SB", retrotweaks$s.CumulativeBasicScore);
            tag.putInt("SD", retrotweaks$s.CumulativeDaysScore);
            tag.putInt("SC", retrotweaks$s.Cumulative404Score);
        }

        if (Config.SCORING.basic.enabled) {
            tag.putInt("BP", retrotweaks$s.BLOCKS_PLACED);
            tag.putInt("BR", retrotweaks$s.BLOCKS_REMOVED);
            tag.putInt("BM", retrotweaks$s.MONSTER_MOBS_KILLED);
            tag.putInt("BA", retrotweaks$s.PASSIVE_MOBS_KILLED);
        }

        if (Config.SCORING.days.enabled) {
            retrotweaks$s.DAYS_PLAYED = (int)Math.floor(this.world.getProperties().getTime() / 24000);
            tag.putInt("DP", retrotweaks$s.DAYS_PLAYED);
            tag.putInt("DL", retrotweaks$s.LAST_DEATH_DAY);

            if (retrotweaks$s.PREV_DAYS_PLAYED != retrotweaks$s.DAYS_PLAYED) {
                retrotweaks$s.PREV_DAYS_PLAYED = retrotweaks$s.DAYS_PLAYED;
                retrotweaks$s.CurrentDaysScore = Score.calculateDaysScore(retrotweaks$s);
                if (100 <= retrotweaks$s.DAYS_PLAYED) {
                    if (365 <= retrotweaks$s.DAYS_PLAYED) {
                    }
                }
            }
        }

        if (Config.SCORING.challenge404.enabled) {
            tag.putInt("CKZ", retrotweaks$s.ZOMBIE_KILLED);
            tag.putInt("CKK", retrotweaks$s.SKELETON_KILLED);
            tag.putInt("CKS", retrotweaks$s.SPIDER_KILLED);
            tag.putInt("CKC", retrotweaks$s.CREEPER_KILLED);
            tag.putInt("CKG", retrotweaks$s.GHAST_KILLED);
            tag.putInt("CKP", retrotweaks$s.ZOMBIE_PIGMAN_KILLED);
            tag.putInt("CBW", retrotweaks$s.WHEAT_BROKEN);
            tag.putInt("CBC", retrotweaks$s.CACTI_BROKEN);
            tag.putInt("CBS", retrotweaks$s.SUGAR_CANES_BROKEN);
            tag.putInt("CBP", retrotweaks$s.PUMPKINS_BROKEN);
            tag.putInt("CPG", retrotweaks$s.GLASS_PLACED);
            tag.putInt("CPB", retrotweaks$s.BRICKS_PLACED);
            tag.putInt("CPW", retrotweaks$s.WOOL_TYPES_PLACED);
            tag.putInt("CW", retrotweaks$s.WOOL_PLACED_BITFIELD);
            tag.putInt("CB", retrotweaks$s.BOW_AND_ARROW_CRAFTING_BITFIELD);
            tag.putInt("CM", retrotweaks$s.MISC_CRAFTING_BITFIELD);
            tag.putInt("CA", retrotweaks$s.ARMOR_CRAFTING_BITFIELD);
            tag.putInt("CE", retrotweaks$s.EXPLOSION_STATUS_BITFIELD);
            tag.putInt("CO", retrotweaks$s.OTHER_BITFIELD);
        }
    }

    @Inject(method = "readNbt", at = @At("HEAD"))
    private void retrotweaks$readCustomDataFromTag(NbtCompound tag, CallbackInfo info) {
        Score.Fields retrotweaks$s = Score.of((PlayerEntity) (Object) this);
        if (Config.SCORING.difficultyDeathMultiplier) {
            retrotweaks$s.CumulativeBasicScore = tag.getInt("SB");
            retrotweaks$s.CumulativeDaysScore  = tag.getInt("SD");
            retrotweaks$s.Cumulative404Score   = tag.getInt("SC");
            retrotweaks$s.PrevCumulativeBasicScore = retrotweaks$s.CumulativeBasicScore;
            retrotweaks$s.PrevCumulativeDaysScore  = retrotweaks$s.CumulativeDaysScore;
            retrotweaks$s.PrevCumulative404Score   = retrotweaks$s.Cumulative404Score;
        }

        if (Config.SCORING.basic.enabled) {
            retrotweaks$s.BLOCKS_PLACED       = tag.getInt("BP");
            retrotweaks$s.BLOCKS_REMOVED      = tag.getInt("BR");
            retrotweaks$s.MONSTER_MOBS_KILLED = tag.getInt("BM");
            retrotweaks$s.PASSIVE_MOBS_KILLED = tag.getInt("BA");
        }

        if (Config.SCORING.days.enabled) {
            retrotweaks$s.DAYS_PLAYED    = tag.getInt("DP");
            retrotweaks$s.LAST_DEATH_DAY = tag.getInt("DL");

            // WhatAreYouScoring also awarded real-time-played achievements here; those need an
            // achievement registry and are not part of RetroTweaks.
        }

        if (Config.SCORING.challenge404.enabled) {
            retrotweaks$s.ZOMBIE_KILLED                     = tag.getInt("CKZ");
            retrotweaks$s.SKELETON_KILLED                   = tag.getInt("CKK");
            retrotweaks$s.SPIDER_KILLED                     = tag.getInt("CKS");
            retrotweaks$s.CREEPER_KILLED                    = tag.getInt("CKC");
            retrotweaks$s.GHAST_KILLED                      = tag.getInt("CKG");
            retrotweaks$s.ZOMBIE_PIGMAN_KILLED              = tag.getInt("CKP");
            retrotweaks$s.WHEAT_BROKEN                      = tag.getInt("CBW");
            retrotweaks$s.CACTI_BROKEN                      = tag.getInt("CBC");
            retrotweaks$s.SUGAR_CANES_BROKEN                = tag.getInt("CBS");
            retrotweaks$s.PUMPKINS_BROKEN                   = tag.getInt("CBP");
            retrotweaks$s.GLASS_PLACED                      = tag.getInt("CPG");
            retrotweaks$s.BRICKS_PLACED                     = tag.getInt("CPB");
            retrotweaks$s.WOOL_TYPES_PLACED                 = tag.getInt("CPW");
            retrotweaks$s.WOOL_PLACED_BITFIELD              = tag.getInt("CW" );
            retrotweaks$s.BOW_AND_ARROW_CRAFTING_BITFIELD   = tag.getInt("CB" );
            retrotweaks$s.MISC_CRAFTING_BITFIELD            = tag.getInt("CM" );
            retrotweaks$s.ARMOR_CRAFTING_BITFIELD           = tag.getInt("CA" );
            retrotweaks$s.EXPLOSION_STATUS_BITFIELD         = tag.getInt("CE" );
            retrotweaks$s.OTHER_BITFIELD                    = tag.getInt("CO" );
        }

        retrotweaks$s.CurrentBasicScore = Score.calculateBasicScore(retrotweaks$s);
        retrotweaks$s.CurrentDaysScore = Score.calculateDaysScore(retrotweaks$s);
        retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, world);
    }
}
