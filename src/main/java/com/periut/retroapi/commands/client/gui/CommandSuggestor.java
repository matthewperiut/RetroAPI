package com.periut.retroapi.commands.client.gui;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;
import com.periut.retroapi.commands.client.ClientCommands;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.commands.SuggestionHelper;
import com.periut.retroapi.text.Formatting;
import com.periut.retroapi.text.Style;
import com.periut.retroapi.text.Text;
import com.periut.retroapi.text.Texts;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * The completion window and the coloured command line, backported from modern Minecraft.
 *
 * <p>Every edit re-parses the input. What the parser understood is coloured argument by argument;
 * what it choked on is underlined in red with the error printed beneath, and whatever could come
 * next is offered in a scrollable window driven by the arrow keys, {@code Tab}, the mouse and the
 * wheel.
 *
 * <p><b>{@code Tab} completes and {@code Enter} sends</b>, as in modern Minecraft. Enter must not
 * complete: a suggestion list can be non-empty for input that is already a finished command
 * ({@code /gamerule s} narrows to three rules while {@code s} itself parses), and an Enter that
 * completed would make the finished form unreachable. The window therefore stays open the whole
 * time it has something to offer, which is what makes typing a letter NARROW the list instead of
 * closing it.
 */
public class CommandSuggestor extends DrawContext {
    /** Modern cycles these across successive arguments so each one is visually distinct. */
    private static final Style[] HIGHLIGHT_STYLES = {
        Style.EMPTY.withColor(Formatting.AQUA),
        Style.EMPTY.withColor(Formatting.YELLOW),
        Style.EMPTY.withColor(Formatting.GREEN),
        Style.EMPTY.withColor(Formatting.LIGHT_PURPLE),
        Style.EMPTY.withColor(Formatting.GOLD),
    };
    private static final Style ERROR_STYLE = Style.EMPTY.withColor(Formatting.RED);
    private static final Style PLAIN_STYLE = Style.EMPTY;

    /** Chat's own limit, and modern's: ten rows hanging off the bottom of the screen. */
    private static final int MAX_VISIBLE = 10;
    private static final int ENTRY_HEIGHT = 12;
    private static final int BACKGROUND = 0xD0000000;
    private static final int SELECTED_COLOR = 0xFFFF00;
    private static final int UNSELECTED_COLOR = 0xAAAAAA;

    private final net.minecraft.client.gui.screen.Screen owner;
    private final ChatInputField input;
    private final TextRenderer textRenderer;

    /**
     * Modern's {@code commandsOnly}: the whole input IS a command, with no leading slash to strip.
     * True for the command block editor, which is where modern turns it on too - false for chat,
     * where a line without a slash is something to say rather than something to run.
     */
    private final boolean commandsOnly;

    /** Where the input's text starts on screen, so the window lines up under what it would replace. */
    private int inputX = 4;
    /** Top of the suggestion list, or -1 for chat's "sit just above the input bar" anchor. */
    private int listY = -1;
    /**
     * How many rows the list may show at once. Chat can afford ten because its list grows upwards into
     * empty screen; a list that drops DOWN from an input has whatever is under it to worry about, which
     * is why modern hands its command block editor a limit of seven and chat ten.
     */
    private int maxVisible = MAX_VISIBLE;

    /** Bumped per request, so a late server answer to an older keystroke can be told apart. */
    private int suggestionToken;

    private ParseResults<RetroCommandSource> parse;
    private CompletableFuture<Suggestions> pendingSuggestions;
    private Window window;
    private boolean completing;
    /** Messages under the input box: a parse error, or the usage list Tab produces. */
    private final List<Text> messages = new ArrayList<>();

    public CommandSuggestor(final RetroChatScreen owner, final ChatInputField input, final TextRenderer textRenderer) {
        this(owner, input, textRenderer, false);
    }

    public CommandSuggestor(final net.minecraft.client.gui.screen.Screen owner, final ChatInputField input,
                            final TextRenderer textRenderer, final boolean commandsOnly) {
        this.owner = owner;
        this.input = input;
        this.textRenderer = textRenderer;
        this.commandsOnly = commandsOnly;
    }

    /**
     * Puts the window somewhere other than chat's corner.
     *
     * @param inputX where the input draws its text, so a completion lines up under the word it replaces
     * @param listY  the top of the list, for an input that is not at the bottom of the screen
     */
    public void setAnchor(final int inputX, final int listY) {
        this.inputX = inputX;
        this.listY = listY;
    }

    /** Caps the list so it ends above whatever the screen has below it. */
    public void setMaxVisible(final int rows) {
        this.maxVisible = Math.max(1, rows);
    }

