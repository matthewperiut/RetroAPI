package com.periut.retrotweaks.mixin.client;

import com.periut.retrotweaks.compat.ApiBridge;
import com.periut.retrotweaks.feature.options.ModOptions;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The half of the FPS limit slider that actually paces frames: a numeric cap via
 * {@code Display.sync(fps)}, or VSync via {@code Display.setVSyncEnabled}, once every pass through
 * the main loop. From UniTweaks (mixin.tweaks.fpslimitslider.MinecraftMixin), extended with the
 * VSync stop.
 *
 * <p>{@link com.periut.retrotweaks.mixin.client.render.FpsLimitMixin} only stops vanilla's
 * own three-step Performance option from overriding the slider's chosen target; without this half
 * nothing ever blocks the thread to hold the frame rate down, which is why the slider "does nothing"
 * (finding #51). Injected right after {@code run()}'s second {@code logGlError("Post render")} call
 * - this frame's tick, render and resize handling are all done and the loop's own fps counter is
 * about to reset - the same point UniTweaks hooks.
 *
 * <p><b>VSync and RetroDragon.</b> {@code Display} here is compiled against the real LWJGL 2 class,
 * which {@code setVSyncEnabled(boolean)} is confirmed to have. RetroDragon does not need a separate
 * bridge for this the way {@code compat/ApiBridge} does for StationAPI/RetroAPI: it ships its own
 * {@code org.lwjgl.opengl.Display} - same fully-qualified name, same {@code setVSyncEnabled(boolean)}
 * signature - as a drop-in compatibility shim for exactly this kind of old-mod call, and RetroTweaks
 * cannot link against two classes of the same name at once, so whichever one is actually on the
 * classpath at runtime is the one this call reaches: the real LWJGL 2 class with RetroDragon absent,
 * RetroDragon's shim with it present. Raw GLFW would have been the wrong target regardless: RetroDragon
 * defaults to an SDL3 backend where GLFW is never initialised, so a direct {@code glfwSwapInterval}
 * call would silently do nothing for most RetroDragon users - {@code Display.setVSyncEnabled}
 * branches to SDL's own swap-interval call in that case, which is why it is used unconditionally
 * below rather than only when {@link ModOptions#fpsLimitActive()} (which stands down for RetroDragon)
 * is true.
 */
@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public class FpsLimitSyncMixin {

	/**
	 * The VSync state last pushed to the driver, so the (stateful, driver-level) setter only runs on
	 * an actual change rather than once per frame. Null until the first frame, so that frame always
	 * pushes an explicit state and the driver is never left at whatever it happened to default to.
	 */
	@Unique
	private static Boolean retrotweaks$vsyncApplied = null;

	/**
	 * Last cap pushed into RetroDragon, so the state is only written when it actually changes rather
	 * than every frame. Null until the first push. 0 means "cleared", which is what leaving CAPPED
	 * has to write - see {@link #retrotweaks$sync}.
	 */
	@Unique
	private static Integer retrotweaks$limitApplied = null;

	@Inject(method = "run",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;logGlError(Ljava/lang/String;)V", ordinal = 1),
		require = 1)
	private void retrotweaks$sync(CallbackInfo ci) {
		// VSync: applies with or without RetroDragon, so this is gated on the config toggle alone,
		// not on ModOptions.fpsLimitActive() - see the class doc for why a driver swap interval does
		// not fight RetroDragon's own pacing the way the numeric cap below would.
		if (ModOptions.fpsLimitEnabled()) {
			boolean wantVSync = ModOptions.frameLimitMode() == ModOptions.FrameLimitMode.VSYNC;
			if (retrotweaks$vsyncApplied == null || retrotweaks$vsyncApplied != wantVSync) {
				Display.setVSyncEnabled(wantVSync);
				retrotweaks$vsyncApplied = wantVSync;
			}
		}

		// Numeric cap. RetroDragon 0.1.6 added a real frame limiter to its own frame pacer, so the
		// cap no longer has to stand down for it - it just has to be expressed the way RetroDragon
		// can act on. Under RetroDragon that means pushing the target into RetroSettings, which its
		// pacer reads; without it, vanilla LWJGL 2's Display.sync does the per-frame sleep itself.
		//
		// The two are shaped differently and that is the whole subtlety here. Display.sync is a sleep
		// that must run EVERY frame and stops mattering the moment you stop calling it. RetroDragon's
		// limit is STATE: set it once and it holds, so leaving CAPPED for VSync or Unlimited has to
		// clear it explicitly or the old cap silently stays in force.
		if (!ModOptions.fpsLimitEnabled()) return;
		boolean capped = ModOptions.frameLimitMode() == ModOptions.FrameLimitMode.CAPPED;

		if (ApiBridge.hasFrameLimit()) {
			int wanted = capped ? ModOptions.frameLimit() : 0;
			if (retrotweaks$limitApplied == null || retrotweaks$limitApplied != wanted) {
				ApiBridge.setFrameLimit(wanted);
				retrotweaks$limitApplied = wanted;
			}
			return;
		}

		// No RetroDragon frame limiter (absent, or older than 0.1.6): vanilla's own sleep.
		if (capped) {
			Display.sync(ModOptions.frameLimit());
		}
	}
}
