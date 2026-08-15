package com.periut.retrotweaks.mixin.client.frontview;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.config.Enums;
import com.periut.retrotweaks.feature.options.ModOptions;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.GameOptions;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Adds a front-facing camera to the third-person cycle. From UniTweaks.
 *
 * <p>The perspective key becomes a three-way cycle - first person, third person, front view - by
 * intercepting the write to {@code thirdPerson} rather than by adding a key, so it keeps whatever
 * binding the player already has.
 */
@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public class FrontViewToggleMixin {

	@WrapOperation(method = "tick", at = @At(value = "FIELD",
		target = "Lnet/minecraft/client/option/GameOptions;thirdPerson:Z", opcode = Opcodes.PUTFIELD))
	private void retrotweaks$cyclePerspective(GameOptions options, boolean value, Operation<Void> original) {
		if (Config.INTERFACE.frontViewThirdPerson != Enums.FrontView.NORMAL) {
			ModOptions.frontView = false;
			original.call(options, value);
			return;
		}

		if (!options.thirdPerson) {
			original.call(options, true);
			ModOptions.frontView = false;
		} else if (ModOptions.frontView) {
			ModOptions.frontView = false;
			original.call(options, false);
		} else {
			ModOptions.frontView = true;
		}
	}
}
