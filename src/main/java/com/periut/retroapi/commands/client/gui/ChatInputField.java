package com.periut.retroapi.commands.client.gui;

import com.periut.retroapi.text.Style;
import com.periut.retroapi.text.Texts;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.CharacterUtils;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * The chat input box: a backport of modern Minecraft's {@code TextFieldWidget}.
 *
 * <p>Beta's own text field is a single string with a cursor pinned to the end - no selection, no
 * scrolling, no keyboard shortcuts. This one has all of it: a cursor and a selection anchor,
 * {@code Ctrl+A/C/X/V}, word-wise movement and deletion with {@code Ctrl}, {@code Home} and
 * {@code End}, shift-selection, a window that scrolls horizontally when the text outruns the box,
 * and the grey ghost text the command suggestor writes into it.
 */
public class ChatInputField extends DrawContext {
    private static final int CURSOR_BLINK_TICKS = 6;
    private static final int TEXT_COLOR = 0xE0E0E0;
    private static final int SELECTION_COLOR = 0xFF3030C0;

    private final TextRenderer textRenderer;
    private final int width;

    /** Modern's {@code EditBox.setTextColor}: chat keeps the default, the creative search box is white. */
    private int textColor = TEXT_COLOR;

    private String text = "";
    private int maxLength = 256;
    private int cursor;
    private int selectionAnchor;
    private int firstCharacterIndex;
    private int focusedTicks;
    private String suggestion = "";

    private Consumer<String> changedListener = ignored -> {
    };
    /** Given the visible slice and where it starts, returns how to colour it. */
    private BiFunction<String, Integer, TextDrawer.Line> renderTextProvider =
        (visible, firstIndex) -> new TextDrawer.Line(List.of(new Texts.Segment(visible, Style.EMPTY)));

    public ChatInputField(final TextRenderer textRenderer, final int width) {
        this.textRenderer = textRenderer;
        this.width = width;
    }

    public void setChangedListener(final Consumer<String> listener) {
        this.changedListener = listener;
    }

    public void setRenderTextProvider(final BiFunction<String, Integer, TextDrawer.Line> provider) {
        this.renderTextProvider = provider;
    }

    public void setMaxLength(final int maxLength) {
        this.maxLength = maxLength;
    }

    public void setTextColor(final int textColor) {
        this.textColor = textColor;
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        final String trimmed = text.length() > maxLength ? text.substring(0, maxLength) : text;
        this.text = trimmed;
        setCursor(trimmed.length());
        setSelectionAnchor(cursor);
        onChanged();
    }

    public int getCursor() {
        return cursor;
    }

    public void setCursor(final int position) {
        cursor = Math.max(0, Math.min(text.length(), position));
        clampScroll();
    }

    public void setSelectionAnchor(final int position) {
        selectionAnchor = Math.max(0, Math.min(text.length(), position));
    }

    public String getSelectedText() {
        final int start = Math.min(cursor, selectionAnchor);
        final int end = Math.max(cursor, selectionAnchor);
        return text.substring(start, end);
    }

    /** Grey text drawn after the cursor; the suggestor uses it to preview a completion. */
    public void setSuggestion(final String suggestion) {
        this.suggestion = suggestion == null ? "" : suggestion;
    }

    public void tick() {
        focusedTicks++;
    }

    public void write(final String written) {
        final StringBuilder filtered = new StringBuilder();
        for (int i = 0; i < written.length(); i++) {
            final char c = written.charAt(i);
            if (CharacterUtils.VALID_CHARACTERS.indexOf(c) >= 0) {
                filtered.append(c);
            }
        }

        final int start = Math.min(cursor, selectionAnchor);
        final int end = Math.max(cursor, selectionAnchor);
        final int room = maxLength - text.length() + (end - start);
        if (room <= 0) {
            return;
        }

        final String insertion = filtered.length() > room ? filtered.substring(0, room) : filtered.toString();
        text = text.substring(0, start) + insertion + text.substring(end);
        setCursor(start + insertion.length());
        setSelectionAnchor(cursor);
        onChanged();
    }

