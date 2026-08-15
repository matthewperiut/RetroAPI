package com.periut.retroapi.gamemode;

import com.periut.retroapi.network.RetroAPINetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.ornithemc.osl.networking.api.client.ClientPlayNetworking;

import java.util.HashMap;
import java.util.Map;

/**
 * The client half of game-mode networking: what it is told, and the two things it may ask for.
 *
 * <p>Client-only, and only referenced from {@code RetroAPIClient} and client mixins, so a dedicated
 * server never loads it.
 *
 * <p>Both requests are <em>requests</em>. Flight and creative items are worth cheating for, so the
 * server re-checks the player's mode before acting on either; a client that lies is simply ignored.
 */
public final class GameModeNetworking {
    private GameModeNetworking() {
    }

    /** Hand me this stack. */
    public static final int GIVE = 0;
    /** This inventory slot now holds this stack. */
    public static final int SET_SLOT = 1;

    public static void registerClient() {
        ClientPlayNetworking.registerListener(RetroAPINetworking.GAMEMODE_CHANNEL, (context, buffer) -> {
            final int count = buffer.readVarInt();
            final Map<String, RetroGameMode> modes = new HashMap<>();
            for (int i = 0; i < count; i++) {
                modes.put(buffer.readString(), RetroGameMode.byId(buffer.readVarInt()));
            }

            context.ensureOnMainThread();
            if (count == 1) {
                modes.forEach(RetroGameModes::applyFromServer);
            } else {
                RetroGameModes.applyFromServer(modes);
            }
        });

        // The server's answer to a flight toggle, and the state it has been keeping for this player
        // since they last left. Without it a client on a dedicated server flies only until it stops
        // asking - the server knew, and nothing ever told the physics that actually holds the player up.
        ClientPlayNetworking.registerListener(RetroAPINetworking.FLIGHT_CHANNEL, (context, buffer) -> {
            final boolean mayFly = buffer.readBoolean();
            final boolean flying = buffer.readBoolean();
            context.ensureOnMainThread();
            final PlayerEntity player = localPlayer();
            if (player != null) {
                // Both halves together: may-fly first, because setting "flying" is refused for a player
                // the client still believes has no wings.
                RetroGameModes.applyFlightFromServer(player.name, mayFly, flying);
            }
        });
    }

    /**
     * Double-tapped jump in creative. In singleplayer there is nobody to ask, so just do it.
     *
     * <p>On a server it is asked AND done: the client already knows its own mode, and flight is what
     * its own movement code does next frame, so waiting a round trip to leave the ground is latency
     * the player feels for nothing. The server's answer arrives on the same channel a moment later
     * and overwrites this - so a client that was not entitled to fly is simply put back down.
     */
    public static void requestFlightToggle() {
        if (ClientPlayNetworking.isPlayReady(RetroAPINetworking.FLIGHT_CHANNEL)) {
            ClientPlayNetworking.send(RetroAPINetworking.FLIGHT_CHANNEL, buffer -> buffer.writeBoolean(true));
        }
        RetroGameModes.toggleFlying(localPlayerName());
    }

    /** Clicked an item in the creative screen. */
    public static void requestCreativeItem(final ItemStack stack) {
        if (stack == null) {
            return;
        }
        if (ClientPlayNetworking.isPlayReady(RetroAPINetworking.CREATIVE_GIVE_CHANNEL)) {
            ClientPlayNetworking.send(RetroAPINetworking.CREATIVE_GIVE_CHANNEL, buffer -> {
                buffer.writeVarInt(GIVE);
                buffer.writeVarInt(stack.itemId);
                buffer.writeVarInt(stack.getDamage());
                buffer.writeVarInt(stack.count);
            });
            return;
        }
        // Singleplayer: this game owns the world, so give it directly.
        CreativeGive.give(localPlayer(), stack);
    }

    /**
     * Modern's {@code ServerboundSetCreativeModeSlotPacket}: this is what the slot now holds, please
     * agree. The server checks the sender is in creative before it does.
     */
    public static void setCreativeSlot(final int slot, final ItemStack stack) {
        if (!ClientPlayNetworking.isPlayReady(RetroAPINetworking.CREATIVE_GIVE_CHANNEL)) {
            return;   // Singleplayer: the click already changed the only inventory there is.
        }
        ClientPlayNetworking.send(RetroAPINetworking.CREATIVE_GIVE_CHANNEL, buffer -> {
            buffer.writeVarInt(SET_SLOT);
            buffer.writeVarInt(slot);
            buffer.writeVarInt(stack == null ? 0 : stack.itemId);
            buffer.writeVarInt(stack == null ? 0 : stack.getDamage());
            buffer.writeVarInt(stack == null ? 0 : stack.count);
        });
    }

    private static net.minecraft.entity.player.PlayerEntity localPlayer() {
        final Object game = net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance();
        return game instanceof net.minecraft.client.Minecraft minecraft ? minecraft.player : null;
    }

    private static String localPlayerName() {
        final net.minecraft.entity.player.PlayerEntity player = localPlayer();
        return player == null ? "" : player.name;
    }
}
