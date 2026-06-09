package com.periut.retroapi.mixin.component;

import com.periut.retroapi.component.ComponentWire;
import com.periut.retroapi.component.SpawnComponentCarrier;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.ItemEntitySpawnS2CPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * Syncs the components of a DROPPED item to clients. The item-entity spawn packet carries
 * only itemRawId/count/damage (no ItemStack object), so on its own a dropped Mood Gem (or
 * any component-driven texture) would render with default data on a dedicated server. This
 * captures the source stack when the packet is built from an ItemEntity, appends the
 * component blob after the packet's last field (the velocity bytes), and stashes the blob
 * on read so {@code ClientNetworkHandler.onItemEntitySpawn} can apply it once it has rebuilt
 * the stack.
 */
@Mixin(ItemEntitySpawnS2CPacket.class)
public class ItemEntitySpawnComponentMixin implements SpawnComponentCarrier {

	@Unique
	private ItemStack retroapi$sourceStack;

	@Unique
	private NbtCompound retroapi$components;

	@Inject(method = "<init>(Lnet/minecraft/entity/ItemEntity;)V", at = @At("TAIL"))
	private void retroapi$captureStack(ItemEntity entity, CallbackInfo ci) {
		this.retroapi$sourceStack = entity.stack;
	}

	@Inject(method = "write", at = @At("TAIL"))
	private void retroapi$writeComponents(DataOutputStream out, CallbackInfo ci) {
		ComponentWire.write(out, this.retroapi$sourceStack);
	}

	@Inject(method = "read", at = @At("TAIL"))
	private void retroapi$readComponents(DataInputStream in, CallbackInfo ci) {
		this.retroapi$components = ComponentWire.readBlob(in);
	}

	@Override
	public NbtCompound retroapi$spawnComponents() {
		return this.retroapi$components;
	}
}
