package com.periut.retroapi.mixin.gamerule;

import com.periut.retroapi.gamerule.RetroGameRules;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code doMobLoot}: a dying mob drops nothing.
 *
 * <p>Players are deliberately exempt - what a player drops on death is {@code keepInventory}'s
 * business, and answering to both rules at once would make each depend on the other's value.
 */
@Mixin(LivingEntity.class)
public class MobLootMixin {

	@Inject(method = "dropItems", at = @At("HEAD"), cancellable = true)
	private void retroapi$doMobLoot(CallbackInfo ci) {
		if ((Object) this instanceof PlayerEntity) {
			return;
		}
		if (!RetroGameRules.getBoolean(RetroGameRules.DO_MOB_LOOT)) {
			ci.cancel();
		}
	}
}
