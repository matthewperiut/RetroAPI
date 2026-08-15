package com.periut.retrotweaks.mixin.client;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.feature.options.ModKeyBindings;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.GameOptions;

import org.lwjgl.input.Keyboard;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Separates the debug overlay from the profiler graph. From MojangFix.
 *
 * <p>In b1.7.3 the F3 key toggles both at once, so anyone who wants the coordinates also gets the
 * pie chart drawn over the world. Modern Minecraft splits them: F3 shows the text, Shift+F3 adds
 * the graph. That is what "Modern Debug Graph" turns on.
 *
 * <p>With "Debug Graph Needs Shift+F3" off, F3 no longer decides the graph at all - pressing the
 * dedicated "Debug Graph" key (see {@link ModKeyBindings#DEBUG_GRAPH}) toggles it on its own, and
 * F3 only gates whether anything is drawn, same as it does in the modern-toggle branch.
 */
@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public class DebugGraphMixin {

	@Unique
	private static boolean retrotweaks$graphVisible;

	/** Records whether shift was held for the F3 press that is about to be handled. */
	@WrapOperation(method = "tick", at = @At(value = "FIELD",
		target = "Lnet/minecraft/client/option/GameOptions;debugHud:Z", opcode = Opcodes.PUTFIELD), require = 0)
	private void retrotweaks$noteShift(GameOptions options, boolean value, Operation<Void> original) {
		original.call(options, value);
		if (!Config.HUD.debug.enableDebugGraph) return;
		if (Config.HUD.debug.debugGraphModernToggle) {
			retrotweaks$graphVisible = value && (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
		}
	}

	/**
	 * Toggles the graph on its own dedicated key when "Debug Graph Needs Shift+F3" is off. From
	 * MojangFix's {@code DebugGraphMixin#onKey}.
	 */
	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isWorldRemote()Z", ordinal = 0))
	private void retrotweaks$onDebugGraphKey(CallbackInfo ci) {
		if (!Config.HUD.debug.enableDebugGraph || Config.HUD.debug.debugGraphModernToggle) return;
		if (Keyboard.getEventKey() == ModKeyBindings.DEBUG_GRAPH.code) {
			retrotweaks$graphVisible = !retrotweaks$graphVisible;
		}
	}

	/** Whether the profiler graph is drawn this frame. */
	@WrapOperation(method = "run", at = @At(value = "FIELD",
		target = "Lnet/minecraft/client/option/GameOptions;debugHud:Z", opcode = Opcodes.GETFIELD), require = 0)
	private boolean retrotweaks$graphVisible(GameOptions options, Operation<Boolean> original) {
		boolean debugHud = original.call(options);
		if (!Config.HUD.debug.enableDebugGraph) return debugHud;
		return debugHud && retrotweaks$graphVisible;
	}
}
