package com.periut.retrotweaks.mixin.block;

import net.minecraft.item.ToolItem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes each tool's own mining speed, which vanilla keeps private. */
@Mixin(ToolItem.class)
public interface ToolItemAccessor {

	@Accessor("miningSpeed")
	float retrotweaks$getMiningSpeed();
}
