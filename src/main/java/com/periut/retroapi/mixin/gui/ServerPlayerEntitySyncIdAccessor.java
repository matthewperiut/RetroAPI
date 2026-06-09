package com.periut.retroapi.mixin.gui;

import net.minecraft.entity.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerPlayerEntity.class)
public interface ServerPlayerEntitySyncIdAccessor {

	@Accessor("screenHandlerSyncId")
	int retroapi$getScreenHandlerSyncId();

	@Invoker("incrementScreenHandlerSyncId")
	void retroapi$incrementScreenHandlerSyncId();
}
