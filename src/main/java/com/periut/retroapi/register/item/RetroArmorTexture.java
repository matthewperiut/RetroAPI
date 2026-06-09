package com.periut.retroapi.register.item;

/**
 * Implemented by a modded armor item to supply its own WORN texture (the armor drawn on the
 * player), replacing beta's fixed {@code /armor/<name>_<layer>.png} path. RetroAPI's
 * {@code PlayerArmorTextureMixin} consumes this when the renderer binds the armor texture, so
 * a modded set shows its own art on the body instead of a vanilla material's.
 *
 * <p>Beta armor draws in two layers: layer 1 is the helmet/chestplate/boots sheet, layer 2 is
 * the leggings sheet. Return the classpath resource for each, e.g.
 * {@code "/assets/mymod/textures/armor/zanite_1.png"}.</p>
 */
public interface RetroArmorTexture {

	/** The worn-armor texture resource for the given layer (1 = body/feet sheet, 2 = legs sheet). */
	String getArmorTexture(int layer);
}
