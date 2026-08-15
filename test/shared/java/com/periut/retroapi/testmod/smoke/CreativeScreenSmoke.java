package com.periut.retroapi.testmod.smoke;

/**
 * The creative screen's own smoke check, in its own class ON PURPOSE.
 *
 * <p>It needs locals of type {@code Minecraft}, and the verifier resolves the types named in a
 * class's stack map frames when that class is LOADED - every method's, not just the one being called.
 * Leaving this in {@link SmokeTest} therefore made the dedicated server try to load
 * {@code ClientPlayerEntity} the moment it ran any check at all, and refuse. Client-typed code lives
 * in a class only the client ever names.
 */
final class CreativeScreenSmoke {
	private CreativeScreenSmoke() {
	}

	/**
	 * The creative screen's parts: its textures resolve, its tabs have enough to fill a page, and -
	 * when a player exists to build one for - its container puts the slots where modern puts them.
	 *
	 * <p>A missing texture is the failure that makes the screen look like nothing at all, and it is
	 * invisible to every other check, because a texture is only ever loaded when it is first drawn.
	 */
	/**
	 * The spectator hooks are LIVE.
	 *
	 * <p>Every one of these is an injector that can fail to find its target without stopping the game,
	 * and the symptom is always the same shrug: "spectator does not work". Asserting the handler
	 * methods exist on the target classes turns that into a named failure.
	 */
	static void spectatorHooks() {
		requireHook(net.minecraft.client.render.entity.PlayerEntityRenderer.class, "retroapi$hideSpectators");
		requireHook(net.minecraft.client.gui.hud.InGameHud.class, "retroapi$hideHotbar");
		requireHook(net.minecraft.client.gui.hud.InGameHud.class, "retroapi$hideCrosshair");
		requireHook(net.minecraft.client.InteractionManager.class, "retroapi$hideStatusBarsInCreative");
		requireHook(net.minecraft.entity.Entity.class, "retroapi$spectatorHasNoCollision");
		requireHook(net.minecraft.entity.LivingEntity.class, "retroapi$noclipFlight");
	}

	private static void requireHook(Class<?> target, String handler) {
		for (java.lang.reflect.Method method : target.getDeclaredMethods()) {
			if (method.getName().contains(handler)) {
				return;
			}
		}
		throw new IllegalStateException(handler + " never applied to " + target.getSimpleName());
	}

	/**
	 * Every test-mod item resolves to a real sprite slot.
	 *
	 * <p>This is the check that tells a missing TEXTURE from a missing UPLOAD. If an item's sprite
	 * index is 0 it never got one and the builder is at fault; if it is a real expanded-atlas slot and
	 * the item still draws blue in game, the index is right and the pixels never reached the GPU -
	 * which is the renderer's business, not RetroAPI's.
	 */
	static void itemSprites() {
		record Entry(String name, net.minecraft.item.Item item) {
		}

		java.util.List<Entry> items = java.util.List.of(
			new Entry("test_item", com.periut.retroapi.testmod.TestMod.TEST_ITEM),
			new Entry("paxel", com.periut.retroapi.testmod.TestMod.PAXEL),
			new Entry("dynamic_tool", com.periut.retroapi.testmod.TestMod.DYNAMIC_TOOL),
			new Entry("code_layered", com.periut.retroapi.testmod.TestMod.CODE_LAYERED),
			new Entry("anim_item", com.periut.retroapi.testmod.TestMod.ANIM_ITEM),
			new Entry("layer_item", com.periut.retroapi.testmod.TestMod.LAYER_ITEM));

		// The atlases are composited lazily, the first time the game asks for them. Ask, or this
		// check reads the un-expanded 256x256 sheet and reports a bug that is only a timing artefact.
		Object game = net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance();
		if (game instanceof net.minecraft.client.Minecraft minecraft) {
			minecraft.textureManager.getTextureId("/gui/items.png");
			minecraft.textureManager.getTextureId("/terrain.png");
		}

		StringBuilder report = new StringBuilder();
		for (Entry entry : items) {
			if (entry.item() == null) {
				throw new IllegalStateException(entry.name() + " was never registered");
			}
			int slot = entry.item().getTextureId(0);
			report.append(entry.name()).append("=").append(slot).append(' ');

			// Slot 0 is the top-left of the vanilla sheet: what an item gets when nothing assigned it
			// one. A modded item landing there is the bug that draws as somebody else's sprite.
			if (slot <= 0) {
				throw new IllegalStateException(entry.name() + " has no sprite slot (got " + slot
					+ "); assigned slots so far: " + report);
			}
		}

		int columns = 16;
		int atlasSize = com.periut.retroapi.client.texture.AtlasExpander.itemAtlasSize;
		int spriteSize = Math.max(1, com.periut.retroapi.client.texture.AtlasExpander.itemSpriteSize);
		int rows = atlasSize / spriteSize;
		com.periut.retroapi.testmod.TestMod.LOGGER.info(
			"[smoke] item atlas {}x{}, sprite {}px, {} rows", atlasSize, atlasSize, spriteSize, rows);
		for (Entry entry : items) {
			int slot = entry.item().getTextureId(0);
			if (slot >= columns * rows) {
				throw new IllegalStateException(entry.name() + " sits at slot " + slot
					+ ", outside the " + columns + "x" + rows + " item atlas - it can only draw garbage");
			}
		}

		com.periut.retroapi.testmod.TestMod.LOGGER.info("[smoke] item sprite slots: {}", report.toString().trim());

		// Every registered sprite had an image to composite.
		if (!com.periut.retroapi.client.texture.AtlasExpander.missingTextures.isEmpty()) {
			throw new IllegalStateException("sprites with no image, they will draw empty: "
				+ com.periut.retroapi.client.texture.AtlasExpander.missingTextures);
		}

		// And the pixels really landed in the slot. This is the line between "RetroAPI composited it"
		// and "something downstream never uploaded it" - the two look identical in game.
		java.awt.image.BufferedImage atlas = com.periut.retroapi.client.texture.AtlasExpander.lastItemAtlas;
		if (atlas == null) {
			throw new IllegalStateException("the item atlas was never composited");
		}
		for (Entry entry : items) {
			int slot = entry.item().getTextureId(0);
			int x = (slot % columns) * spriteSize;
			int y = (slot / columns) * spriteSize;
			boolean anyPixel = false;
			for (int px = 0; px < spriteSize && !anyPixel; px++) {
				for (int py = 0; py < spriteSize && !anyPixel; py++) {
					if ((atlas.getRGB(x + px, y + py) >>> 24) != 0) {
						anyPixel = true;
					}
				}
			}
			if (!anyPixel) {
				throw new IllegalStateException(entry.name() + " has an empty atlas slot (" + slot
					+ " at " + x + "," + y + "): nothing was composited there");
			}
		}
	}

