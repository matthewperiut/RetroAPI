package com.periut.retroapi.testmod;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers;
import com.periut.retroapi.dimension.DimensionRegistration;
import com.periut.retroapi.dimension.RetroDimensions;
import com.periut.retroapi.entity.EntityRegistration;
import com.periut.retroapi.entity.RetroEntities;
import com.periut.retroapi.entity.client.MobFactory;
import com.periut.retroapi.register.block.RetroBlockAccess;
import com.periut.retroapi.register.item.RetroItemAccess;
import com.periut.retroapi.register.recipe.RetroRecipes;
import com.periut.retroapi.register.recipe.event.RecipeRegistrationCallback;
import com.periut.retroapi.register.rendertype.RenderType;
import com.periut.retroapi.register.rendertype.RenderTypes;
import net.minecraft.item.PickaxeItem;
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
	public static Block TEST_PORTAL;
	public static Block CRATE;
	public static Item TEST_ITEM;
	public static DimensionRegistration TEST_DIMENSION;
	public static EntityRegistration ZEVE;

	// Asset platform test content (tags, animation, states, models, item models).
	public static Block TAGGED_ORE;
	public static Block LAMP;
	public static Block ANIM_BLOCK;
	public static Item ANIM_ITEM;
	public static Item LAYER_ITEM;
	public static Item DEF_ITEM;

	// A data component for the headless round-trip self-check.
	public static com.periut.retroapi.component.RetroComponentType<Integer> TEST_COUNT;
	// A RECORD (compound) and a LIST component, to prove richer data types round-trip.
	public static com.periut.retroapi.component.RetroComponentType<int[]> TEST_PAIR;
	public static com.periut.retroapi.component.RetroComponentType<java.util.List<Integer>> TEST_LIST;

	private static NamespacedIdentifier id(String name) {
		return NamespacedIdentifiers.from("retroapi_test", name);
	}

	@Override
	public void init() {
		LOGGER.info("RetroAPI Test Mod initializing");

		TEST_BLOCK = RetroBlockAccess.create(Material.STONE)
			.sounds(Block.STONE_SOUND_GROUP)
			.strength(1.5f, 10.0f)
			.texture(id("test_block"))
			.register(id("test_block"));

		COLOR_BLOCK = RetroBlockAccess.of(new ColorBlock(RetroBlockAccess.allocateId(), Material.STONE))
			.sounds(Block.STONE_SOUND_GROUP)
			.strength(1.5f, 10.0f)
			.renderType(RenderTypes.BLOCK)
			.alwaysEffectiveTool()
			.alwaysDrops()
			.register(id("color_block"));

		PIPE_BLOCK = RetroBlockAccess.create(Material.STONE)
			.sounds(Block.METAL_SOUND_GROUP)
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

		// A modded block with an inventory block entity (crate). Exercises the modded-BE sidecar +
		// conversion path (whole-BE strip/restore, and BE injection/extraction during flattening).
		CRATE = RetroBlockAccess.create(Material.WOOD)
			.sounds(Block.WOOD_SOUND_GROUP)
			.strength(2.0f)
			.sprite(Block.CHEST.getTexture(0))
			.register(id("crate"));
		com.periut.retroapi.register.blockentity.RetroBlockEntities.register(id("crate"), TestCrateBlockEntity.class);

		// Walk-into-it portal to the test dimension. Given to the player on spawn (see GiveItemsMixin).
		TEST_PORTAL = RetroBlockAccess.of(new TestPortalBlock(RetroBlockAccess.allocateId()))
			.sounds(Block.GLASS_SOUND_GROUP)
			.sprite(Block.GLASS.getTexture(0))
			.register(id("test_portal"));

		for (int i = 0; i < BLOCK_COUNT; i++) {
			BLOCKS[i] = RetroBlockAccess.create(Material.STONE)
				.sounds(Block.STONE_SOUND_GROUP)
				.strength(1.5f)
				.sprite(Block.COBBLESTONE.getTexture(0))
				.register(id("block_" + i));
		}

		for (int i = 0; i < ITEM_COUNT; i++) {
			ITEMS[i] = RetroItemAccess.create()
				.maxStackSize(64)
				.register(id("item_" + i));
		}

		// Register recipes through the RetroAPI callback (fires after vanilla recipes are
		// registered; the list is then re-sorted). Bridged from StationAPI's recipe event
		// when StationAPI is present, so this listener works in both worlds.
		RecipeRegistrationCallback.EVENT.register(TestMod::registerRecipes);

		// Register a test dimension (direct registration, like the blocks above - no callback ordering
		// concern). RetroAPI's DimensionMixin makes its serial id resolvable; the server creates a
		// ReadOnlyServerWorld for it at startup and preloads its spawn region into DIM<serialId>/.
		TEST_DIMENSION = RetroDimensions.register(id("test_dim"), TestDimension::new);
		LOGGER.info("Registered test dimension {} (serial id {})", TEST_DIMENSION.getId(), TEST_DIMENSION.getSerialId());

		// Register the zeve test mob (direct registration, like blocks/dimensions above). The
		// MobFactory marks it living, so multiplayer spawns flow over the OSL mob spawn channel and
		// the client reconstructs it via this factory. One spawns at the player on join (GiveItemsMixin).
		ZEVE = RetroEntities.register(ZeveEntity.ID, ZeveEntity.class)
			.factory((MobFactory) ZeveEntity::new);
		LOGGER.info("Registered test entity {}", ZEVE.getId());

		// ---------------------------------------------------- asset platform tests --

		// Tags + mineable: code-tagged ore (drops only with a pickaxe, mined at tool speed).
		// The data file data/retroapi_test/tags/block/mineable/pickaxe.json adds this block
		// again (union is a no-op) plus minecraft:glass, proving vanilla-name resolution.
		TAGGED_ORE = RetroBlockAccess.create(Material.STONE)
			.sounds(Block.STONE_SOUND_GROUP)
			.strength(3.0f)
			.texture(id("tagged_ore"))
			.mineable(com.periut.retroapi.tag.RetroTool.PICKAXE)
			.register(id("tagged_ore"));

		// Flattened states + blockstate JSON models: 20 states (xmeta engages past 16),
		// lit=false/true variants in assets/retroapi_test/retroapi/blockstates/lamp.json.
		LAMP = RetroBlockAccess.of(new TestLampBlock(RetroBlockAccess.allocateId()))
			.sounds(Block.STONE_SOUND_GROUP)
			.strength(0.5f)
			.alwaysEffectiveTool()
			.alwaysDrops()
			.states(TestLampBlock.LIT, TestLampBlock.AGE)
			.register(id("lamp"));

		// Animated block texture, data-driven: anim_block.png is a 4-frame vertical strip
		// with anim_block.png.mcmeta (frametime 5, interpolate).
		ANIM_BLOCK = RetroBlockAccess.create(Material.STONE)
			.sounds(Block.STONE_SOUND_GROUP)
			.strength(0.5f)
			.texture(id("anim_block"))
			.register(id("anim_block"));

		// Animated item texture, code-driven: 3-frame strip, no mcmeta.
		ANIM_ITEM = RetroItemAccess.create()
			.maxStackSize(64)
			.register(id("anim_item"));
		com.periut.retroapi.register.block.RetroTexture animTex =
			com.periut.retroapi.register.block.RetroTextures.addAnimatedItemTexture(id("anim_item"), 3, 8);
		ANIM_ITEM.setTextureId(animTex.id);
		com.periut.retroapi.register.block.RetroTextures.trackItem(ANIM_ITEM, animTex);

		// Layered item model: models/item/layer_item.json (item/generated, layer0 + layer1
		// flattened onto one atlas slot at composite time).
		LAYER_ITEM = RetroItemAccess.create()
			.maxStackSize(64)
			.register(id("layer_item"));

		// Modern (26.1.2) item model DEFINITION: items/def_item.json is a composite of
		// two model refs; the second references minecraft:item/apple, resolved straight
		// off the vanilla items.png by name. Also exercises handheld-by-parent.
		DEF_ITEM = RetroItemAccess.create()
			.maxStackSize(64)
			.register(id("def_item"));

		TEST_COUNT = com.periut.retroapi.component.RetroComponents.register(
			id("test_count"), 0, com.periut.retroapi.component.RetroComponentType.INT);
		// compound(): writes a multi-field value into its own sub-compound (a record).
		TEST_PAIR = com.periut.retroapi.component.RetroComponents.register(id("test_pair"), new int[]{0, 0},
			com.periut.retroapi.component.RetroComponentType.compound(
				(nbt, v) -> { nbt.putInt("a", v[0]); nbt.putInt("b", v[1]); },
				nbt -> new int[]{nbt.getInt("a"), nbt.getInt("b")}));
		TEST_LIST = com.periut.retroapi.component.RetroComponents.register(id("test_list"), java.util.Collections.emptyList(),
			com.periut.retroapi.component.RetroComponentType.listOf(com.periut.retroapi.component.RetroComponentType.INT));

		// Register achievements AFTER blocks/items so icons can reference registered content.
		TestAchievements.register();

		LOGGER.info("Registered test_block, color_block, pipe, test_item, + " + BLOCK_COUNT + " numbered blocks, + " + ITEM_COUNT + " numbered items");
	}

	private static void registerRecipes() {
		// (existing) bare-Item ingredient -> WILDCARD: a stick of any metadata works.
		RetroRecipes.addShaped(new ItemStack(Block.PLANKS), "SSS", "SSS", "SSS", 'S', Item.STICK);

		// (a) Metadata-specific shaped recipe: 4 red wool (meta 14) in a square -> 1 cobblestone.
		//     Proves exact-damage matching (white wool meta 0 must NOT craft this).
		RetroRecipes.addShaped(new ItemStack(Block.COBBLESTONE),
			"WW", "WW", 'W', new ItemStack(Block.WOOL, 1, 14));

		// (b) Wildcard shapeless recipe: a bare Block ingredient matches any metadata.
		RetroRecipes.addShapeless(new ItemStack(Item.STICK, 2), Block.WOOL);

		// (c) Modded-output recipe: output is TEST_ITEM (numeric id >= 256), crafted from cobblestone.
		//     Verifies modded crafted outputs round-trip through the inventory sidecar.
		RetroRecipes.addShaped(new ItemStack(TEST_ITEM),
			"CC", "CC", 'C', Block.COBBLESTONE);
	}
}
