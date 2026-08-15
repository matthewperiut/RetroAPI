package com.periut.retroapi.itemgroup;

import com.periut.retroapi.text.Text;
import net.minecraft.item.ItemStack;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * One tab of the creative inventory.
 *
 * <p>Built the way modern Fabric builds an item group, so the shape of the code carries over:
 *
 * <pre>{@code
 * public static final RetroItemGroup MY_GROUP = RetroItemGroup.builder(id("mymod", "gems"))
 *     .icon(() -> new ItemStack(MY_GEM))
 *     .displayName(Text.literal("Gems"))
 *     .entries(entries -> {
 *         entries.add(MY_GEM);
 *         entries.add(MY_GEM_BLOCK);
 *     })
 *     .build();
 * }</pre>
 *
 * <p>{@code entries} runs every time the screen is opened rather than once at registration, so a
 * group can show something that depends on the world or on what other mods added later.
 */
public final class RetroItemGroup {

    private final NamespacedIdentifier id;
    private final Text displayName;
    private final Supplier<ItemStack> icon;
    private final EntryCollector entries;

    /** Extra entries other mods asked to add to this group. */
    private final List<EntryCollector> additions = new ArrayList<>();

    private RetroItemGroup(final NamespacedIdentifier id, final Text displayName,
                           final Supplier<ItemStack> icon, final EntryCollector entries) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.entries = entries;
    }

    /** What a group fills itself with. */
    @FunctionalInterface
    public interface EntryCollector {
        void collect(ItemGroupEntries entries);
    }

    public static Builder builder(final NamespacedIdentifier id) {
        return new Builder(id);
    }

    public NamespacedIdentifier getId() {
        return id;
    }

    public Text getDisplayName() {
        return displayName;
    }

    public ItemStack getIcon() {
        final ItemStack stack = icon == null ? null : icon.get();
        return stack == null ? new ItemStack(net.minecraft.block.Block.STONE) : stack;
    }

    /** Another mod's additions to this group; see {@link RetroItemGroups#modifyEntries}. */
    void addModifier(final EntryCollector collector) {
        additions.add(collector);
    }

    /** Everything in the tab right now, in order: the group's own entries, then any additions. */
    public List<ItemStack> collect() {
        final ItemGroupEntries collector = new ItemGroupEntries();
        if (entries != null) {
            entries.collect(collector);
        }
        for (final EntryCollector addition : additions) {
            addition.collect(collector);
        }
        return collector.getStacks();
    }

    public static final class Builder {
        private final NamespacedIdentifier id;
        private Text displayName;
        private Supplier<ItemStack> icon;
        private EntryCollector entries;

        private Builder(final NamespacedIdentifier id) {
            this.id = id;
        }

        public Builder displayName(final Text displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder displayName(final String displayName) {
            return displayName(Text.literal(displayName));
        }

        public Builder icon(final Supplier<ItemStack> icon) {
            this.icon = icon;
            return this;
        }

        public Builder entries(final EntryCollector entries) {
            this.entries = entries;
            return this;
        }

        /** Builds the group and registers it, because an unregistered tab has nowhere to appear. */
        public RetroItemGroup build() {
            final Text name = displayName == null ? Text.literal(id.identifier()) : displayName;
            return RetroItemGroups.register(new RetroItemGroup(id, name, icon, entries));
        }
    }
}
