package com.periut.retroapi.testmod;

import com.periut.retroapi.dimension.CustomPortal;
import com.periut.retroapi.dimension.HasTeleportationManager;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.dimension.PortalForcer;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

/**
 * Test {@link CustomPortal} block: walk into it to travel to the RetroAPI test dimension (and walk
 * into it again over there to come back). Non-colliding like a vanilla nether portal, so walking
 * through it fires {@code onEntityCollision} -> {@code tickPortalCooldown()} -> (after the cooldown)
 * the redirected dimension change to this portal's manager.
 */
public class TestPortalBlock extends Block implements CustomPortal {
	public TestPortalBlock(int id) {
		super(id, Material.GLASS);
	}

	@Override
	public Box getCollisionShape(World world, int x, int y, int z) {
		return null; // walk-through
	}

	@Override
	public boolean isOpaque() {
		return false; // non-opaque so the player inside it doesn't suffocate
	}

	@Override
	public void onEntityCollision(World world, int x, int y, int z, Entity entity) {
		if (entity instanceof PlayerEntity && entity.vehicle == null && entity.passenger == null) {
			((HasTeleportationManager) entity).setTeleportationManager(this);
			entity.tickPortalCooldown();
		}
	}

	@Override
	public NamespacedIdentifier getDimension(PlayerEntity player) {
		return TestMod.TEST_DIMENSION.getId();
	}

	@Override
	public PortalForcer getTravelAgent(PlayerEntity player) {
		return new PortalForcer();
	}
}
