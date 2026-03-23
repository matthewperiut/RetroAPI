package com.periut.retroapi.testmod;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers;
import com.periut.retroapi.register.block.RetroBlockAccess;
import com.periut.retroapi.register.item.RetroItemAccess;
import com.periut.retroapi.register.recipe.RetroRecipes;
import com.periut.retroapi.register.rendertype.RenderType;
import com.periut.retroapi.register.rendertype.RenderTypes;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.ornithemc.osl.entrypoints.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TestMod implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger("RetroAPI Test");

	public static final NamespacedIdentifier PIPE_RENDER_TYPE = RenderType.register(
		NamespacedIdentifiers.from("retroapi_test", "pipe"),
		ctx -> {
			ctx.renderAllLitFaces(4);
			return true;
		}
	);

	public static final int BLOCK_COUNT = 200;
	public static final Block[] BLOCKS = new Block[BLOCK_COUNT];
	public static final int ITEM_COUNT = 200;
	public static final Item[] ITEMS = new Item[ITEM_COUNT];

	public static Block TEST_BLOCK;
	public static Block COLOR_BLOCK;
	public static Block PIPE_BLOCK;
	public static Item TEST_ITEM;

	private static NamespacedIdentifier id(String name) {
		return NamespacedIdentifiers.from("retroapi_test", name);
	}

	@Override
	public void init() {
		LOGGER.info("RetroAPI Test Mod initializing");

		TEST_BLOCK = RetroBlockAccess.create(Material.STONE)
			.sounds(Block.STONE_SOUNDS)
			.strength(1.5f, 10.0f)
			.texture(id("test_block"))
			.register(id("test_block"));

		COLOR_BLOCK = RetroBlockAccess.of(new ColorBlock(RetroBlockAccess.allocateId(), Material.STONE))
			.sounds(Block.STONE_SOUNDS)
			.strength(1.5f, 10.0f)
			.renderType(RenderTypes.BLOCK)
			.register(id("color_block"));

		PIPE_BLOCK = RetroBlockAccess.create(Material.STONE)
			.sounds(Block.METAL_SOUNDS)
			.strength(2.0f)
			.nonOpaque()
			.bounds(4 / 16.0F, 4 / 16.0F, 4 / 16.0F, 12 / 16.0F, 12 / 16.0F, 12 / 16.0F)
			.renderType(PIPE_RENDER_TYPE)
			.sprite(4)
			.register(id("pipe"));

		TEST_ITEM = RetroItemAccess.create()
			.maxStackSize(64)
			.texture(id("test_item"))
			.register(id("test_item"));

		for (int i = 0; i < BLOCK_COUNT; i++) {
			BLOCKS[i] = RetroBlockAccess.create(Material.STONE)
				.sounds(Block.STONE_SOUNDS)
				.strength(1.5f)
				.sprite(Block.COBBLESTONE.getSprite(0))
				.register(id("block_" + i));
		}

		for (int i = 0; i < ITEM_COUNT; i++) {
			ITEMS[i] = RetroItemAccess.create()
				.maxStackSize(64)
				.register(id("item_" + i));
		}

		RetroRecipes.addShaped(new ItemStack(Block.PLANKS), "SSS", "SSS", "SSS", 'S', new ItemStack(Item.STICK));

		LOGGER.info("Registered test_block, color_block, pipe, test_item, + " + BLOCK_COUNT + " numbered blocks, + " + ITEM_COUNT + " numbered items");
	}
}
