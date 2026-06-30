package com.oliveryasuna.mc.omniconfig.fabric.gui;

import com.oliveryasuna.mc.omniconfig.api.ConfigManager;
import com.oliveryasuna.mc.omniconfig.api.annotation.Reload;
import com.oliveryasuna.mc.omniconfig.api.annotation.Sync;
import com.oliveryasuna.mc.omniconfig.api.annotation.Widget;
import com.oliveryasuna.mc.omniconfig.schema.EntryMetadata;
import com.oliveryasuna.mc.omniconfig.schema.Schema;
import com.oliveryasuna.mc.omniconfig.schema.SchemaCategory;
import com.oliveryasuna.mc.omniconfig.schema.SchemaEntry;
import com.oliveryasuna.mc.omniconfig.validation.Validator;
import com.oliveryasuna.mc.omniconfig.validation.validator.OneOfValidator;
import com.oliveryasuna.mc.omniconfig.validation.validator.RangeValidator;
import com.oliveryasuna.mc.omniconfig.value.ValueType;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.awt.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.List;
import java.util.function.Function;

public final class YaclScreenProvider implements ScreenProvider {

    //==================================================
    // Static methods
    //==================================================

    private static Component displayName(
            final SchemaEntry entry,
            final EntryMetadata meta
    ) {
        final MutableComponent base = Component.literal(entry.getKey());

        // Reload tier and sync scope are non-obvious from the label alone —
        // suffix them so users see at a glance why a value won't take effect
        // immediately or won't sync.
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

    private static OptionDescription description(final EntryMetadata meta) {
        final OptionDescription.Builder b = OptionDescription.createBuilder();
        if(!meta.getComment().isEmpty()) {
            b.text(Component.literal(String.join("\n", meta.getComment())));
        }

        return b.build();
    }

    private static Optional<RangeValidator> findRange(final EntryMetadata meta) {
        for(final Validator<?> v : meta.getValidators()) {
            if(v instanceof final RangeValidator r) {
                return Optional.of(r);
            }
        }

        return Optional.empty();
    }

    private static Optional<OneOfValidator> findOneOf(final EntryMetadata meta) {
        for(final Validator<?> v : meta.getValidators()) {
            if(v instanceof final OneOfValidator o) {
                return Optional.of(o);
            }
        }

        return Optional.empty();
    }

    private static boolean useSlider(
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

    private static double sliderStep(
            final double min,
            final double max
    ) {
        // 200 ticks across the range is a reasonable default — granular enough
        // for typical opacity/volume sliders, coarse enough that drag feels
        // responsive. Override via @Widget extensions later if needed.
        final double span = max - min;
        if(span <= 0) {
            return 0.01;
        }

        return span / 200.0;
    }

    private static Object coerceNullDefault(
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

    private static java.util.function.Function<String, Object> stringForLeaf(final Class<?> raw) {
        // Placeholder: future codec-round-trip hook. For now the staged value
        // is the raw String the user typed, and manager.set will throw if it
        // doesn't decode — which surfaces as a YACL "save failed" toast.
        return s -> s;
    }

    private static Color parseColor(
            final String hex,
            final Color fallback
    ) {
        if(hex == null || hex.isBlank()) {
            return fallback;
        }

        try {
            return Color.decode(hex.startsWith("#") ? hex : "#" + hex);
        } catch(final NumberFormatException invalid) {
            return fallback;
        }
    }

    private static String formatColor(final Color c) {
        return String.format(Locale.ROOT, "#%06X", c.getRGB() & 0xFFFFFF);
    }

    //==================================================
    // Constructors
    //==================================================

    public YaclScreenProvider() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public Screen create(
            final Minecraft client,
            final Screen parent,
            final ConfigManager<?> manager
    ) {
        final Schema schema = manager.getSchema();
        // Staged values live until the user clicks Save & Exit. Each entry's
        // setter writes here; cancel = discard, save = flush via manager.set
        // (which validates) then manager.save (which persists).
        final Map<String, Object> staged = new LinkedHashMap<>();

        final YetAnotherConfigLib.Builder ycl = YetAnotherConfigLib.createBuilder()
                .title(Component.literal(schema.name()));

        final SchemaCategory root = schema.root();
        // Top-level entries belong to no @Category; bucket them under a
        // "General" tab keyed by the schema id so multi-tab nav still works.
        if(!root.entries().isEmpty()) {
            final OptionGroup.Builder rootGroup = OptionGroup.createBuilder()
                    .name(Component.literal("General"))
                    .collapsed(false);
            for(final SchemaEntry entry : root.entries()) {
                addEntryAsOption(rootGroup, entry, entry.getKey(), staged, manager);
            }
            ycl.category(ConfigCategory.createBuilder()
                    .name(Component.literal("General"))
                    .group(rootGroup.build())
                    .build());
        }
        for(final SchemaCategory child : root.categories()) {
            ycl.category(buildCategory(child, child.getName(), staged, manager));
        }

        ycl.save(() -> {
            try {
                for(final Map.Entry<String, Object> e : staged.entrySet()) {
                    manager.set(e.getKey(), e.getValue());
                }
                manager.save();
            } catch(final IOException io) {
                throw new UncheckedIOException(io);
            }
        });

        return ycl.build().generateScreen(parent);
    }

    private ConfigCategory buildCategory(
            final SchemaCategory cat,
            final String pathPrefix,
            final Map<String, Object> staged,
            final ConfigManager<?> manager
    ) {
        final ConfigCategory.Builder b = ConfigCategory.createBuilder()
                .name(Component.literal(cat.getName()));

        if(!cat.getComment().isEmpty()) {
            b.tooltip(Component.literal(String.join("\n", cat.getComment())));
        }

        // Direct entries in this category → one un-collapsed group.
        if(!cat.entries().isEmpty()) {
            final OptionGroup.Builder grp = OptionGroup.createBuilder()
                    .name(Component.literal(cat.getName()))
                    .collapsed(false);
            for(final SchemaEntry entry : cat.entries()) {
                addEntryAsOption(grp, entry, pathPrefix + "." + entry.getKey(), staged, manager);
            }
            b.group(grp.build());
        }

        // Each nested SchemaCategory becomes a collapsible OptionGroup. Deeper
        // nesting collapses into one flat group per branch — YACL groups don't
        // recurse, so we flatten naming with the dotted prefix.
        for(final SchemaCategory child : cat.categories()) {
            final OptionGroup.Builder grp = OptionGroup.createBuilder()
                    .name(Component.literal(child.getName()))
                    .collapsed(false);
            flattenInto(grp, child, pathPrefix + "." + child.getName(), staged, manager);
            b.group(grp.build());
        }

        return b.build();
    }

    private void flattenInto(
            final OptionGroup.Builder grp,
            final SchemaCategory cat,
            final String pathPrefix,
            final Map<String, Object> staged,
            final ConfigManager<?> manager
    ) {
        for(final SchemaEntry entry : cat.entries()) {
            addEntryAsOption(grp, entry, pathPrefix + "." + entry.getKey(), staged, manager);
        }
        for(final SchemaCategory child : cat.categories()) {
            flattenInto(grp, child, pathPrefix + "." + child.getName(), staged, manager);
        }
    }

    private void addEntryAsOption(
            final OptionGroup.Builder grp,
            final SchemaEntry entry,
            final String path,
            final Map<String, Object> staged,
            final ConfigManager<?> manager
    ) {
        final EntryMetadata meta = entry.getMetadata();
        if(meta.isHidden()) {
            return;
        }

        final Option<?> option = buildOption(entry, path, staged, manager);
        if(option != null) {
            grp.option(option);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Option<?> buildOption(
            final SchemaEntry entry,
            final String path,
            final Map<String, Object> staged,
            final ConfigManager<?> manager
    ) {
        final EntryMetadata meta = entry.getMetadata();
        final ValueType type = entry.getType();
        final Class<?> raw = type.getRawType();

        return switch(type.getKind()) {
            case SCALAR -> buildScalarOption(entry, path, staged, manager, raw);
            case ENUM -> buildEnumOption(entry, path, staged, manager, (Class)raw);
            case LIST, MAP, OBJECT -> buildPlaceholderOption(entry, path);
        };
    }

    private Option<?> buildScalarOption(
            final SchemaEntry entry,
            final String path,
            final Map<String, Object> staged,
            final ConfigManager<?> manager,
            final Class<?> raw
    ) {
        final EntryMetadata meta = entry.getMetadata();

        if(raw == boolean.class || raw == Boolean.class) {
            return scalarOption(Boolean.class, entry, path, staged, manager,
                    opt -> BooleanControllerBuilder.create(opt)
                            .yesNoFormatter()
                            .coloured(true));
        } else if(raw == String.class) {
            final Optional<OneOfValidator> oneOf = findOneOf(meta);
            if(oneOf.isPresent()) {
                final List<String> allowed = new ArrayList<>(oneOf.get().allowed());

                return scalarOption(String.class, entry, path, staged, manager,
                        opt -> dev.isxander.yacl3.api.controller.DropdownStringControllerBuilder.create(opt)
                                .values(allowed)
                                .allowAnyValue(false));
            }

            if(meta.getWidget() == Widget.Type.COLOR) {
                return buildColorOption(entry, path, staged, manager);
            }

            return scalarOption(String.class, entry, path, staged, manager, StringControllerBuilder::create);
        } else if(raw == int.class || raw == Integer.class) {
            final Optional<RangeValidator> r = findRange(meta);
            if(useSlider(meta, r)) {
                final RangeValidator rv = r.get();
                return scalarOption(Integer.class, entry, path, staged, manager,
                        opt -> IntegerSliderControllerBuilder.create(opt)
                                .range((int)Math.round(rv.min()), (int)Math.round(rv.max()))
                                .step(1));
            }

            return scalarOption(Integer.class, entry, path, staged, manager, IntegerFieldControllerBuilder::create);
        } else if(raw == long.class || raw == Long.class) {
            final Optional<RangeValidator> r = findRange(meta);
            if(useSlider(meta, r)) {
                final RangeValidator rv = r.get();
                return scalarOption(Long.class, entry, path, staged, manager,
                        opt -> LongSliderControllerBuilder.create(opt)
                                .range(Math.round(rv.min()), Math.round(rv.max()))
                                .step(1L));
            }

            return scalarOption(Long.class, entry, path, staged, manager, LongFieldControllerBuilder::create);
        } else if(raw == double.class || raw == Double.class) {
            final Optional<RangeValidator> r = findRange(meta);
            if(useSlider(meta, r)) {
                final RangeValidator rv = r.get();
                return scalarOption(Double.class, entry, path, staged, manager,
                        opt -> DoubleSliderControllerBuilder.create(opt)
                                .range(rv.min(), rv.max())
                                .step(sliderStep(rv.min(), rv.max())));
            }

            return scalarOption(Double.class, entry, path, staged, manager, DoubleFieldControllerBuilder::create);
        } else if(raw == float.class || raw == Float.class) {
            final Optional<RangeValidator> r = findRange(meta);
            if(useSlider(meta, r)) {
                final RangeValidator rv = r.get();
                return scalarOption(Float.class, entry, path, staged, manager,
                        opt -> FloatSliderControllerBuilder.create(opt)
                                .range((float)rv.min(), (float)rv.max())
                                .step((float)sliderStep(rv.min(), rv.max())));
            }

            return scalarOption(Float.class, entry, path, staged, manager, FloatFieldControllerBuilder::create);
        }

        // Codec-mediated leaves (UUID/Instant/Duration/ResourceLocation/etc.):
        // render as String via toString round-trip. Real codec round-trip is
        // future work — ScalarCodec.encode/decode through the entry's
        // CodecRegistry would let edits flow through validation cleanly.
        return scalarOption(String.class, entry, path, staged, manager,
                StringControllerBuilder::create,
                () -> String.valueOf(entry.readFrom(manager.get())),
                () -> entry.getDefaultValue() == null ? "" : entry.getDefaultValue().toString(),
                stringForLeaf(raw));
    }

    private <E extends Enum<E>> Option<?> buildEnumOption(
            final SchemaEntry entry,
            final String path,
            final Map<String, Object> staged,
            final ConfigManager<?> manager,
            final Class<E> enumClass
    ) {
        return scalarOption(enumClass, entry, path, staged, manager, opt -> EnumControllerBuilder.create(opt).enumClass(enumClass));
    }

    private Option<?> buildColorOption(
            final SchemaEntry entry,
            final String path,
            final Map<String, Object> staged,
            final ConfigManager<?> manager
    ) {
        final EntryMetadata meta = entry.getMetadata();
        final Color def = parseColor((String)entry.getDefaultValue(), Color.WHITE);

        return Option.<Color>createBuilder()
                .name(displayName(entry, meta))
                .description(description(meta))
                .binding(
                        def,
                        () -> {
                            final Object current = staged.containsKey(path)
                                    ? staged.get(path)
                                    : entry.readFrom(manager.get());
                            return parseColor(String.valueOf(current), def);
                        },
                        v -> staged.put(path, formatColor(v))
                )
                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(false))
                .build();
    }

    private Option<?> buildPlaceholderOption(
            final SchemaEntry entry,
            final String path
    ) {
        // Lists / maps / nested objects render as a disabled label until
        // dedicated YACL ListOption / nested-screen wiring lands. Skipping
        // them entirely would hide their existence from users editing the GUI;
        // a visible disabled row keeps discoverability.
        final EntryMetadata meta = entry.getMetadata();
        return Option.<String>createBuilder()
                .name(Component.literal(entry.getKey() + " (edit on disk)")
                        .withStyle(ChatFormatting.GRAY))
                .description(description(meta))
                .binding("(complex)", () -> "(complex)", v -> {
                })
                .controller(StringControllerBuilder::create)
                .available(false)
                .build();
    }

    // Per-type scalar option builder. typeClass is what YACL binds & the
    // staged map stores; manager.set will receive the same.
    private <T> Option<T> scalarOption(
            final Class<T> typeClass,
            final SchemaEntry entry,
            final String path,
            final Map<String, Object> staged,
            final ConfigManager<?> manager,
            final Function<Option<T>, ? extends ControllerBuilder<T>> controller
    ) {
        return scalarOption(typeClass, entry, path, staged, manager, controller, null, null, null);
    }

    @SuppressWarnings("unchecked")
    private <T> Option<T> scalarOption(
            final Class<T> typeClass,
            final SchemaEntry entry,
            final String path,
            final Map<String, Object> staged,
            final ConfigManager<?> manager,
            final Function<Option<T>, ? extends ControllerBuilder<T>> controller,
            final java.util.function.Supplier<T> getterOverride,
            final java.util.function.Supplier<T> defaultOverride,
            final java.util.function.Function<T, Object> setterAdapter
    ) {
        final EntryMetadata meta = entry.getMetadata();
        final T defaultValue = defaultOverride != null
                ? defaultOverride.get()
                : typeClass.cast(coerceNullDefault(entry.getDefaultValue(), typeClass));

        return Option.<T>createBuilder()
                .name(displayName(entry, meta))
                .description(description(meta))
                .binding(
                        defaultValue,
                        () -> {
                            if(getterOverride != null) {
                                return getterOverride.get();
                            }
                            if(staged.containsKey(path)) {
                                return (T)staged.get(path);
                            }
                            final Object live = entry.readFrom(manager.get());
                            return live == null ? defaultValue : (T)live;
                        },
                        v -> staged.put(path, setterAdapter != null ? setterAdapter.apply(v) : v))
                .controller(controller::apply)
                .build();
    }

}
