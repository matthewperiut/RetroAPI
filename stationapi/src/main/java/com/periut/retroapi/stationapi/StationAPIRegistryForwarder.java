package com.periut.retroapi.stationapi;

import com.periut.retroapi.entity.client.RetroEntityRenderers;
import com.periut.retroapi.register.block.RetroTextures;
import com.periut.retroapi.register.recipe.event.RecipeRegistrationCallback;
import com.periut.retroapi.storage.RetroBlockData;
import com.periut.retroapi.tag.RetroMining;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.block.Block;
import com.periut.retroapi.dimension.DimensionRegistration;
import com.periut.retroapi.dimension.RetroDimensionRegistry;
import net.modificationstation.stationapi.api.client.event.render.entity.EntityRendererRegisterEvent;
import net.modificationstation.stationapi.api.client.event.texture.TextureRegisterEvent;
import net.modificationstation.stationapi.api.event.registry.DimensionRegistryEvent;
import net.modificationstation.stationapi.api.event.world.BlockSetEvent;
import net.modificationstation.stationapi.api.registry.DimensionContainer;
import net.modificationstation.stationapi.api.util.Identifier;
import net.modificationstation.stationapi.api.event.entity.player.IsPlayerUsingEffectiveToolEvent;
import net.modificationstation.stationapi.api.event.entity.player.PlayerStrengthOnBlockEvent;
import net.modificationstation.stationapi.api.event.achievement.AchievementRegisterEvent;
import net.modificationstation.stationapi.api.event.recipe.RecipeRegisterEvent;
import net.modificationstation.stationapi.api.mod.entrypoint.Entrypoint;
import net.modificationstation.stationapi.api.mod.entrypoint.EventBusPolicy;
import net.modificationstation.stationapi.api.util.Namespace;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * StationAPI event bus entrypoint - handles texture registration and block tool/harvest overrides.
 * Only loaded when StationAPI is present.
 */
@Entrypoint(eventBus = @EventBusPolicy(registerInstance = true))
public class StationAPIRegistryForwarder {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/StationAPI");

	@Entrypoint.Namespace
	public Namespace namespace = Namespace.of("retroapi");

	@Entrypoint.Logger
	public Logger logger = LOGGER;

	/** Set once RetroAPI's achievement event has been passed on; the StationAPI one can fire again. */
	private static boolean achievementsForwarded;

	/**
	 * Fires RetroAPI's own {@code AchievementRegistrationCallback} from StationAPI's achievement event.
	 *
	 * <p>Without StationAPI, {@code RetroAPI.init} runs that callback itself. With it, RetroAPI hands
	 * registration over wholesale and deliberately does not fire its block, item or achievement events -
	 * so every mod registering achievements through RetroAPI's API simply registered none, and anything
	 * built on them (the WAYS pages and the button that opens them) was missing on exactly the installs
	 * that had StationAPI. This is the missing half of that handover: StationAPI decides WHEN, RetroAPI's
	 * own API still decides what.
	 *
	 * <p>Latched, because StationAPI's event is dispatched from a resource-reload path that can run more
	 * than once, and registering the same achievement twice would allocate it a second id.
	 */
	@EventListener
	public void registerAchievements(AchievementRegisterEvent event) {
		if (achievementsForwarded) return;
		achievementsForwarded = true;
		com.periut.retroapi.achievement.event.AchievementRegistrationCallback.fire();
	}

	@EventListener
	public void registerTextures(TextureRegisterEvent event) {
		LOGGER.info("Resolving RetroAPI textures with StationAPI atlas system");
		RetroTextures.resolveStationAPITextures();
	}

	/**
	 * Forward every RetroAPI-registered dimension into StationAPI's {@code DimensionRegistry} so StationAPI
	 * creates its world and resolves it by identifier - required for RetroAPI portals to teleport through
	 * StationAPI (see {@link com.periut.retroapi.compat.StationPortalBridge}) and for the dimension to exist
	 * at all under StationAPI (RetroAPI's own side-mapped world creation is disabled there). Mirrors
	 * StationAPI's own {@code VanillaDimensionFixImpl}; RetroAPI's serial id is reused so the {@code DIM<n>}
	 * folder stays consistent. Vanilla dimension ids (-1/0/1) are skipped - StationAPI owns those.
	 */
	@EventListener
	public void registerDimensions(DimensionRegistryEvent event) {
		for (DimensionRegistration reg : RetroDimensionRegistry.getAll()) {
			if (RetroDimensionRegistry.isVanillaId(reg.getSerialId())) continue;
			Identifier id = Identifier.of(reg.getId().toString());
			if (event.registry.containsId(id)) continue;
			event.registry.register(id, reg.getSerialId(), new DimensionContainer<>(reg.getFactory()));
			LOGGER.debug("Forwarded RetroAPI dimension {} (serial {}) into StationAPI DimensionRegistry", id, reg.getSerialId());
		}
	}

