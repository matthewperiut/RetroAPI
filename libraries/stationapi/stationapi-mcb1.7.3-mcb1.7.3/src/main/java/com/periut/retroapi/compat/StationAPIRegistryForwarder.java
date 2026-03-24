package com.periut.retroapi.compat;

import com.periut.retroapi.register.block.RetroBlockAccess;
import com.periut.retroapi.register.block.RetroTextures;
import com.periut.retroapi.registry.RetroRegistry;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.block.Block;
import net.minecraft.entity.mob.player.PlayerInventory;
import net.minecraft.item.*;
import net.modificationstation.stationapi.api.client.event.texture.TextureRegisterEvent;
import net.modificationstation.stationapi.api.event.entity.player.IsPlayerUsingEffectiveToolEvent;
import net.modificationstation.stationapi.api.event.entity.player.PlayerStrengthOnBlockEvent;
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
			event.resultProvider = () -> inventory.canMineBlock(block);
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
			float speed = inventory.getMiningSpeed(block);

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

	private static float getToolSpeed(Item item, PlayerInventory inventory) {
		if (item instanceof PickaxeItem) return inventory.getMiningSpeed(Block.STONE);
		if (item instanceof AxeItem) return inventory.getMiningSpeed(Block.PLANKS);
		if (item instanceof ShovelItem) return inventory.getMiningSpeed(Block.DIRT);
		if (item instanceof SwordItem) return 1.5f;
		return 1.0f;
	}
}