    /** Re-parses and re-requests completions; called whenever the text changes. */
    public void refresh() {
        final String text = input.getText();

        if (parse != null && !parse.getReader().getString().equals(text)) {
            parse = null;
        }
        if (!completing) {
            input.setSuggestion("");
            window = null;
        }
        messages.clear();

        final boolean slash = text.startsWith("/");
        if (!slash && !commandsOnly) {
            parse = null;
            pendingSuggestions = null;
            return;
        }

        final StringReader reader = new StringReader(text);
        if (slash) {
            reader.skip();
        }

        if (parse == null) {
            parse = ClientCommands.parse(reader);
        }

        final int cursor = input.getCursor();
        if ((commandsOnly || cursor >= 1) && (window == null || !completing)) {
            // The reader was positioned after the slash but still spans the whole input, so every
            // range - and the cursor - is an index into the text as typed.
            // Each request carries a number so a server answer that arrives after the player has typed
            // on is recognised as stale and dropped rather than replacing what they are looking at.
            final int token = ++suggestionToken;
            pendingSuggestions = ClientCommands.suggest(parse, cursor, refined -> {
                if (token == suggestionToken) {
                    pendingSuggestions = CompletableFuture.completedFuture(refined);
                    show();
                }
            });

            final CompletableFuture<Suggestions> requested = pendingSuggestions;
            requested.thenRun(() -> {
                if (requested == pendingSuggestions && requested.isDone()) {
                    show();
                }
            });
        }
    }

    private void show() {
        if (pendingSuggestions == null || !pendingSuggestions.isDone()) {
            return;
        }

        final Suggestions suggestions = pendingSuggestions.join();

        if (input.getCursor() == input.getText().length()) {
            describeErrors(suggestions);
        }

        if (suggestions.isEmpty()) {
            window = null;
            input.setSuggestion("");
            return;
        }

        window = new Window(suggestions);
    }

    /** Explains why a command will not run, using Brigadier's own message and position. */
    private void describeErrors(final Suggestions suggestions) {
        if (parse == null) {
            return;
        }

        // An empty box is not a mistake, it is a box nobody has typed in yet. Brigadier has plenty to
        // say about the empty string - every root child fails, and it reports the first complaint,
        // which in commandsOnly mode is a whitespace one - and modern says none of it until there is
        // something to be wrong about.
        if (input.getText().isBlank()) {
            return;
        }

        if (suggestions.isEmpty() && !parse.getExceptions().isEmpty()) {
            for (final Map.Entry<CommandNode<RetroCommandSource>, CommandSyntaxException> entry : parse.getExceptions().entrySet()) {
                messages.add(Text.literal(entry.getValue().getRawMessage().getString()).formatted(Formatting.RED));
            }
            return;
        }

        if (parse.getReader().canRead()) {
            messages.add(Text.literal(parse.getContext().getRange().isEmpty()
                ? "Unknown command"
                : "Incorrect argument for command").formatted(Formatting.RED));
        }
    }

    /**
     * @return true if the key was consumed by the suggestion window
     */
    public boolean keyPressed(final char typed, final int keyCode) {
        if (window != null) {
            switch (keyCode) {
                case Keys.UP -> {
                    window.select(window.selection - 1);
                    return true;
                }
                case Keys.DOWN -> {
                    window.select(window.selection + 1);
                    return true;
                }
                case Keys.TAB -> {
                    window.complete();
                    return true;
                }
                case Keys.ESCAPE -> {
                    window = null;
                    return true;
                }
                default -> {
                    return false;
                }
            }
        }

        // Tab with nothing to complete lists what the command actually accepts, as modern does.
        if (keyCode == Keys.TAB) {
            showUsages();
            return true;
        }

        return false;
    }

    private void showUsages() {
        if (parse == null || (!commandsOnly && !input.getText().startsWith("/"))) {
            return;
        }

        messages.clear();

        final CommandContextBuilder<RetroCommandSource> context = parse.getContext().getLastChild();
        final List<ParsedCommandNode<RetroCommandSource>> nodes = context.getNodes();
        final CommandNode<RetroCommandSource> node = nodes.isEmpty()
            ? ClientCommands.getDispatcher().getRoot()
            : nodes.get(nodes.size() - 1).getNode();

        final String[] usages = ClientCommands.getDispatcher().getAllUsage(node, ClientCommands.getSource(), true);
        for (final String usage : usages) {
            messages.add(Text.literal(usage).formatted(Formatting.GRAY));
        }
        if (usages.length == 0) {
            messages.add(Text.literal("No further arguments").formatted(Formatting.GRAY));
        }
    }

