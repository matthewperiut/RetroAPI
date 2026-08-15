package com.periut.retroapi.commands;

import com.periut.retroapi.commands.argument.ItemIds;
import com.periut.retroapi.commands.argument.ItemNames;
import com.periut.retroapi.commands.argument.ItemStackArgument;
import com.periut.retroapi.text.Formatting;
import com.periut.retroapi.text.HoverEvent;
import com.periut.retroapi.text.Text;
import com.periut.retroapi.commands.util.ServerUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.List;

/** Small operations several commands need, kept in one place rather than copied between them. */
public final class CommandUtil {
    private CommandUtil() {
    }

    /**
     * Hands a player a stack the way walking over one does: topped into a stack of the same item
     * first, then into a free slot - which is beta's own {@code PlayerInventory.addStack}, the very
     * method item pickup calls.
     *
     * <p>A stack carrying per-stack data goes straight to a free slot instead. Beta's merge rebuilds
     * the destination as {@code new ItemStack(id, count, damage)} and matches candidates on those
     * three alone, so a spawner set to one mob would happily pour into a stack of spawners set to
     * another and lose which was which.
     *
     * @return how many items actually went in, which is fewer than the stack held when the inventory
     *         filled up on the way - and zero, with a message already sent, when none of it fit
     */
    public static int give(final RetroCommandSource source, final PlayerEntity player, final ItemStack stack) {
        final int wanted = stack.count;

        if (carriesStackData(stack)) {
            final ItemStack[] inventory = player.inventory.main;
            for (int slot = 0; slot < inventory.length; slot++) {
                if (inventory[slot] == null) {
                    inventory[slot] = stack;
                    stack.count = 0;
                    player.inventory.dirty = true;
                    return wanted;
                }
            }
        } else {
            player.inventory.addStack(stack);
        }

        final int given = wanted - stack.count;
        if (given > 0) {
            player.inventory.dirty = true;
        }
        if (stack.count > 0) {
            source.sendError(Text.literal("Cannot give " + ItemNames.displayName(stack.itemId, stack.getDamage())
                + " because " + player.name + "'s inventory is full"));
        }
        return given;
    }

    /** Whether this stack holds something beyond id/count/damage, which beta's merge cannot carry. */
    private static boolean carriesStackData(final ItemStack stack) {
        final String spawnerEntity = ((com.periut.retroapi.commands.api.ItemInstanceStr) (Object) stack).spc$getStr();
        return spawnerEntity != null && !spawnerEntity.isEmpty();
    }

    /**
     * Moves an entity.
     *
     * <p>A server has to go through the network handler so the client is told to move too;
     * singleplayer sets the position directly, and must clear velocity or the player keeps falling
     * at whatever speed they arrived with.
     */
    public static void teleport(final Entity entity, final Position position) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER && entity instanceof PlayerEntity player) {
            ServerUtil.serverTeleport(player, position.x(), position.y(), position.z());
            return;
        }

        entity.setPosition(position.x(), position.y(), position.z());
        entity.velocityX = 0.0;
        entity.velocityY = 0.0;
        entity.velocityZ = 0.0;
    }

    /**
     * An item as modern writes it in command output: its display name in square brackets, left the
     * default colour, with the identifier and numeric id on hover for anyone who needs them.
     */
    public static Text describeItem(final ItemStackArgument item) {
        final String identifier = ItemIds.nameOf(item.itemId())
            + (item.meta() == 0 ? "" : ":" + item.meta());

        // One line, with a space: the chat tooltip draws a single line and swallows a newline whole,
        // which is how "minecraft:command_block" and "id 573" ended up printed as one word.
        return Text.literal("[" + ItemNames.displayName(item.itemId(), item.meta()) + "]")
            .styled(style -> style.withHoverEvent(HoverEvent.showText(
                Text.literal(identifier).formatted(Formatting.GRAY)
                    .append(Text.literal(" (id " + item.itemId() + ")").formatted(Formatting.DARK_GRAY)))));
    }

    /** "a, b and c" - how modern phrases a list of affected targets. */
    public static String joinNames(final List<? extends Entity> entities) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < entities.size(); i++) {
            if (i > 0) {
                builder.append(i == entities.size() - 1 ? " and " : ", ");
            }
            builder.append(com.periut.retroapi.commands.selector.EntitySelectorReader.nameOf(entities.get(i)));
        }
        return builder.toString();
    }
}
