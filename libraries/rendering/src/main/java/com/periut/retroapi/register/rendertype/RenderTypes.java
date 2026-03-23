package com.periut.retroapi.register.rendertype;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers;

/**
 * Constants for all vanilla block render types in Beta 1.7.3.
 * <p>
 * Use these with {@link RetroBlockAccess#retroapi$setRenderType} or pass to
 * {@link RenderType#resolve} to get the numeric ID.
 */
public final class RenderTypes {
	public static final NamespacedIdentifier BLOCK = NamespacedIdentifiers.from("minecraft", "block");
	public static final NamespacedIdentifier CROSS = NamespacedIdentifiers.from("minecraft", "cross");
	public static final NamespacedIdentifier TORCH = NamespacedIdentifiers.from("minecraft", "torch");
	public static final NamespacedIdentifier FIRE = NamespacedIdentifiers.from("minecraft", "fire");
	public static final NamespacedIdentifier LIQUID = NamespacedIdentifiers.from("minecraft", "liquid");
	public static final NamespacedIdentifier REDSTONE_WIRE = NamespacedIdentifiers.from("minecraft", "redstone_wire");
	public static final NamespacedIdentifier PLANT = NamespacedIdentifiers.from("minecraft", "plant");
	public static final NamespacedIdentifier DOOR = NamespacedIdentifiers.from("minecraft", "door");
	public static final NamespacedIdentifier LADDER = NamespacedIdentifiers.from("minecraft", "ladder");
	public static final NamespacedIdentifier RAIL = NamespacedIdentifiers.from("minecraft", "rail");
	public static final NamespacedIdentifier STAIRS = NamespacedIdentifiers.from("minecraft", "stairs");
	public static final NamespacedIdentifier FENCE = NamespacedIdentifiers.from("minecraft", "fence");
	public static final NamespacedIdentifier LEVER = NamespacedIdentifiers.from("minecraft", "lever");
	public static final NamespacedIdentifier CACTUS = NamespacedIdentifiers.from("minecraft", "cactus");
	public static final NamespacedIdentifier BED = NamespacedIdentifiers.from("minecraft", "bed");
	public static final NamespacedIdentifier REPEATER = NamespacedIdentifiers.from("minecraft", "repeater");
	public static final NamespacedIdentifier PISTON_BASE = NamespacedIdentifiers.from("minecraft", "piston_base");
	public static final NamespacedIdentifier PISTON_HEAD = NamespacedIdentifiers.from("minecraft", "piston_head");
#if MC_VER >= 180
	public static final NamespacedIdentifier PANE = NamespacedIdentifiers.from("minecraft", "pane");
	public static final NamespacedIdentifier STEM = NamespacedIdentifiers.from("minecraft", "stem");
	public static final NamespacedIdentifier VINE = NamespacedIdentifiers.from("minecraft", "vine");
	public static final NamespacedIdentifier FENCE_GATE = NamespacedIdentifiers.from("minecraft", "fence_gate");
	public static final NamespacedIdentifier BLOCK_ENTITY = NamespacedIdentifiers.from("minecraft", "block_entity");
#endif
#if MC_VER >= 190
	public static final NamespacedIdentifier LILY_PAD = NamespacedIdentifiers.from("minecraft", "lily_pad");
	public static final NamespacedIdentifier CAULDRON = NamespacedIdentifiers.from("minecraft", "cauldron");
	public static final NamespacedIdentifier BREWING_STAND = NamespacedIdentifiers.from("minecraft", "brewing_stand");
	public static final NamespacedIdentifier END_PORTAL_FRAME = NamespacedIdentifiers.from("minecraft", "end_portal_frame");
	public static final NamespacedIdentifier DRAGON_EGG = NamespacedIdentifiers.from("minecraft", "dragon_egg");
#endif

	private RenderTypes() {}
}
