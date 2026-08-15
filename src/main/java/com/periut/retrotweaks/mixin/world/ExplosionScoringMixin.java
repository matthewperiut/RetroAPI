/*
 * Ported from WhatAreYouScoring by Telvarost. The scoring rules are unchanged; the achievement
 * pages it registered are not carried over, as they need an item/achievement registry API.
 */
package com.periut.retrotweaks.mixin.world;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.config.Enums;
import com.periut.retrotweaks.feature.scoring.Score;
import com.periut.retrotweaks.util.Players;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Set;

@Mixin(Explosion.class)
public class ExplosionScoringMixin {

    @Shadow
    private World world;
    @Shadow
    public Set damagedBlocks;

    @Inject(method = "playExplosionSound", at = @At("HEAD"), cancellable = true)
    public void retrotweaks$cancelAllExplosionTileDamage(boolean renderParticles, CallbackInfo ci) {
        // Explosion scoring is a client-side detail of the 404 challenge, so it follows the local
        // player rather than trying to attribute a creeper's blast to somebody.
        Score.Fields retrotweaks$s = Score.ofLocalPlayer();
        if (Config.SCORING.challenge404.enabled) {
            if (0xF != retrotweaks$s.EXPLOSION_STATUS_BITFIELD) {
                if (0 < retrotweaks$s.DETECT_CREEPER_EXPLOSION) {
                    retrotweaks$s.DETECT_CREEPER_EXPLOSION--;
                    ArrayList tilesToCheck = new ArrayList<BlockPos>();
                    tilesToCheck.addAll(this.damagedBlocks);

                    for (int tileIndex = tilesToCheck.size() - 1; tileIndex >= 0; --tileIndex) {
                        BlockPos tilePos = (BlockPos) tilesToCheck.get(tileIndex);
                        int blockId = this.world.getBlockId(tilePos.x, tilePos.y, tilePos.z);

                        if (Block.IRON_BLOCK.id == blockId) {
                            retrotweaks$s.EXPLOSION_STATUS_BITFIELD |= 0x1;
                            retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, world);
                            PlayerEntity player = Players.local();
                            if (null != player) {
                            }
                        } else if (Block.GOLD_BLOCK.id == blockId) {
                            retrotweaks$s.EXPLOSION_STATUS_BITFIELD |= 0x2;
                            retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, world);
                            PlayerEntity player = Players.local();
                            if (null != player) {
                            }
                        } else if (Block.LAPIS_BLOCK.id == blockId) {
                            retrotweaks$s.EXPLOSION_STATUS_BITFIELD |= 0x4;
                            retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, world);
                            PlayerEntity player = Players.local();
                            if (null != player) {
                            }
                        } else if (Block.DIAMOND_BLOCK.id == blockId) {
                            retrotweaks$s.EXPLOSION_STATUS_BITFIELD |= 0x8;
                            retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, world);
                            PlayerEntity player = Players.local();
                            if (null != player) {
                            }
                        }
                    }
                }
            }
        }
    }

}