	static void run() {
		String[] textures = {
			"/assets/retroapi/gui/creative_panel.png",
			"/assets/retroapi/gui/creative_panel_search.png",
			"/assets/retroapi/gui/creative_tabs.png",
			"/assets/retroapi/gui/creative_widgets.png",
		};
		for (String texture : textures) {
			try (java.io.InputStream in = SmokeTest.class.getResourceAsStream(texture)) {
				if (in == null) {
					throw new IllegalStateException("missing creative texture: " + texture);
				}
			} catch (java.io.IOException e) {
				throw new IllegalStateException("could not read " + texture, e);
			}
		}

		// The first page is 45 slots; a vanilla tab that cannot fill one would leave holes.
		int building = com.periut.retroapi.itemgroup.VanillaItemGroups.BUILDING_BLOCKS.collect().size();
		if (building < 20) {
			throw new IllegalStateException("the building blocks tab only has " + building + " entries");
		}
		if (com.periut.retroapi.itemgroup.VanillaItemGroups.SEARCH.collect().size() < 45) {
			throw new IllegalStateException("the search tab cannot even fill one page");
		}

		Object game = net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance();
		net.minecraft.entity.player.PlayerEntity player =
			game instanceof net.minecraft.client.Minecraft minecraft ? minecraft.player : null;
		if (player == null) {
			// No world open in this run; the geometry check needs an inventory to build slots over.
			return;
		}

		com.periut.retroapi.gamemode.screen.CreativeScreenHandler handler =
			new com.periut.retroapi.gamemode.screen.CreativeScreenHandler(player);

		// Modern's layout: 45 picker slots from (9,18) on an 18 pitch, hotbar at y=112, destroy at 173.
		net.minecraft.screen.slot.Slot first = handler.getSlot(0);
		if (first.x != 9 || first.y != 18) {
			throw new IllegalStateException("the first picker slot is at " + first.x + "," + first.y);
		}
		net.minecraft.screen.slot.Slot lastPicker = handler.getSlot(44);
		if (lastPicker.x != 9 + 8 * 18 || lastPicker.y != 18 + 4 * 18) {
			throw new IllegalStateException("the last picker slot is at " + lastPicker.x + "," + lastPicker.y);
		}
		net.minecraft.screen.slot.Slot hotbar = handler.getSlot(45);
		if (hotbar.y != 112) {
			throw new IllegalStateException("the hotbar row is at y=" + hotbar.y);
		}
		net.minecraft.screen.slot.Slot destroy = handler.getSlot(54);
		if (destroy.x != 173 || destroy.y != 112) {
			throw new IllegalStateException("the destroy slot is at " + destroy.x + "," + destroy.y);
		}

		// Filling the picker actually reaches the slots, and a picker slot refuses to be filled by hand.
		handler.setContents(java.util.List.of(new net.minecraft.item.ItemStack(net.minecraft.block.Block.STONE)), 0);
		if (handler.getSlot(0).getStack() == null) {
			throw new IllegalStateException("setContents did not reach the slots");
		}
		if (handler.getSlot(0).canInsert(new net.minecraft.item.ItemStack(net.minecraft.block.Block.DIRT))) {
			throw new IllegalStateException("a picker slot accepted an item being put into it");
		}
	}
}
