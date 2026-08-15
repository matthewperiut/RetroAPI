package com.periut.retroapi.commands.argument;

import com.periut.retroapi.text.Translations;
import net.minecraft.item.Item;

import java.util.Locale;

/**
 * Human-readable item names, of the sort modern Minecraft shows in {@code Gave 1 [Apple] to Steve}.
 *
 * <p>A name is always produced, on either side, whether or not a translation table exists:
 *
 * <ol>
 *   <li>a subtype the mod names itself - beta's lang file has one entry for all sixteen wools, so
 *       {@code minecraft:wool:14} becomes "Red Wool" here and could not anywhere else;</li>
 *   <li>the game's translation for the item's key, which is the localised name when a client is
 *       asking;</li>
 *   <li>the identifier the item is registered under, spelled out - {@code iron_ingot} becomes
 *       "Iron Ingot", and a modded {@code somemod:copper_ingot} becomes "Copper Ingot". This step
 *       needs no lang file, so a dedicated server never comes up empty.</li>
 * </ol>
 */
public final class ItemNames {
    private ItemNames() {
    }

    public static String displayName(final int itemId, final int meta) {
        final String subtype = meta == 0 ? null : VanillaIds.aliasFor(itemId, meta);
        if (subtype != null) {
            return spellOut(subtype);
        }

        final String translated = Translations.find(translationKey(itemId));
        if (translated != null) {
            return translated;
        }

        // Whichever registry owns this id can still name it, which is how a modded item gets a
        // readable name on a server where no translation table exists at all.
        final String identifier = ItemIds.identifierOf(itemId);
        if (identifier == null) {
            return "Item " + itemId;
        }

        final int separator = identifier.indexOf(':');
        return spellOut(separator < 0 ? identifier : identifier.substring(separator + 1));
    }

    /**
     * Beta's key for an item, safe to ask for on either side - unlike {@code getTranslatedName},
     * which resolves it through the client-only translation class.
     */
    private static String translationKey(final int itemId) {
        // Every access here is inside the guard, including the array bounds check: reaching
        // Item.ITEMS is what triggers the item and block class initialisers, and those failing
        // raises an Error rather than an exception. A name is never worth taking a command down
        // for, so anything thrown means "no key" and the derived name takes over.
        try {
            if (itemId < 0 || itemId >= Item.ITEMS.length || Item.ITEMS[itemId] == null) {
                return null;
            }
            return Item.ITEMS[itemId].getTranslationKey() + ".name";
        } catch (final RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    /** {@code iron_ingot} to {@code Iron Ingot}. */
    public static String spellOut(final String identifier) {
        final StringBuilder result = new StringBuilder(identifier.length());
        boolean startOfWord = true;

        for (int i = 0; i < identifier.length(); i++) {
            final char c = identifier.charAt(i);
            if (c == '_') {
                result.append(' ');
                startOfWord = true;
                continue;
            }
            result.append(startOfWord ? Character.toUpperCase(c) : Character.toLowerCase(c));
            startOfWord = false;
        }

        return result.toString();
    }

    /** Lower-cased, for prose such as "cannot give red wool". */
    public static String lowerCase(final int itemId, final int meta) {
        return displayName(itemId, meta).toLowerCase(Locale.ROOT);
    }
}
