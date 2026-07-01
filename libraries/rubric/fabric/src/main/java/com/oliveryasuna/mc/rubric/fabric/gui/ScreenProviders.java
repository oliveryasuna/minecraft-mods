package com.oliveryasuna.mc.rubric.fabric.gui;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.rubric.api.annotation.Reload;
import com.oliveryasuna.mc.rubric.api.annotation.Sync;
import com.oliveryasuna.mc.rubric.schema.EntryMetadata;
import com.oliveryasuna.mc.rubric.schema.SchemaEntry;
import com.oliveryasuna.mc.rubric.validation.Validator;
import com.oliveryasuna.mc.rubric.validation.validator.OneOfValidator;
import com.oliveryasuna.mc.rubric.validation.validator.RangeValidator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Shared helpers used by frontend {@link ScreenProvider} implementations
 * ({@link YaclScreenProvider}, {@link ClothScreenProvider}). Pure utilities:
 * no framework binding, no state.
 */
public final class ScreenProviders {

    //==================================================
    // Static methods
    //==================================================

    /**
     * Renders an entry's display name, optionally with reload-tier and
     * sync-scope tag suffixes (e.g. {@code "opacity [restart, server]"}) so
     * users see at a glance why a value won't take effect immediately or won't
     * sync. Callers read the {@code gui.showMetadataSuffixes} config flag and
     * pass the result — keeps this helper free of a runtime config dependency.
     */
    public static Component displayName(
            final SchemaEntry entry,
            final EntryMetadata meta,
            final boolean showSuffixes
    ) {
        final MutableComponent base = Component.literal(entry.getKey());

        if(!showSuffixes) {
            return base;
        }

        final List<String> tags = new ArrayList<>();
        if(meta.getReloadTier() == Reload.Tier.RESTART) {
            tags.add("restart");
        } else if(meta.getReloadTier() == Reload.Tier.WORLD) {
            tags.add("world");
        }
        if(meta.getSyncScope() == Sync.Scope.SERVER) {
            tags.add("server");
        } else if(meta.getSyncScope() == Sync.Scope.COMMON) {
            tags.add("common");
        }
        if(tags.isEmpty()) {
            return base;
        }

        return base.append(Component.literal(" [" + String.join(", ", tags) + "]")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * @return The first {@link RangeValidator} attached to {@code meta}, if
     * any. Frontends read this to size numeric sliders / field bounds.
     */
    public static Optional<RangeValidator> findRange(final EntryMetadata meta) {
        for(final Validator<?> v : meta.getValidators()) {
            if(v instanceof final RangeValidator r) {
                return Optional.of(r);
            }
        }

        return Optional.empty();
    }

    /**
     * @return The first {@link OneOfValidator} attached to {@code meta}, if
     * any. Frontends read this to populate dropdown selections.
     */
    public static Optional<OneOfValidator> findOneOf(final EntryMetadata meta) {
        for(final Validator<?> v : meta.getValidators()) {
            if(v instanceof final OneOfValidator o) {
                return Optional.of(o);
            }
        }

        return Optional.empty();
    }

    /**
     * @return {@code true} when a slider widget is appropriate for a numeric
     * entry: a bounded finite range is present, and the entry's
     * {@link com.oliveryasuna.mc.rubric.api.annotation.Widget @Widget}
     * kind is AUTO or SLIDER.
     */
    public static boolean useSlider(
            final EntryMetadata meta,
            final Optional<RangeValidator> range
    ) {
        if(range.isEmpty()) {
            return false;
        }

        final RangeValidator r = range.get();
        if(!Double.isFinite(r.min()) || !Double.isFinite(r.max())) {
            return false;
        }

        return switch(meta.getWidget()) {
            case AUTO, SLIDER -> true;
            default -> false;
        };
    }

    /**
     * Uniform slider step: 200 ticks across the range — granular enough for
     * typical opacity/volume sliders, coarse enough that drag feels
     * responsive. Callers coerce to the widget's numeric type.
     */
    public static double sliderStep(
            final double min,
            final double max
    ) {
        final double span = max - min;
        if(span <= 0) {
            return 0.01;
        }

        return span / 200.0;
    }

    /**
     * Falls back to a boxed-primitive zero when a scalar entry has no default
     * value (unusual — schema reader normally captures a POJO field value).
     * Prevents an NPE at binding time.
     */
    public static Object coerceNullDefault(
            final Object def,
            final Class<?> typeClass
    ) {
        if(def != null) {
            return def;
        }

        if(typeClass == Boolean.class) {
            return Boolean.FALSE;
        } else if(typeClass == Integer.class) {
            return 0;
        } else if(typeClass == Long.class) {
            return 0L;
        } else if(typeClass == Float.class) {
            return 0f;
        } else if(typeClass == Double.class) {
            return 0d;
        } else if(typeClass == String.class) {
            return "";
        }

        return null;
    }

    /**
     * Parses a {@code "#RRGGBB"} (or bare {@code "RRGGBB"}) hex string into a
     * masked 24-bit RGB int. Returns {@code fallback} on null / blank /
     * unparseable input.
     */
    public static int parseColor(
            final String hex,
            final int fallback
    ) {
        if(hex == null || hex.isBlank()) {
            return fallback;
        }

        try {
            return Color.decode(hex.startsWith("#") ? hex : "#" + hex).getRGB() & 0xFFFFFF;
        } catch(final NumberFormatException invalid) {
            return fallback;
        }
    }

    /**
     * Formats a 24-bit RGB int as {@code "#RRGGBB"} (uppercase, alpha stripped).
     */
    public static String formatColor(final int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    //==================================================
    // Constructors
    //==================================================

    private ScreenProviders() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
