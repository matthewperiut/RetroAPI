package com.periut.retroapi.register.block;

import com.periut.retroapi.state.RetroBlockState;
import com.periut.retroapi.state.RetroFacing;
import com.periut.retroapi.util.RetroDirection;

/**
 * Per-face block textures declared in code - the furnace/dispenser look with no model JSON, no blockstate
 * file and no {@code getTexture} override.
 *
 * <p>Two shapes:
 * <ul>
 *   <li><b>oriented</b> ({@code RetroBlockAccess.sided(top, side, front[, bottom])}): the front follows the
 *       block's {@code facing} state, so one declaration covers all four (or six) orientations;</li>
 *   <li><b>absolute</b> ({@code RetroBlockAccess.textures(bottom, top, north, south, west, east)}): one
 *       texture nailed to each world face, for blocks that do not turn.</li>
 * </ul>
 *
 * <p>Sprites are looked up through live {@link RetroTexture} handles, so they stay correct after an atlas
 * rebuild (resource pack change, StationAPI resolution) instead of freezing a slot number at registration.
 */
public final class RetroFaceTextures {

	// Absolute mode: [bottom, top, north, south, west, east], entries may be null (fall back to the block's
	// own sprite). Oriented mode: the four roles below.
	private final RetroTexture[] absolute;
	private final RetroTexture front;
	private final RetroTexture side;
	private final RetroTexture top;
	private final RetroTexture bottom;

	private RetroFaceTextures(RetroTexture[] absolute, RetroTexture front, RetroTexture side,
			RetroTexture top, RetroTexture bottom) {
		this.absolute = absolute;
		this.front = front;
		this.side = side;
		this.top = top;
		this.bottom = bottom;
	}

	/** Oriented: front follows the facing state, everything else is side/top/bottom. */
	public static RetroFaceTextures oriented(RetroTexture top, RetroTexture side, RetroTexture front,
			RetroTexture bottom) {
		return new RetroFaceTextures(null, front, side, top, bottom != null ? bottom : top);
	}

	/** Absolute: a texture per world face, in vanilla face order (0 bottom, 1 top, 2 north, 3 south, 4 west, 5 east). */
	public static RetroFaceTextures absolute(RetroTexture[] faces) {
		return new RetroFaceTextures(faces.clone(), null, null, null, null);
	}

	/** The sprite this block's front face uses - also the sensible particle/inventory sprite. */
	public RetroTexture primary() {
		if (front != null) return front;
		if (absolute != null) {
			for (RetroTexture tex : absolute) {
				if (tex != null) return tex;
			}
		}
		return side != null ? side : top;
	}

	/**
	 * The sprite for a world face given the block's state.
	 *
	 * @param face  vanilla face index: 0 bottom, 1 top, 2 north, 3 south, 4 west, 5 east
	 * @param state the block's state, used to find its {@code facing} property; may be null
	 * @return the atlas sprite index, or -1 to let vanilla decide
	 */
	public int spriteFor(int face, RetroBlockState state) {
		if (absolute != null) {
			RetroTexture tex = face >= 0 && face < absolute.length ? absolute[face] : null;
			return tex != null ? tex.id : -1;
		}
		return resolveOriented(face, facingOf(state));
	}

	private int resolveOriented(int face, RetroDirection facing) {
		if (facing == null) {
			facing = RetroDirection.NORTH;
		}
		RetroTexture tex;
		if (face == facing.face()) {
			tex = front;
		} else if (facing.isHorizontal()) {
			tex = face == 1 ? top : face == 0 ? bottom : side;
		} else {
			// Vertical facing (dispenser style): the back gets the bottom texture, the ring gets the side.
			tex = face == facing.opposite().face() ? bottom : side;
		}
		if (tex == null) tex = side != null ? side : top;
		return tex != null ? tex.id : -1;
	}

	/** The facing a state points, from either the 6-way or the 4-way {@code facing} property. Null if it has none. */
	public static RetroDirection facingOf(RetroBlockState state) {
		if (state == null) {
			return null;
		}
		// Both the 4-way and the 6-way property are named "facing", so one lookup covers either.
		Object value = state.getByName(RetroDirection.PROPERTY.name());
		if (value instanceof RetroDirection direction) {
			return direction;
		}
		if (value instanceof RetroFacing facing) {
			return facing.toDirection();
		}
		return null;
	}
}