    /**
     * @param keyCode an LWJGL key code, as beta's screens receive them
     * @return whether the field handled it
     */
    public boolean keyPressed(final char typed, final int keyCode) {
        final boolean control = Keys.isControlDown();
        final boolean shift = Keys.isShiftDown();

        if (control) {
            switch (keyCode) {
                case Keys.A -> {
                    setCursor(text.length());
                    setSelectionAnchor(0);
                    return true;
                }
                case Keys.C -> {
                    Clipboard.write(getSelectedText());
                    return true;
                }
                case Keys.V -> {
                    write(Clipboard.read());
                    return true;
                }
                case Keys.X -> {
                    Clipboard.write(getSelectedText());
                    write("");
                    return true;
                }
                default -> {
                    // Fall through to the shared handling below.
                }
            }
        }

        switch (keyCode) {
            case Keys.BACKSPACE -> {
                if (control) {
                    eraseWords(-1);
                } else {
                    erase(-1);
                }
                return true;
            }
            case Keys.DELETE -> {
                if (control) {
                    eraseWords(1);
                } else {
                    erase(1);
                }
                return true;
            }
            case Keys.LEFT -> {
                // With text selected and no shift held, the arrow collapses the selection to its edge
                // rather than stepping one character from wherever the cursor happens to be - which is
                // what every text field does, and what makes "select, then left" put you before the
                // selection instead of somewhere inside it.
                if (!shift && !control && cursor != selectionAnchor) {
                    setCursor(Math.min(cursor, selectionAnchor));
                    setSelectionAnchor(cursor);
                    return true;
                }
                moveCursor(control ? wordSkipPosition(-1) - cursor : -1, shift);
                return true;
            }
            case Keys.RIGHT -> {
                if (!shift && !control && cursor != selectionAnchor) {
                    setCursor(Math.max(cursor, selectionAnchor));
                    setSelectionAnchor(cursor);
                    return true;
                }
                moveCursor(control ? wordSkipPosition(1) - cursor : 1, shift);
                return true;
            }
            case Keys.HOME -> {
                moveCursor(-cursor, shift);
                return true;
            }
            case Keys.END -> {
                moveCursor(text.length() - cursor, shift);
                return true;
            }
            default -> {
                if (CharacterUtils.VALID_CHARACTERS.indexOf(typed) >= 0) {
                    write(String.valueOf(typed));
                    return true;
                }
                return false;
            }
        }
    }

    private void moveCursor(final int offset, final boolean select) {
        setCursor(cursor + offset);
        if (!select) {
            setSelectionAnchor(cursor);
        }
    }

    /** Removes the selection if there is one, otherwise the character in the given direction. */
    public void erase(final int direction) {
        if (cursor != selectionAnchor) {
            write("");
            return;
        }

        final int target = Math.max(0, Math.min(text.length(), cursor + direction));
        if (target == cursor) {
            return;
        }

        final int start = Math.min(target, cursor);
        final int end = Math.max(target, cursor);
        text = text.substring(0, start) + text.substring(end);
        setCursor(start);
        setSelectionAnchor(cursor);
        onChanged();
    }

    public void eraseWords(final int direction) {
        if (cursor != selectionAnchor) {
            write("");
            return;
        }
        erase(wordSkipPosition(direction) - cursor);
    }

    /** Where the cursor lands after a {@code Ctrl}-arrow: past the run of spaces, then the word. */
    private int wordSkipPosition(final int direction) {
        int position = cursor;

        if (direction < 0) {
            while (position > 0 && text.charAt(position - 1) == ' ') {
                position--;
            }
            while (position > 0 && text.charAt(position - 1) != ' ') {
                position--;
            }
        } else {
            final int length = text.length();
            while (position < length && text.charAt(position) == ' ') {
                position++;
            }
            while (position < length && text.charAt(position) != ' ') {
                position++;
            }
        }

        return position;
    }

