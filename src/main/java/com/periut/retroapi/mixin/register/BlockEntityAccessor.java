package com.periut.retroapi.mixin.register;

import net.minecraft.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockEntity.class)
public interface BlockEntityAccessor {

	@Invoker("create")
	static void retroapi$create(Class<? extends BlockEntity> blockEntityClass, String id) {
		throw new AssertionError();
	}
}
