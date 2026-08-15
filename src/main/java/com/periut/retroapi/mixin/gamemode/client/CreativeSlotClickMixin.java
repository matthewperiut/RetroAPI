package com.periut.retroapi.mixin.gamemode.client;

import com.periut.retroapi.gamemode.screen.CreativeScreenHandler;
import net.minecraft.client.MultiplayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The creative screen's clicks never become window-click packets.
 *
 * <p>b1.7.3 has no creative mode and so no creative container: the server only ever knows about the
 * player's own {@code PlayerScreenHandler}, forty-five slots wide. RetroAPI's creative menu is a real
 * container on the client with fifty-five, and vanilla's multiplayer manager posts every slot click
 * to the server with the id it was given - so clicking the hotbar row put slot 45 through the server's
 * forty-five-slot handler and threw {@code IndexOutOfBoundsException} out of {@code quickMove},
 * dropping the packet and desyncing the inventory.
 *
 * <p>Modern has the same problem and the same answer: {@code CreativeModeInventoryScreen} never sends
 * a container click, it sends {@code ServerboundSetCreativeModeSlotPacket} for whichever slots it
 * changed. RetroAPI already does that through {@code CreativeSync}, so the click only has to stay
 * local - which is what cancelling the send here does.
 */
@Mixin(MultiplayerInteractionManager.class)
public class CreativeSlotClickMixin {

	@Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
	private void retroapi$keepCreativeClicksLocal(int syncId, int slotId, int button, boolean shift,
			PlayerEntity player, CallbackInfoReturnable<ItemStack> cir) {
		if (player != null && player.currentScreenHandler instanceof CreativeScreenHandler handler) {
			// Apply it to the container this client actually has open, and tell nobody: the handler
			// itself reports the slots it changed over RetroAPI's own creative channel.
			cir.setReturnValue(handler.onSlotClick(slotId, button, shift, player));
		}
	}
}