    /** @param x the mouse position relative to the left edge of the field's text */
    public void click(final int x, final boolean select) {
        final String visible = visibleText();
        final int index = firstCharacterIndex + charIndexAt(visible, x);
        setCursor(index);
        if (!select) {
            setSelectionAnchor(cursor);
        }
    }

    private int charIndexAt(final String visible, final int x) {
        int accumulated = 0;
        for (int i = 0; i < visible.length(); i++) {
            final int charWidth = textRenderer.getWidth(String.valueOf(visible.charAt(i)));
            // Past the halfway point of a character means the cursor belongs after it.
            if (x < accumulated + charWidth / 2) {
                return i;
            }
            accumulated += charWidth;
        }
        return visible.length();
    }

    public void render(final int x, final int y) {
        final String visible = visibleText();
        final int cursorOffset = textRenderer.getWidth(text.substring(firstCharacterIndex, Math.max(firstCharacterIndex, cursor)));

        if (cursor != selectionAnchor) {
            renderSelection(x, y, visible);
        }

        TextDrawer.INSTANCE.draw(textRenderer, renderTextProvider.apply(visible, firstCharacterIndex), x, y, textColor, 255);

        if (!suggestion.isEmpty()) {
            drawTextWithShadow(textRenderer, suggestion, x + textRenderer.getWidth(visible), y, 0x808080);
        }

        if (focusedTicks / CURSOR_BLINK_TICKS % 2 == 0) {
            final int cursorX = x + cursorOffset;
            if (cursor < text.length()) {
                fill(cursorX, y - 1, cursorX + 1, y + 9, 0xFFD0D0D0);
            } else {
                drawTextWithShadow(textRenderer, "_", cursorX, y, textColor);
            }
        }
    }

    private void renderSelection(final int x, final int y, final String visible) {
        final int start = Math.max(Math.min(cursor, selectionAnchor), firstCharacterIndex);
        final int end = Math.min(Math.max(cursor, selectionAnchor), firstCharacterIndex + visible.length());
        if (start >= end) {
            return;
        }

        final int startX = x + textRenderer.getWidth(text.substring(firstCharacterIndex, start));
        final int endX = x + textRenderer.getWidth(text.substring(firstCharacterIndex, end));
        fill(startX, y - 1, endX, y + 9, SELECTION_COLOR);
    }

    /** The slice of the text that fits in the box, given where the window has scrolled to. */
    public String visibleText() {
        final int available = width;
        int accumulated = 0;
        int index = firstCharacterIndex;

        while (index < text.length()) {
            final int charWidth = textRenderer.getWidth(String.valueOf(text.charAt(index)));
            if (accumulated + charWidth > available) {
                break;
            }
            accumulated += charWidth;
            index++;
        }

        return text.substring(firstCharacterIndex, index);
    }

    public int getFirstCharacterIndex() {
        return firstCharacterIndex;
    }

    /** Keeps the cursor inside the visible window, scrolling it into view from either side. */
    private void clampScroll() {
        if (firstCharacterIndex > text.length()) {
            firstCharacterIndex = text.length();
        }

        if (cursor < firstCharacterIndex) {
            firstCharacterIndex = cursor;
        } else {
            while (firstCharacterIndex < cursor
                && textRenderer.getWidth(text.substring(firstCharacterIndex, cursor)) > width) {
                firstCharacterIndex++;
            }
        }

        // Springs back when the text shrinks. Without this the window only ever moves right: delete
        // the tail of a long line and the box sits half empty with the start of the line still
        // scrolled off it. Modern's EditBox pulls its own displayPos back for the same reason.
        while (firstCharacterIndex > 0
            && textRenderer.getWidth(text.substring(firstCharacterIndex - 1)) <= width) {
            firstCharacterIndex--;
        }
    }

    private void onChanged() {
        suggestion = "";
        changedListener.accept(text);
    }

}
