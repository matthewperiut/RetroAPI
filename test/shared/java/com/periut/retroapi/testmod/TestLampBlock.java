package com.periut.retroapi.testmod;

import com.periut.retroapi.state.RetroBlockState;
import com.periut.retroapi.state.RetroBoolProperty;
import com.periut.retroapi.state.RetroIntProperty;
import com.periut.retroapi.state.RetroStates;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

/**
 * Exercises the flattened-state platform end to end: two properties (2 x 10 = 20 states,
 * deliberately past the 16-state nibble so xmeta engages), a blockstate JSON with lit
 * variants (model render type auto-wired), and a right-click that walks the state space.
 */
public class TestLampBlock extends Block {

	public static final RetroBoolProperty LIT = RetroBoolProperty.of("lit");
	public static final RetroIntProperty AGE = RetroIntProperty.of("age", 0, 9);

	public TestLampBlock(int id) {
		super(id, Material.STONE);
	}

	@Override
	public boolean onUse(World world, int x, int y, int z, PlayerEntity player) {
		RetroBlockState state = RetroStates.get(world, x, y, z);
		if (state == null) {
			return false;
		}
		// Cycle: bump age, toggle lit on wrap; exercises with(), set(), sync and re-render.
		int age = state.get(AGE);
		RetroBlockState next = age == 9
			? state.with(AGE, 0).with(LIT, !state.get(LIT))
			: state.with(AGE, age + 1);
		RetroStates.set(world, x, y, z, next);
		if (!world.isRemote) {
			player.sendMessage("lamp -> " + next + " (index " + next.getIndex() + ")");
		}
		return true;
	}
}
