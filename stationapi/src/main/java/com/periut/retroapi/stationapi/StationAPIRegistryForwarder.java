package com.periut.retroapi.stationapi;

import com.periut.retroapi.entity.client.RetroEntityRenderers;
import com.periut.retroapi.register.block.RetroBlockAccess;
import com.periut.retroapi.register.block.RetroTextures;
import com.periut.retroapi.register.recipe.event.RecipeRegistrationCallback;
import com.periut.retroapi.registry.RetroRegistry;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import com.periut.retroapi.dimension.DimensionRegistration;
import com.periut.retroapi.dimension.RetroDimensionRegistry;
import net.modificationstation.stationapi.api.client.event.render.entity.EntityRendererRegisterEvent;
import net.modificationstation.stationapi.api.client.event.texture.TextureRegisterEvent;
import net.modificationstation.stationapi.api.event.registry.DimensionRegistryEvent;
import net.modificationstation.stationapi.api.registry.DimensionContainer;
import net.modificationstation.stationapi.api.util.Identifier;
import net.modificationstation.stationapi.api.event.entity.player.IsPlayerUsingEffectiveToolEvent;
import net.modificationstation.stationapi.api.event.entity.player.PlayerStrengthOnBlockEvent;
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
			LOGGER.info("Forwarded RetroAPI dimension {} (serial {}) into StationAPI DimensionRegistry", id, reg.getSerialId());
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

	@EventListener
	public void onIsPlayerUsingEffectiveTool(IsPlayerUsingEffectiveToolEvent event) {
		Block block = event.blockState.getBlock();
		if (RetroRegistry.getBlockRegistration(block) == null) return;

		RetroBlockAccess access = (RetroBlockAccess) block;
		if (access.isAlwaysDrops()) {
			event.resultProvider = () -> true;
		} else {
			// Fall back to vanilla canMineBlock for RetroAPI blocks
			PlayerInventory inventory = event.player.inventory;
			event.resultProvider = () -> inventory.isUsingEffectiveTool(block);
		}
	}

	@EventListener
	public void onPlayerStrengthOnBlock(PlayerStrengthOnBlockEvent event) {
		Block block = event.blockState.getBlock();
		if (RetroRegistry.getBlockRegistration(block) == null) return;

		RetroBlockAccess access = (RetroBlockAccess) block;
		boolean alwaysEffective = access.isAlwaysEffectiveTool();
		Class<? extends Item> effectiveTool = access.getEffectiveTool();

		// Fall back to vanilla getMiningSpeed for RetroAPI blocks, then apply tool overrides
		PlayerInventory inventory = event.player.inventory;
		event.resultProvider = () -> {
			float speed = inventory.getStrengthOnBlock(block);

			if (speed > 1.0f) return speed;

			ItemStack held = inventory.getSelectedItem();
			if (held == null) return speed;
			Item item = held.getItem();

			if (alwaysEffective || (effectiveTool != null && effectiveTool.isInstance(item))) {
				return getToolSpeed(item, inventory);
			}
			return speed;
		};
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

	private static float getToolSpeed(Item item, PlayerInventory inventory) {
		if (item instanceof PickaxeItem) return inventory.getStrengthOnBlock(Block.STONE);
		if (item instanceof AxeItem) return inventory.getStrengthOnBlock(Block.PLANKS);
		if (item instanceof ShovelItem) return inventory.getStrengthOnBlock(Block.DIRT);
		if (item instanceof SwordItem) return 1.5f;
		return 1.0f;
	}
}
