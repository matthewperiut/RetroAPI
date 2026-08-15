package com.periut.retroapi.mixin.gamerule;

import com.periut.retroapi.gamerule.RetroGameRules;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * {@code mobGriefing}: a creeper or a ghast still explodes, hurts and makes noise - it just leaves
 * the terrain alone.
 *
 * <p>Emptying the collected block set after the explosion has worked out what it would destroy is
 * the smallest place to intervene: the damage, sound and particles all happen elsewhere and are
 * untouched. TNT and a player-triggered blast are not a mob's doing, so they still break blocks.
 */
@Mixin(Explosion.class)
public class MobGriefingMixin {
	@Shadow private Set damagedBlocks;
	@Shadow private Entity source;

	@Inject(method = "explode", at = @At("TAIL"))
	private void retroapi$mobGriefing(CallbackInfo ci) {
		if (source instanceof LivingEntity && !(source instanceof PlayerEntity)
			&& !RetroGameRules.getBoolean(RetroGameRules.MOB_GRIEFING)) {
			damagedBlocks.clear();
		}
	}
}
