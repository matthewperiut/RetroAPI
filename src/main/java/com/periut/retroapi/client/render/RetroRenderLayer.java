package com.periut.retroapi.client.render;

/**
 * Render layers, modern naming mapped onto Beta's two render passes:
 * SOLID and CUTOUT draw in pass 0 (the terrain pass, which alpha-tests; CUTOUT exists as
 * intent metadata so the mapping can improve later), TRANSLUCENT draws in pass 1
 * (vanilla's water pass, sorted after opaque geometry).
 */
public enum RetroRenderLayer {
	SOLID(0),
	CUTOUT(0),
	TRANSLUCENT(1);

	private final int pass;

	RetroRenderLayer(int pass) {
		this.pass = pass;
	}

	public int getPass() {
		return pass;
	}
}