    public boolean mouseClicked(final int mouseX, final int mouseY, final int button) {
        return window != null && window.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseScrolled(final int amount) {
        if (window == null) {
            return false;
        }
        window.scroll(amount);
        return true;
    }

    public void render(final int mouseX, final int mouseY) {
        if (window != null) {
            window.render(mouseX, mouseY);
            return;
        }

        // Messages sit where the window would have been, so the two never overlap - and under the input
        // they belong to, not at the screen's left edge. Chat's input starts at 4 and that is where
        // these used to be nailed, which put a command block's parse errors half a screen from the box
        // that caused them.
        int y = listY >= 0 ? listY : owner.height - 14 - messages.size() * ENTRY_HEIGHT;
        for (final Text message : messages) {
            final TextDrawer.Line line = TextDrawer.flatten(message);
            final int width = TextDrawer.INSTANCE.width(textRenderer, line);
            fill(inputX - 2, y - 1, inputX + width, y + ENTRY_HEIGHT - 3, BACKGROUND);
            TextDrawer.INSTANCE.draw(textRenderer, line, inputX, y, 255);
            y += ENTRY_HEIGHT;
        }
    }

    /** Colours the visible slice of the input the way modern colours a command as you type it. */
    public TextDrawer.Line highlight(final String visible, final int firstCharacterIndex) {
        if (parse == null) {
            return new TextDrawer.Line(List.of(new Texts.Segment(visible, PLAIN_STYLE)));
        }

        final List<Texts.Segment> segments = new ArrayList<>();
        int consumed = 0;
        int styleIndex = -1;

        // Ranges are absolute over the whole input; the visible slice starts here.
        final int offset = firstCharacterIndex;

        final CommandContextBuilder<RetroCommandSource> context = parse.getContext().getLastChild();
        for (final ParsedArgument<RetroCommandSource, ?> argument : context.getArguments().values()) {
            styleIndex = (styleIndex + 1) % HIGHLIGHT_STYLES.length;

            final int start = Math.max(argument.getRange().getStart() - offset, 0);
            if (start >= visible.length()) {
                break;
            }

            final int end = Math.min(argument.getRange().getEnd() - offset, visible.length());
            if (end > 0) {
                segments.add(new Texts.Segment(visible.substring(consumed, start), PLAIN_STYLE));
                segments.add(new Texts.Segment(visible.substring(start, end), HIGHLIGHT_STYLES[styleIndex]));
                consumed = end;
            }
        }

        if (parse.getReader().canRead()) {
            final int errorStart = Math.max(parse.getReader().getCursor() - offset, 0);
            if (errorStart < visible.length()) {
                final int errorEnd = Math.min(errorStart + parse.getReader().getRemainingLength(), visible.length());
                segments.add(new Texts.Segment(visible.substring(consumed, errorStart), PLAIN_STYLE));
                segments.add(new Texts.Segment(visible.substring(errorStart, errorEnd), ERROR_STYLE));
                consumed = errorEnd;
            }
        }

        segments.add(new Texts.Segment(visible.substring(consumed), PLAIN_STYLE));
        return new TextDrawer.Line(segments);
    }

    /** The scrollable list of completions. */
    private final class Window {
        private final Suggestions suggestions;
        private final List<Suggestion> entries;
        private final int x;
        private final int width;

        private int selection;
        private int scrollOffset;

        private Window(final Suggestions suggestions) {
            this.suggestions = suggestions;
            this.entries = suggestions.getList();

            int widest = 0;
            for (final Suggestion suggestion : entries) {
                widest = Math.max(widest, textRenderer.getWidth(suggestion.getText()));
            }
            this.width = widest + 2;

            // Line the window up with the text it would replace, but keep it on screen.
            final String visible = input.visibleText();
            final int start = Math.min(suggestions.getRange().getStart(), input.getText().length());
            final int visibleStart = Math.max(0, Math.min(start - input.getFirstCharacterIndex(), visible.length()));
            final int offset = textRenderer.getWidth(visible.substring(0, visibleStart));
            this.x = Math.max(2, Math.min(inputX + offset, owner.width - width - 2));

            // Modern previews the first entry as soon as the window appears.
            select(0);
        }

        private void select(final int index) {
            selection = Math.floorMod(index, entries.size());

            if (selection < scrollOffset) {
                scrollOffset = selection;
            } else if (selection >= scrollOffset + maxVisible) {
                scrollOffset = selection - maxVisible + 1;
            }

            // Modern previews the highlighted entry as ghost text after the cursor.
            final String applied = entries.get(selection).apply(input.getText());
            input.setSuggestion(applied.startsWith(input.getText()) ? applied.substring(input.getText().length()) : "");
        }

        private void scroll(final int amount) {
            final int max = Math.max(0, entries.size() - maxVisible);
            scrollOffset = Math.max(0, Math.min(scrollOffset - amount, max));
        }

        private void complete() {
            completing = true;
            input.setText(entries.get(selection).apply(input.getText()));
            completing = false;
            window = null;
            refresh();
        }

        private boolean mouseClicked(final int mouseX, final int mouseY, final int button) {
            if (button != 0) {
                return false;
            }

            final int entry = entryAt(mouseX, mouseY);
            if (entry < 0) {
                return false;
            }

            select(entry);
            complete();
            return true;
        }

        /** Which entry the pointer is over, or -1. */
        private int entryAt(final int mouseX, final int mouseY) {
            final int visibleCount = Math.min(entries.size(), maxVisible);
            final int top = topY(visibleCount);

            if (mouseX < x || mouseX > x + width || mouseY < top || mouseY > top + visibleCount * ENTRY_HEIGHT) {
                return -1;
            }
            return scrollOffset + (mouseY - top) / ENTRY_HEIGHT;
        }

        private int topY(final int visibleCount) {
            // Chat hangs its list off the bottom of the screen; an input in the middle of a screen
            // (the command block editor) says where its own list goes.
            return listY >= 0 ? listY : owner.height - 14 - visibleCount * ENTRY_HEIGHT;
        }

        private void render(final int mouseX, final int mouseY) {
            final int visibleCount = Math.min(entries.size(), maxVisible);
            final int top = topY(visibleCount);

            // Pointing at an entry selects it: it turns yellow and previews as ghost text, so what a
            // click would take is what is already being shown rather than something a click reveals.
            final int hovered = entryAt(mouseX, mouseY);
            if (hovered >= 0 && hovered != selection) {
                select(hovered);
            }

            fill(x, top, x + width, top + visibleCount * ENTRY_HEIGHT, BACKGROUND);

            for (int i = 0; i < visibleCount; i++) {
                final int index = scrollOffset + i;
                final Suggestion suggestion = entries.get(index);
                final int y = top + i * ENTRY_HEIGHT + 2;
                drawTextWithShadow(textRenderer, suggestion.getText(), x + 1, y, index == selection ? SELECTED_COLOR : UNSELECTED_COLOR);
            }

            if (entries.size() > maxVisible) {
                renderScrollBar(top, visibleCount);
            }

            renderTooltip(top, visibleCount, mouseX, mouseY);
        }

        private void renderScrollBar(final int top, final int visibleCount) {
            final int trackHeight = visibleCount * ENTRY_HEIGHT;
            final int barHeight = Math.max(4, trackHeight * visibleCount / entries.size());
            final int travel = trackHeight - barHeight;
            final int barY = top + travel * scrollOffset / Math.max(1, entries.size() - visibleCount);

            fill(x + width, top, x + width + 2, top + trackHeight, 0x40FFFFFF);
            fill(x + width, barY, x + width + 2, barY + barHeight, 0xFFAAAAAA);
        }

        /** Shows the highlighted entry's tooltip, which is where argument descriptions surface. */
        private void renderTooltip(final int top, final int visibleCount, final int mouseX, final int mouseY) {
            final Suggestion suggestion = entries.get(selection);
            if (suggestion.getTooltip() == null) {
                return;
            }

            final Text tooltip = Texts.of(suggestion.getTooltip());
            final TextDrawer.Line line = TextDrawer.flatten(tooltip);
            final int tooltipWidth = TextDrawer.INSTANCE.width(textRenderer, line);

            // Beside the entry it describes, not above the list: above is where the command line is,
            // and a description that covers what you are typing is worse than no description. The
            // scrollbar, when there is one, sits in the two pixels immediately right of the list.
            final int gutter = entries.size() > maxVisible ? 4 : 2;
            int tooltipX = x + width + gutter;
            if (tooltipX + tooltipWidth + 2 > owner.width) {
                // No room on the right: put it on the left of the list instead of off the screen.
                tooltipX = Math.max(2, x - tooltipWidth - gutter - 2);
            }
            final int tooltipY = top + (selection - scrollOffset) * ENTRY_HEIGHT + 2;

            fill(tooltipX - 1, tooltipY - 2, tooltipX + tooltipWidth + 1, tooltipY + ENTRY_HEIGHT - 3, BACKGROUND);
            TextDrawer.INSTANCE.draw(textRenderer, line, tooltipX, tooltipY, 255);
        }
    }
}
