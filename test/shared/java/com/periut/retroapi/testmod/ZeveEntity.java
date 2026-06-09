package com.periut.retroapi.testmod;

import com.periut.retroapi.entity.spawn.RetroMobSpawnData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers;

/**
 * Test mob {@code retroapi_test:zeve}: a bare biped that just stands around. Extends
 * {@link LivingEntity} directly - the common base player/zombie/skeleton all inherit - so it gets
 * health (base ctor sets 10), damage/death, gravity, and the idle "look at nearby players" behavior,
 * but NO pathfinding AI (that lives in the {@code PathAwareEntity}/{@code MonsterEntity} subtree).
 * Wears the zombie skin via the protected {@code texture} field, exactly like {@code ZombieEntity}'s
 * ctor does. Implements {@link RetroMobSpawnData} so the tracker sends it over RetroAPI's OSL mob
 * spawn channel in multiplayer (vanilla's spawn packet throws for unknown types).
 */
public class ZeveEntity extends LivingEntity implements RetroMobSpawnData {
	public static final NamespacedIdentifier ID = NamespacedIdentifiers.from("retroapi_test", "zeve");

	public ZeveEntity(World world) {
		super(world);
		this.texture = "/mob/zombie.png";
		// health = 10 already set by the LivingEntity ctor.
	}

	/** Test mob: never despawn, so it's still there to look at after relogs/world reloads. */
	@Override
	protected boolean canDespawn() {
		return false;
	}

	private int testTicks;

	/**
	 * DataTracker-sync probe: 10 seconds after spawning, the server sets the zeve on fire. The
	 * shared flags byte (DataTracker entry 0, bit 0) goes dirty -> EntityTrackerUpdateS2CPacket
	 * (0x28) must reach clients and the zeve must visibly burn. Checked in runTestClient.
	 */
	@Override
	public void tick() {
		super.tick();
		if (!this.world.isRemote && ++testTicks == 200) {
			this.fireTicks = 1200;
		}
	}

	@Override
	public NamespacedIdentifier getHandlerId() {
		return ID;
	}
}
