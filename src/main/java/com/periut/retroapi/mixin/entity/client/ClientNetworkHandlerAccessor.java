package com.periut.retroapi.mixin.entity.client;

import net.minecraft.client.network.ClientNetworkHandler;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes the client network handler's private {@code getEntity(int)} so the entity spawn listener can
 * resolve an owner entity by id (for {@link com.periut.retroapi.entity.spawn.RetroHasOwner} projectiles).
 */
@Mixin(ClientNetworkHandler.class)
public interface ClientNetworkHandlerAccessor {
	@Invoker("getEntity")
	Entity retroapi$getEntity(int id);
}
