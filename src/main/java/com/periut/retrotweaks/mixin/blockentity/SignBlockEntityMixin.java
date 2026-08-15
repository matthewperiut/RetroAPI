package com.periut.retrotweaks.mixin.blockentity;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Companion to the dye-a-sign action in {@code ItemUseMixin}: vanilla {@code readNbt} hard-clamps
 * every sign line to 15 characters, which silently eats the 2-character colour-code prefix that
 * the dye action prepends the next time the sign's NBT is read (chunk reload, world restart). From
 * MiscTweaks.
 *
 * <p>Widens the clamp by 2 characters when the line already starts with a colour code, so a line
 * that used all 15 visible characters keeps its colour instead of losing its last 2 characters.
 */
@Mixin(SignBlockEntity.class)
public class SignBlockEntityMixin extends BlockEntity {

	@WrapOperation(
		method = "readNbt",
		at = @At(
			value = "INVOKE",
			target = "Ljava/lang/String;substring(II)Ljava/lang/String;"
		)
	)
	private String retrotweaks$readIdentifyingDataSubstring(String instance, int beginIndex, int endIndex, Operation<String> original) {
		if (!Config.BLOCKS.colorSignsWithDye) {
			return original.call(instance, beginIndex, endIndex);
		}

		int lineLimit = 15;

		// Allow the first instance of a section character with its colour modifier.
		if (instance.contains("§")) {
			lineLimit = lineLimit + 2;
		}

		return original.call(instance, beginIndex, lineLimit);
	}
}
