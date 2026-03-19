package com.periut.retroapi.mixin.network;

import net.minecraft.server.entity.mob.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerPlayerEntity.class)
public interface ServerPlayerEntityAccessor {
	@Invoker("incrementSyncId")
	void invokeIncrementSyncId();

	@Accessor("menuId")
	int getMenuId();
}
