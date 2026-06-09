package com.periut.retroapi.testmod.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets the headless conversion scenarios stop the dedicated server gracefully once their work is
 * done - flipping {@code running} to false ends the main loop, the JVM exits, and the Gradle run
 * task completes (so a scenario is a finite, scriptable step).
 */
@Mixin(MinecraftServer.class)
public interface ServerControlAccessor {
	@Accessor("running")
	void retroapi_test$setRunning(boolean running);
}
