package com.periut.retroapi.mixin.register;

import net.minecraft.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockEntity.class)
public interface BlockEntityAccessor {
	@Invoker("register")
	static void invokeRegister(Class<? extends BlockEntity> clazz, String id) {
		throw new AssertionError();
	}
}
