package com.periut.retroapi.mixin.register;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import com.periut.retroapi.register.block.RetroTexture;
import com.periut.retroapi.register.block.RetroTextures;
import com.periut.retroapi.register.item.RetroItemAccess;
import com.periut.retroapi.compat.StationAPICompat;
import com.periut.retroapi.registry.ItemRegistration;
import com.periut.retroapi.registry.RetroRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Item.class)
public abstract class ItemMixin implements RetroItemAccess {

	@Shadow public int maxStackSize;
	@Shadow public abstract Item setSprite(int sprite);

	@Override
	public RetroItemAccess maxStackSize(int size) {
		this.maxStackSize = size;
		return this;
	}

	@Override
	public RetroItemAccess texture(NamespacedIdentifier textureId) {
		Item self = (Item) (Object) this;
		RetroTexture tex = RetroTextures.addItemTexture(textureId);
		self.setSprite(tex.id);
		RetroTextures.trackItem(self, tex);
		return this;
	}

	@Override
	public Item register(NamespacedIdentifier id) {
		Item self = (Item) (Object) this;
		self.setKey(id.namespace() + "." + id.identifier());
		RetroRegistry.registerItem(new ItemRegistration(id, self));

		if (FabricLoader.getInstance().isModLoaded("stationapi")) {
			StationAPICompat.registerItem(id.namespace(), id.identifier(), self);
		}

		return self;
	}
}
