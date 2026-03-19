package com.periut.retroapi.register.blockentity;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import com.periut.retroapi.mixin.register.BlockEntityAccessor;
import net.minecraft.block.entity.BlockEntity;

import java.util.function.Supplier;

public class RetroBlockEntityType<T extends BlockEntity> {
	private final NamespacedIdentifier id;
	private final Class<T> clazz;
	private final Supplier<T> factory;

	public RetroBlockEntityType(NamespacedIdentifier id, Class<T> clazz, Supplier<T> factory) {
		this.id = id;
		this.clazz = clazz;
		this.factory = factory;
		BlockEntityAccessor.invokeRegister(clazz, id.toString());
	}

	public T create() {
		return factory.get();
	}

	public NamespacedIdentifier getId() {
		return id;
	}
}
