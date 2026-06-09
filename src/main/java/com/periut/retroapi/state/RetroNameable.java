package com.periut.retroapi.state;

/**
 * Optional interface for enums used with {@link RetroEnumProperty}: implement it to
 * control the serialized name (blockstate JSON variant keys, tag files). Enums that
 * don't implement it serialize as their lowercase constant name.
 */
public interface RetroNameable {
	String getName();
}
