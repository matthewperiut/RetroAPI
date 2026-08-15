package com.periut.retroapi.text;

import java.util.Locale;
import java.util.Objects;

/**
 * Only {@code show_text} carries meaning here - beta has no item or entity payloads worth
 * describing - but the action is modelled as an enum so a modern payload round-trips.
 */
public class HoverEvent {
    private final Action action;
    private final Text value;

    public HoverEvent(final Action action, final Text value) {
        this.action = action;
        this.value = value;
    }

    public static HoverEvent showText(final Text text) {
        return new HoverEvent(Action.SHOW_TEXT, text);
    }

    public Action getAction() {
        return action;
    }

    public Text getValue() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HoverEvent)) {
            return false;
        }
        final HoverEvent that = (HoverEvent) o;
        return action == that.action && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, value);
    }

    @Override
    public String toString() {
        return "HoverEvent{action=" + action + ", value=" + value + "}";
    }

    public enum Action {
        SHOW_TEXT("show_text"),
        SHOW_ITEM("show_item"),
        SHOW_ENTITY("show_entity");

        private final String name;

        Action(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static Action byName(final String name) {
            if (name == null) {
                return null;
            }
            final String lower = name.toLowerCase(Locale.ROOT);
            for (final Action action : values()) {
                if (action.name.equals(lower)) {
                    return action;
                }
            }
            return null;
        }
    }
}
