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
import com.periut.retroapi.entrypoint.RetroModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TestMod implements RetroModInitializer {
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

	// New-feature exercises (multi-tool, dynamic tier, code layers, facing, factory-of, item tags).
	public static Item PAXEL;
	public static Item DYNAMIC_TOOL;
	public static Item CODE_LAYERED;
	public static Block FACTORY_BLOCK;
	public static Block FACING_BLOCK;
	/** 0.3.0: a stone-material block with NO mineable tag, to prove the material-inferred defaults. */
	public static Block UNTAGGED_STONE_BLOCK;
	/** 0.3.0: six-way facing + code-declared per-face textures. */
	public static Block SIX_WAY_BLOCK;
	/** 0.3.0: a tool built from parameters instead of a (non-extensible) ToolMaterial. */
	public static Item STAT_TOOL;
	/** 0.3.0: a tier that depends on the block being mined. */
	public static Item CONTEXT_TOOL;
	/** 0.3.0: an ASCII-declared multiblock pattern. */
	public static com.periut.retroapi.world.multiblock.RetroMultiblock TEST_MULTIBLOCK;
	/**
	 * 0.3.1: a registered particle and its sprite. Deliberately a terrain texture that no block uses -
	 * the case {@link com.periut.retroapi.client.particle.RetroSpriteParticle} advertises - so the client
	 * smoke test can prove the whole particle path (registry + world-renderer hook) is live.
	 */
	/** 0.3.3: a plain vanilla Item carrying a statically declared, tinted overlay layer. */
	public static Item TINT_ITEM;
	public static final NamespacedIdentifier SPARK_PARTICLE = NamespacedIdentifiers.from("retroapi_test", "spark");
	public static com.periut.retroapi.register.block.RetroTexture SPARK_TEXTURE;

	// A data component for the headless round-trip self-check.
	public static com.periut.retroapi.component.RetroComponentType<Integer> TEST_COUNT;
	// A RECORD (compound) and a LIST component, to prove richer data types round-trip.
	public static com.periut.retroapi.component.RetroComponentType<int[]> TEST_PAIR;
	public static com.periut.retroapi.component.RetroComponentType<java.util.List<Integer>> TEST_LIST;

	private static NamespacedIdentifier id(String name) {
		return NamespacedIdentifiers.from("retroapi_test", name);
	}

	@Override
	public void initRetro() {
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

		// A statically tinted item: base sprite + a colored overlay pass, declared on a stock Item.
		TINT_ITEM = RetroItemAccess.create()
			.maxStackSize(64)
			.texture(id("test_item"))
			.overlay(id("layer_overlay"), 0x3355FF)
			.register(id("tint_item"));

		// A terrain sprite owned by nothing but the spark particle (see SPARK_PARTICLE).
		SPARK_TEXTURE = com.periut.retroapi.register.block.RetroTextures.addBlockTexture(id("test_block"));

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

		// ---------------------------------------------- new-feature exercises --

		// Multi-kind tool (a paxel): mines everything in mineable/pickaxe AND mineable/axe, at iron tier.
		PAXEL = (Item) RetroItemAccess.create()
			.maxStackSize(1)
			.tool(com.periut.retroapi.tag.RetroTool.PICKAXE, com.periut.retroapi.tag.RetroTool.AXE)
			.tier(com.periut.retroapi.tag.RetroToolTier.IRON)
			.texture(id("test_item"))
			.register(id("paxel"));

		// Dynamic tier: the tier is read off the stack each harvest (undamaged = diamond, worn = iron).
		DYNAMIC_TOOL = (Item) RetroItemAccess.create()
			.maxStackSize(1)
			.tool(com.periut.retroapi.tag.RetroTool.PICKAXE)
			.tier(stack -> stack.getDamage() == 0
				? com.periut.retroapi.tag.RetroToolTier.DIAMOND
				: com.periut.retroapi.tag.RetroToolTier.IRON)
			.texture(id("test_item"))
			.register(id("dynamic_tool"));

		// Layered item with NO model JSON: base sprite + one overlay, flattened at atlas build.
		CODE_LAYERED = RetroItemAccess.create()
			.maxStackSize(64)
			.layers(id("layer_base"), id("layer_overlay"))
			.register(id("code_layered"));

		// Factory-of: wrap a Block subclass by its constructor, no id boilerplate (Kyd's of(Ctor::new)).
		FACTORY_BLOCK = RetroBlockAccess.of(TestLampBlock::new)
			.sounds(Block.STONE_SOUND_GROUP)
			.strength(0.5f)
			.alwaysDrops()
			.alwaysEffectiveTool()
			.states(TestLampBlock.LIT, TestLampBlock.AGE)
			.sprite(Block.COBBLESTONE.getTexture(0))
			.register(id("factory_block"));

		// Facing: furnace-like rotation with no onPlaced of our own. The blockstate JSON
		// (retroapi/blockstates/facing_block.json) y-rotates an orientable model per facing;
		// the front is the distinct tagged_ore texture so you can SEE it turn to face you.
		FACING_BLOCK = RetroBlockAccess.create(Material.STONE)
			.sounds(Block.STONE_SOUND_GROUP)
			.strength(1.5f)
			.facing()
			.register(id("facing_block"));

		// 0.3.0: NO .mineable(...) call at all. Its stone material must still make it a
		// pickaxe block, which is what stops "my tool can't break my block".
		UNTAGGED_STONE_BLOCK = RetroBlockAccess.create(Material.STONE)
			.sounds(Block.STONE_SOUND_GROUP)
			.strength(1.5f)
			.sprite(Block.COBBLESTONE.getTexture(0))
			.register(id("untagged_stone"));

		// 0.3.0: six-way facing (up/down too) + per-face textures declared in code, no model JSON.
		// .states(...) after .facing*() must NOT drop the facing property - that ordering bug is
		// exactly what this registration shape reproduces.
		SIX_WAY_BLOCK = RetroBlockAccess.create(Material.STONE)
			.sounds(Block.STONE_SOUND_GROUP)
			.strength(1.5f)
			.facingAll()
			.states(TestLampBlock.LIT)
			.sided(id("layer_base"), id("layer_overlay"), id("tagged_ore"))
			.tag(com.periut.retroapi.tag.RetroTagKey.block("retroapi_test/machines"))
			.register(id("six_way"));

		// 0.3.0: a tool with no ToolMaterial - every parameter given directly.
		STAT_TOOL = (Item) RetroItemAccess.create()
			.maxStackSize(1)
			.tool(com.periut.retroapi.tag.RetroTool.PICKAXE)
			.tier(com.periut.retroapi.tag.RetroToolTier.IRON)
			.miningSpeed(9.0F)
			.attackDamage(4)
			.durability(750)
			.handheld()
			.texture(id("test_item"))
			.register(id("stat_tool"));

		// 0.3.0: a tier that sees the block: diamond on stone, wood on everything else. 0.3.1 also hands
		// it the player, so it can blunt itself in the nether - the case the javadoc always advertised
		// but the two-argument form could not actually reach.
		CONTEXT_TOOL = (Item) RetroItemAccess.create()
			.maxStackSize(1)
			.tool(com.periut.retroapi.tag.RetroTool.PICKAXE)
			.tier((stack, block, player) -> {
				if (player != null && player.world != null && player.world.dimension.isNether) {
					return com.periut.retroapi.tag.RetroToolTier.WOOD;
				}
				return block != null && block.material == Material.STONE
					? com.periut.retroapi.tag.RetroToolTier.DIAMOND
					: com.periut.retroapi.tag.RetroToolTier.WOOD;
			})
			.texture(id("test_item"))
			.register(id("context_tool"));

		// 0.3.0: an ore feature, registered instead of mixin-ing the chunk generator.
		com.periut.retroapi.world.feature.RetroFeatures
			.ore(UNTAGGED_STONE_BLOCK).size(4).count(2).heightRange(8, 40).rarity(2).register();

		// 0.3.0: a multiblock pattern, declared as ASCII layers.
		TEST_MULTIBLOCK = com.periut.retroapi.world.multiblock.RetroMultiblock.builder()
			.layer("SSS",
			       "SCS",
			       "SSS")
			.where('S', Block.STONE)
			.where('C', UNTAGGED_STONE_BLOCK)
			.anchor('C')
			.build();

		// Arbitrary runtime item tags: tag two items, then query.
		com.periut.retroapi.tag.RetroTagKey gems = com.periut.retroapi.tag.RetroTagKey.item("retroapi_test/gems");
		com.periut.retroapi.tag.RetroTags.addToTag(gems, TEST_ITEM, PAXEL);

		retroapi$smokeCheck(gems);

		TEST_COUNT = com.periut.retroapi.component.RetroComponents.register(
			id("test_count"), 0, com.periut.retroapi.component.RetroComponentType.INT);
		// compound(): writes a multi-field value into its own sub-compound (a record).
		TEST_PAIR = com.periut.retroapi.component.RetroComponents.register(id("test_pair"), new int[]{0, 0},
			com.periut.retroapi.component.RetroComponentType.compound(
				(nbt, v) -> { nbt.putInt("a", v[0]); nbt.putInt("b", v[1]); },
				nbt -> new int[]{nbt.getInt("a"), nbt.getInt("b")}));
		TEST_LIST = com.periut.retroapi.component.RetroComponents.register(id("test_list"), java.util.Collections.emptyList(),
			com.periut.retroapi.component.RetroComponentType.listOf(com.periut.retroapi.component.RetroComponentType.INT));

		// A MUTABLE component default must not be shared between stacks. It used to be handed out as one
		// instance, so a mod that mutated what get() returned was writing into the value every other
		// stack reads - the data showed up on every item in the game at once, and only for list/map/set
		// components, which is exactly how it presented in the wild.
		ItemStack one = new ItemStack(Item.STICK);
		ItemStack two = new ItemStack(Item.STICK);
		com.periut.retroapi.component.RetroComponents.get(one, TEST_LIST).add(99);
		java.util.List<Integer> otherStack =
			com.periut.retroapi.component.RetroComponents.get(two, TEST_LIST);
		boolean isolated = otherStack.isEmpty()
			&& !com.periut.retroapi.component.RetroComponents.has(one, TEST_LIST);
		LOGGER.info("[new-features] mutable component default isolated (other stack sees {}) {}",
			otherStack, isolated ? "PASS" : "FAIL");

		// Register achievements AFTER blocks/items so icons can reference registered content.
		TestAchievements.register();

		LOGGER.info("Registered test_block, color_block, pipe, test_item, + " + BLOCK_COUNT + " numbered blocks, + " + ITEM_COUNT + " numbered items");
	}

	/** Logs PASS/FAIL for the new-feature exercises so headless runs verify them. */
	private static void retroapi$smokeCheck(com.periut.retroapi.tag.RetroTagKey gems) {
		java.util.Set<com.periut.retroapi.tag.RetroTool> paxelKinds = com.periut.retroapi.tag.RetroTool.kindsOf(PAXEL);
		boolean multi = paxelKinds.contains(com.periut.retroapi.tag.RetroTool.PICKAXE)
			&& paxelKinds.contains(com.periut.retroapi.tag.RetroTool.AXE);
		LOGGER.info("[new-features] multi-tool paxel kinds={} {}", paxelKinds, multi ? "PASS" : "FAIL");

		com.periut.retroapi.tag.RetroToolTier fresh =
			com.periut.retroapi.tag.RetroToolTier.of(new ItemStack(DYNAMIC_TOOL));
		boolean dyn = fresh == com.periut.retroapi.tag.RetroToolTier.DIAMOND;
		LOGGER.info("[new-features] dynamic tier (undamaged)={} {}", fresh, dyn ? "PASS" : "FAIL");

		boolean itemTagged = com.periut.retroapi.tag.RetroTags.isIn(TEST_ITEM, gems)
			&& com.periut.retroapi.tag.RetroTags.isIn(PAXEL, gems)
			&& !com.periut.retroapi.tag.RetroTags.isIn(DYNAMIC_TOOL, gems);
		LOGGER.info("[new-features] runtime item tag membership {}", itemTagged ? "PASS" : "FAIL");

		boolean facing = FACING_BLOCK != null
			&& ((com.periut.retroapi.register.block.RetroBlockAccess) FACING_BLOCK).isAutoFacing();
		LOGGER.info("[new-features] .facing() auto-orient flag {}", facing ? "PASS" : "FAIL");

		// Vanilla-block coverage only applies without StationAPI (which owns vanilla harvesting itself).
		if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("stationapi")) {
			boolean stone = com.periut.retroapi.tag.RetroTags.isIn(
				Block.STONE, com.periut.retroapi.tag.RetroTool.PICKAXE.mineableTag());
			boolean goldTier = com.periut.retroapi.tag.RetroTags.requiredTier(Block.GOLD_ORE)
				== com.periut.retroapi.tag.RetroToolTier.IRON;
			boolean ironTier = com.periut.retroapi.tag.RetroTags.requiredTier(Block.IRON_ORE)
				== com.periut.retroapi.tag.RetroToolTier.STONE;
			LOGGER.info("[new-features] vanilla defaults: stone=mineable/pickaxe={} goldOre=needs_iron={} ironOre=needs_stone={} {}",
				stone, goldTier, ironTier, (stone && goldTier && ironTier) ? "PASS" : "FAIL");
		}

		retroapi$smokeCheck030();
	}

	/** 0.3.0 features: directions, positions, char states, multiblocks, features, tool stats. */
	private static void retroapi$smokeCheck030() {
		// --- RetroDirection / RetroFacing helpers -------------------------------------------------
		com.periut.retroapi.util.RetroDirection north = com.periut.retroapi.util.RetroDirection.NORTH;
		boolean dirs = north.opposite() == com.periut.retroapi.util.RetroDirection.SOUTH
			&& north.rotateRight() == com.periut.retroapi.util.RetroDirection.EAST
			&& north.rotateLeft() == com.periut.retroapi.util.RetroDirection.WEST
			&& north.face() == 2
			&& com.periut.retroapi.util.RetroDirection.fromFace(1) == com.periut.retroapi.util.RetroDirection.UP
			&& com.periut.retroapi.util.RetroDirection.nearest(0.0, -2.0, 0.5)
				== com.periut.retroapi.util.RetroDirection.DOWN
			&& com.periut.retroapi.state.RetroFacing.EAST.rotateLeft() == com.periut.retroapi.state.RetroFacing.NORTH
			&& com.periut.retroapi.state.RetroFacing.EAST.toDirection().offsetX == 1;
		LOGGER.info("[new-features] direction helpers {}", dirs ? "PASS" : "FAIL");

		// --- RetroVec3i ---------------------------------------------------------------------------
		com.periut.retroapi.util.RetroVec3i base = com.periut.retroapi.util.RetroVec3i.of(10, 64, -5);
		com.periut.retroapi.util.RetroVec3i moved = base.offset(com.periut.retroapi.util.RetroDirection.EAST, 3).up(2);
		boolean vec = moved.equals(com.periut.retroapi.util.RetroVec3i.of(13, 66, -5))
			&& base.add(1, 1, 1).subtract(1, 1, 1).equals(base)
			&& base.multiply(2).equals(com.periut.retroapi.util.RetroVec3i.of(20, 128, -10))
			&& base.manhattanDistance(moved) == 5
			&& base.isWithin(moved, 3)
			// north (0,0,-1) rotated to face EAST becomes (+1,0,0): the multiblock rotation rule
			&& com.periut.retroapi.util.RetroVec3i.NORTH.rotateTo(com.periut.retroapi.state.RetroFacing.EAST)
				.equals(com.periut.retroapi.util.RetroVec3i.EAST);
		LOGGER.info("[new-features] RetroVec3i math {}", vec ? "PASS" : "FAIL");

		// --- RetroCharProperty --------------------------------------------------------------------
		com.periut.retroapi.state.RetroCharProperty letters =
			com.periut.retroapi.state.RetroCharProperty.letters("letter");
		boolean chars = letters.values().size() == 26
			&& letters.ordinalOf('c') == 2
			&& letters.valueName('q').equals("q")
			&& letters.parse("q") == Character.valueOf('q')
			&& letters.parse("7") == null;
		LOGGER.info("[new-features] RetroCharProperty {}", chars ? "PASS" : "FAIL");

		// --- material-inferred mineable defaults (the "modded tool can't break modded block" fix) --
		if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("stationapi")) {
			java.util.Set<com.periut.retroapi.tag.RetroTool> untaggedStone =
				com.periut.retroapi.tag.RetroTags.mineableTools(UNTAGGED_STONE_BLOCK);
			boolean defaults = untaggedStone.contains(com.periut.retroapi.tag.RetroTool.PICKAXE)
				&& com.periut.retroapi.tag.RetroTags.defaultMineableTools(Block.LEAVES)
					.contains(com.periut.retroapi.tag.RetroTool.SHEARS)
				&& UNTAGGED_STONE_BLOCK.material != null;
			LOGGER.info("[new-features] material mineable defaults (untagged stone={}) {}",
				untaggedStone, defaults ? "PASS" : "FAIL");

			// A declared plain-Item tool must satisfy VANILLA's own effectiveness question.
			boolean suitable = STAT_TOOL.isSuitableFor(UNTAGGED_STONE_BLOCK);
			LOGGER.info("[new-features] declared tool isSuitableFor(modded stone)={} {}",
				suitable, suitable ? "PASS" : "FAIL");
		}

		// --- tools built from parameters instead of a ToolMaterial --------------------------------
		com.periut.retroapi.register.item.RetroItemAccess statAccess =
			(com.periut.retroapi.register.item.RetroItemAccess) STAT_TOOL;
		boolean stats = statAccess.getMiningSpeed() == 9.0F
			&& statAccess.getAttackDamage() == 4
			&& STAT_TOOL.getMaxDamage() == 750
			&& STAT_TOOL.getMiningSpeedMultiplier(new ItemStack(STAT_TOOL), Block.STONE) == 9.0F;
		LOGGER.info("[new-features] material-free tool stats {}", stats ? "PASS" : "FAIL");

		// --- block-aware (contextual) tier ---------------------------------------------------------
		com.periut.retroapi.tag.RetroToolTier onStone =
			com.periut.retroapi.tag.RetroToolTier.of(new ItemStack(CONTEXT_TOOL), Block.STONE);
		com.periut.retroapi.tag.RetroToolTier onWood =
			com.periut.retroapi.tag.RetroToolTier.of(new ItemStack(CONTEXT_TOOL), Block.PLANKS);
		boolean contextual = onStone == com.periut.retroapi.tag.RetroToolTier.DIAMOND
			&& onWood == com.periut.retroapi.tag.RetroToolTier.WOOD;
		LOGGER.info("[new-features] contextual tier stone={} wood={} {}",
			onStone, onWood, contextual ? "PASS" : "FAIL");

		// --- world features registered (not generated here; generation is exercised in-world) ------
		boolean features = !com.periut.retroapi.world.feature.RetroFeatures.getAll().isEmpty();
		LOGGER.info("[new-features] world feature registry size={} {}",
			com.periut.retroapi.world.feature.RetroFeatures.getAll().size(), features ? "PASS" : "FAIL");

		// --- multiblock pattern shape --------------------------------------------------------------
		boolean multiblock = TEST_MULTIBLOCK != null;
		LOGGER.info("[new-features] multiblock pattern built {}", multiblock ? "PASS" : "FAIL");

		// --- 0.3.3: statically tinted item layers, on an item we do not subclass -------------------
		// The point is that TINT_ITEM is a plain vanilla Item: no RetroLayeredTexture implementation,
		// no subclass of ours. Declaring .overlay(id, tint) has to be enough, because the whole use case
		// is wrapping an item class the mod does not own.
		java.util.List<com.periut.retroapi.component.RetroTextureLayer> declared =
			((com.periut.retroapi.register.item.RetroItemAccess) TINT_ITEM).getDeclaredLayers();
		boolean tinted = declared.size() == 2
			&& declared.get(0).tint() == com.periut.retroapi.component.RetroTextureLayer.NO_TINT
			&& declared.get(1).tint() == 0x3355FF
			&& declared.get(1).texture() != null;
		LOGGER.info("[new-features] static item tint layers={} overlayTint=0x{} {}",
			declared.size(),
			declared.size() > 1 ? Integer.toHexString(declared.get(1).tint()) : "-",
			tinted ? "PASS" : "FAIL");

		// --- 0.3.3: a tier that REFUSES ------------------------------------------------------------
		// null means "no opinion, fall through", and falling through lands on WOOD, so a contextual
		// lambda had no way to say "this tool cannot harvest this block". NONE is below every tier, so
		// it satisfies nothing - not even the implicit WOOD a tool-requiring block demands.
		boolean refuses = !com.periut.retroapi.tag.RetroToolTier.NONE
			.isAtLeast(com.periut.retroapi.tag.RetroToolTier.WOOD)
			&& com.periut.retroapi.tag.RetroToolTier.fromLevel(-1) == com.periut.retroapi.tag.RetroToolTier.NONE
			&& com.periut.retroapi.tag.RetroToolTier.DIAMOND
				.isAtLeast(com.periut.retroapi.tag.RetroToolTier.NONE);
		LOGGER.info("[new-features] NONE tier refuses every requirement {}", refuses ? "PASS" : "FAIL");

		// --- 0.3.3: texture get-or-register must not burn a second atlas slot ----------------------
		com.periut.retroapi.register.block.RetroTexture firstAsk =
			com.periut.retroapi.register.block.RetroTextures.getOrAddItemTexture(id("test_item"));
		com.periut.retroapi.register.block.RetroTexture secondAsk =
			com.periut.retroapi.register.block.RetroTextures.getOrAddItemTexture(id("test_item"));
		boolean deduped = firstAsk == secondAsk;
		LOGGER.info("[new-features] getOrAddItemTexture reuses the slot (slot {}) {}",
			firstAsk.id, deduped ? "PASS" : "FAIL");

		// --- 0.3.1: tags that reference other tags -------------------------------------------------
		// Resolving a "#other:tag" entry used to re-enter the tag cache before it was populated, which
		// restarted resolution and recursed until the stack died - a hard crash for any pack whose tag
		// files reference each other. Both files exist purely so every run resolves one.
		java.util.Set<Item> allTools = com.periut.retroapi.tag.RetroTags.itemsIn(
			com.periut.retroapi.tag.RetroTagKey.item("retroapi_test:all_tools"));
		java.util.Set<Block> hardThings = com.periut.retroapi.tag.RetroTags.blocksIn(
			com.periut.retroapi.tag.RetroTagKey.block("retroapi_test:hard_things"));
		boolean refs = allTools.contains(PAXEL) && allTools.contains(STAT_TOOL)
			&& hardThings.contains(TAGGED_ORE) && hardThings.contains(Block.OBSIDIAN);
		LOGGER.info("[new-features] tag references (items={} blocks={}) {}",
			allTools.size(), hardThings.size(), refs ? "PASS" : "FAIL");
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
