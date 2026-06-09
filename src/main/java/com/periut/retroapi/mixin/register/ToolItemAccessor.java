package com.periut.retroapi.mixin.register;

import net.minecraft.item.ToolItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes a tool's material mining speed (wood 2, stone 4, iron 6, diamond 8, gold 12)
 * so the {@code mineable/<tool>} tag hook can apply tool speed to tagged blocks for ANY
 * {@link ToolItem} subclass, not just the vanilla pickaxe/axe/shovel classes.
 */
@Mixin(ToolItem.class)
public interface ToolItemAccessor {

	@Accessor("miningSpeed")
	float retroapi$getMiningSpeed();

	@Accessor("toolMaterial")
	net.minecraft.item.ToolMaterial retroapi$getToolMaterial();
}