	/**
	 * Bridges StationAPI's recipe-register event to RetroAPI's RecipeRegistrationCallback so
	 * a single mod listener works in both worlds. RetroAPI's own recipe mixins are disabled
	 * when StationAPI is present, so the callback would otherwise never fire. We fire it once
	 * (on CRAFTING_SHAPED) - RetroRecipes.addShaped/addShapeless then delegate to StationAPI's
	 * CraftingRegistry. The vanilla type lookup is null for non-vanilla recipe ids; guard it.
	 */
	@EventListener
	public void onRecipeRegister(RecipeRegisterEvent event) {
		if (RecipeRegisterEvent.Vanilla.CRAFTING_SHAPED == RecipeRegisterEvent.Vanilla.fromType(event.recipeId)) {
			LOGGER.info("Bridging StationAPI RecipeRegisterEvent -> RetroAPI RecipeRegistrationCallback");
			RecipeRegistrationCallback.EVENT.invoker().run();
		}
	}

	/**
	 * The two mining questions, answered by RetroAPI core's {@link RetroMining} - the same class beta's own
	 * {@code PlayerEntity} hooks call, because these must agree.
	 *
	 * <p>Not gated on the block being RetroAPI-registered. The rules are about TAGS, and a tag holds
	 * whatever a mod put in it: a modded pickaxe on vanilla stone is the same question as a vanilla
	 * pickaxe on a modded ore. Gating on registration is what left the whole tag system, and with it every
	 * {@code .mineable(...)} declaration, doing nothing at all under StationAPI.
	 *
	 * <p>The result providers wrap rather than replace, so the answer already assembled - vanilla's, plus
	 * anything another mod contributed ahead of this - is what RetroAPI's rules are applied on top of.
	 */
	@EventListener
	public void onIsPlayerUsingEffectiveTool(IsPlayerUsingEffectiveToolEvent event) {
		Boolean harvests = RetroMining.canHarvest(event.player, event.blockState.getBlock());
		if (harvests == null) return;
		boolean result = harvests;
		event.resultProvider = () -> result;
	}

	/**
	 * Drops a position's {@link com.periut.retroapi.storage.RetroBlockData} when the block there changes.
	 *
	 * <p>RetroAPI does this from {@code Chunk.setBlock}, which StationAPI's {@code FlattenedChunk} does not
	 * go through - it writes states into its own sections and announces it here instead. Without this the
	 * data outlives the block it described: break a clad block, put a plain one back, and it stands there
	 * wearing the dead block's cladding.
	 *
	 * <p>The event is posted before the write, which is what the check needs: the id it compares against is
	 * the one still standing.
	 */
	@EventListener
	public void onBlockSet(BlockSetEvent event) {
		if (event.chunk == null || event.blockState == null) {
			return;
		}
		RetroBlockData.onBlockChanged(event.chunk, event.x & 15, event.y, event.z & 15,
			event.blockState.getBlock().id);
	}

	@EventListener
	public void onPlayerStrengthOnBlock(PlayerStrengthOnBlockEvent event) {
		Block block = event.blockState.getBlock();
		PlayerStrengthOnBlockEvent.ResultProvider inner = event.resultProvider;
		event.resultProvider = () -> RetroMining.breakingSpeed(event.player, block, inner.getAsFloat());
	}

	/**
	 * Bridges RetroAPI's modded entity renderers into StationAPI's renderer registration. RetroAPI's own
	 * EntityRenderDispatcher @Redirect is disabled under StationAPI (two configs must not both rewrite the
	 * dispatcher ctor), so renderers registered flat via RetroEntityRenderers are forwarded into the
	 * dispatcher map through StationAPI's EntityRendererRegisterEvent - exactly one renderer redirect stays
	 * live (StationAPI's). Client-only event; never fires on a dedicated server.
	 */
	@EventListener
	public void registerEntityRenderers(EntityRendererRegisterEvent event) {
		RetroEntityRenderers.forEach(event::register);
	}

}
