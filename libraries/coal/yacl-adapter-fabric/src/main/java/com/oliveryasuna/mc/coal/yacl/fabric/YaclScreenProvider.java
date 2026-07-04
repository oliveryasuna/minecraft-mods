package com.oliveryasuna.mc.coal.yacl.fabric;

import com.oliveryasuna.mc.coal.api.annotation.Widget;
import com.oliveryasuna.mc.coal.api.config.ConfigManager;
import com.oliveryasuna.mc.coal.api.gui.ScreenProvider;
import com.oliveryasuna.mc.coal.api.schema.*;
import com.oliveryasuna.mc.coal.api.validation.Validator;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;

/**
 * YACL-backed {@link ScreenProvider}. Priority {@code 50} — leaves headroom
 * for provider-preferred frontends. Renders scalars, enums, and
 * {@code @OneOf}-driven dropdowns; lists/maps/objects render as a disabled
 * placeholder pointing at the on-disk file.
 * <p>
 * Values are staged as the user edits and flushed to
 * {@link ConfigManager#set} + {@link ConfigManager#save} on YACL's Save &amp;
 * Quit callback.
 */
public final class YaclScreenProvider implements ScreenProvider {

    //==================================================
    // Static fields
    //==================================================

    private static final Logger LOGGER = LoggerFactory.getLogger("coal-yacl-adapter");

    //==================================================
    // Static methods
    //==================================================

    private static OptionDescription description(final EntryMetadata meta) {
        final OptionDescription.Builder b = OptionDescription.createBuilder();
        if(!meta.comment().isEmpty()) {
            b.text(Component.literal(String.join("\n", meta.comment())));
        }

        return b.build();
    }

    private static Object readLive(
            final ConfigManager<?> manager,
            final String path
    ) {
        return manager.schema().find(path).map(entry -> entry.readFrom(manager.get())).orElse(null);
    }

    @SuppressWarnings("unchecked")
    private static <T> T coerce(
            final Object value,
            final Class<T> target
    ) {
        if(value == null) {
            if(target == Boolean.class) {
                return (T)Boolean.FALSE;
            } else if(target == Integer.class) {
                return (T)Integer.valueOf(0);
            } else if(target == Long.class) {
                return (T)Long.valueOf(0L);
            } else if(target == Double.class) {
                return (T)Double.valueOf(0d);
            } else if(target == Float.class) {
                return (T)Float.valueOf(0f);
            } else if(target == String.class) {
                return (T)"";
            } else if(target.isEnum()) {
                return target.getEnumConstants()[0];
            }

            return null;
        } else if(target.isInstance(value)) {
            return target.cast(value);
        } else if(value instanceof final Number n) {
            if(target == Integer.class) {
                return (T)Integer.valueOf(n.intValue());
            } else if(target == Long.class) {
                return (T)Long.valueOf(n.longValue());
            } else if(target == Double.class) {
                return (T)Double.valueOf(n.doubleValue());
            } else if(target == Float.class) {
                return (T)Float.valueOf(n.floatValue());
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static <V extends Validator<?>> Optional<V> findValidator(
            final EntryMetadata meta,
            final Class<V> type
    ) {
        for(final Validator<?> v : meta.validators()) {
            if(type.isInstance(v)) {
                return Optional.of((V)v);
            }
        }

        return Optional.empty();
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

    // ScreenProvider
    //--------------------------------------------------

    @Override
    public String id() {
        return "coal-yacl-adapter";
    }

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public Screen create(
            final Minecraft client,
            final Screen parent,
            final ConfigManager<?> manager
    ) {
        final Schema schema = manager.schema();
        final Map<String, Object> staged = new LinkedHashMap<>();

        final YetAnotherConfigLib.Builder ycl = YetAnotherConfigLib.createBuilder()
                .title(Component.literal(schema.name()));

        final SchemaCategory root = schema.root();
        if(!root.entries().isEmpty()) {
            final OptionGroup.Builder rootGroup = OptionGroup.createBuilder()
                    .name(Component.literal("General"))
                    .collapsed(false);
            for(final SchemaEntry entry : root.entries()) {
                addEntry(rootGroup, entry, entry.key(), manager, staged);
            }
            ycl.category(ConfigCategory.createBuilder()
                    .name(Component.literal("General"))
                    .group(rootGroup.build())
                    .build());
        }
        for(final SchemaCategory child : root.categories()) {
            ycl.category(buildCategory(child, child.name(), manager, staged));
        }

        ycl.save(() -> flush(manager, staged));

        return ycl.build().generateScreen(parent);
    }

    // Category / group assembly
    //--------------------------------------------------

    private ConfigCategory buildCategory(
            final SchemaCategory cat,
            final String pathPrefix,
            final ConfigManager<?> manager,
            final Map<String, Object> staged
    ) {
        final ConfigCategory.Builder b = ConfigCategory.createBuilder()
                .name(Component.literal(cat.name()));
        if(!cat.comment().isEmpty()) {
            b.tooltip(Component.literal(String.join("\n", cat.comment())));
        }

        if(!cat.entries().isEmpty()) {
            final OptionGroup.Builder grp = OptionGroup.createBuilder()
                    .name(Component.literal(cat.name()))
                    .collapsed(false);
            for(final SchemaEntry entry : cat.entries()) {
                addEntry(grp, entry, pathPrefix + "." + entry.key(), manager, staged);
            }
            b.group(grp.build());
        }

        for(final SchemaCategory child : cat.categories()) {
            final OptionGroup.Builder grp = OptionGroup.createBuilder()
                    .name(Component.literal(child.name()))
                    .collapsed(false);
            flattenInto(grp, child, pathPrefix + "." + child.name(), manager, staged);
            b.group(grp.build());
        }
        return b.build();
    }

    private void flattenInto(
            final OptionGroup.Builder grp,
            final SchemaCategory cat,
            final String pathPrefix,
            final ConfigManager<?> manager,
            final Map<String, Object> staged
    ) {
        for(final SchemaEntry entry : cat.entries()) {
            addEntry(grp, entry, pathPrefix + "." + entry.key(), manager, staged);
        }
        for(final SchemaCategory child : cat.categories()) {
            flattenInto(grp, child, pathPrefix + "." + child.name(), manager, staged);
        }
    }

    private void addEntry(
            final OptionGroup.Builder grp,
            final SchemaEntry entry,
            final String path,
            final ConfigManager<?> manager,
            final Map<String, Object> staged
    ) {
        if(entry.metadata().isHidden()) {
            return;
        }

        final Option<?> option = buildOption(entry, path, manager, staged);
        if(option != null) {
            grp.option(option);
        }
    }

    // Option builders
    //--------------------------------------------------

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Option<?> buildOption(
            final SchemaEntry entry,
            final String path,
            final ConfigManager<?> manager,
            final Map<String, Object> staged
    ) {
        final ValueType type = entry.type();
        final Class<?> raw = type.rawType();

        return switch(type.kind()) {
            case SCALAR -> buildScalar(entry, path, manager, staged, raw);
            case ENUM -> buildEnum(entry, path, manager, staged, (Class)raw);
            case LIST, MAP, OBJECT -> buildPlaceholder(entry);
        };
    }

    private Option<?> buildScalar(
            final SchemaEntry entry,
            final String path,
            final ConfigManager<?> manager,
            final Map<String, Object> staged,
            final Class<?> raw
    ) {
        final EntryMetadata meta = entry.metadata();

        if(raw == boolean.class || raw == Boolean.class) {
            return option(
                    Boolean.class,
                    entry,
                    path,
                    manager,
                    staged,
                    opt -> BooleanControllerBuilder.create(opt)
                            .yesNoFormatter()
                            .coloured(true)
            );
        } else if(raw == String.class) {
            final Optional<Validators.OneOfValidator> oneOf = findValidator(meta, Validators.OneOfValidator.class);
            if(oneOf.isPresent()) {
                final List<String> allowed = new ArrayList<>(oneOf.get().getAllowed());

                return option(
                        String.class,
                        entry,
                        path,
                        manager,
                        staged,
                        opt -> DropdownStringControllerBuilder.create(opt)
                                .values(allowed)
                                .allowAnyValue(false)
                );
            }
            if(meta.widget() == Widget.Type.COLOR) {
                return buildColor(entry, path, manager, staged);
            }

            return option(String.class, entry, path, manager, staged, StringControllerBuilder::create);
        }

        final Optional<Validators.RangeValidator> range = findValidator(meta, Validators.RangeValidator.class);
        if(raw == int.class || raw == Integer.class) {
            if(range.isPresent()) {
                final Validators.RangeValidator rv = range.get();

                return option(
                        Integer.class,
                        entry,
                        path,
                        manager,
                        staged,
                        opt -> IntegerSliderControllerBuilder.create(opt)
                                .range((int)Math.round(rv.getMin()), (int)Math.round(rv.getMax()))
                                .step(1)
                );
            }

            return option(Integer.class, entry, path, manager, staged, IntegerFieldControllerBuilder::create);
        } else if(raw == long.class || raw == Long.class) {
            if(range.isPresent()) {
                final Validators.RangeValidator rv = range.get();

                return option(
                        Long.class,
                        entry,
                        path,
                        manager,
                        staged,
                        opt -> LongSliderControllerBuilder.create(opt)
                                .range(Math.round(rv.getMin()), Math.round(rv.getMax()))
                                .step(1L)
                );
            }

            return option(Long.class, entry, path, manager, staged, LongFieldControllerBuilder::create);
        } else if(raw == double.class || raw == Double.class) {
            if(range.isPresent()) {
                final Validators.RangeValidator rv = range.get();

                return option(
                        Double.class,
                        entry,
                        path,
                        manager,
                        staged,
                        opt -> DoubleSliderControllerBuilder.create(opt)
                                .range(rv.getMin(), rv.getMax())
                                .step(0.01d)
                );
            }

            return option(Double.class, entry, path, manager, staged, DoubleFieldControllerBuilder::create);
        } else if(raw == float.class || raw == Float.class) {
            if(range.isPresent()) {
                final Validators.RangeValidator rv = range.get();

                return option(
                        Float.class,
                        entry,
                        path,
                        manager,
                        staged,
                        opt -> FloatSliderControllerBuilder.create(opt)
                                .range((float)rv.getMin(), (float)rv.getMax())
                                .step(0.01f)
                );
            }

            return option(Float.class, entry, path, manager, staged, FloatFieldControllerBuilder::create);
        }

        // Unknown scalar type — render as disabled placeholder.
        return buildPlaceholder(entry);
    }

    private <E extends Enum<E>> Option<?> buildEnum(
            final SchemaEntry entry,
            final String path,
            final ConfigManager<?> manager,
            final Map<String, Object> staged,
            final Class<E> enumClass
    ) {
        return option(
                enumClass,
                entry,
                path,
                manager,
                staged,
                opt -> EnumControllerBuilder.create(opt)
                        .enumClass(enumClass)
        );
    }

    /**
     * {@code @Widget(COLOR)} on a {@code String} field storing
     * {@code "#RRGGBB"} hex. YACL binds {@link Color}; we bridge string ↔ RGB
     * on read and RGB &lt;-&gt; string on stage. Alpha channel not
     * exposed — spec §7.14 defines the on-disk shape as {@code #RRGGBB} only.
     */
    private Option<?> buildColor(
            final SchemaEntry entry,
            final String path,
            final ConfigManager<?> manager,
            final Map<String, Object> staged
    ) {
        final EntryMetadata meta = entry.metadata();
        final String defString = entry.defaultValue() == null ? "" : entry.defaultValue().toString();
        final int defRgb = parseColor(defString, 0xFFFFFF);
        final Color def = new Color(defRgb);

        return Option.<Color>createBuilder()
                .name(Component.literal(entry.key()))
                .description(description(meta))
                .binding(
                        def,
                        () -> {
                            final Object staged0 = staged.get(path);
                            if(staged0 instanceof final String s) {
                                return new Color(parseColor(s, defRgb));
                            }

                            final Object live = readLive(manager, path);
                            if(live instanceof final String s) {
                                return new Color(parseColor(s, defRgb));
                            }

                            return def;
                        },
                        v -> staged.put(path, formatColor(v.getRGB() & 0xFFFFFF))
                )
                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(false))
                .build();
    }

    private static int parseColor(
            final String hex,
            final int fallback
    ) {
        if(hex == null) {
            return fallback;
        }

        final String s = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch(final NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String formatColor(final int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    private Option<?> buildPlaceholder(final SchemaEntry entry) {
        return Option.<String>createBuilder()
                .name(Component.literal(entry.key() + " (edit on disk)").withStyle(ChatFormatting.GRAY))
                .description(description(entry.metadata()))
                .binding("(complex)", () -> "(complex)", v -> { /* discarded */ })
                .controller(StringControllerBuilder::create)
                .available(false)
                .build();
    }

    /**
     * Build a per-type option binding. Getter reads staged value if present,
     * otherwise the live manager state. Setter stages into {@code staged}.
     */
    @SuppressWarnings("unchecked")
    private <T> Option<T> option(
            final Class<T> typeClass,
            final SchemaEntry entry,
            final String path,
            final ConfigManager<?> manager,
            final Map<String, Object> staged,
            final Function<Option<T>, ? extends ControllerBuilder<T>> controller
    ) {
        final T defaultValue = coerce(entry.defaultValue(), typeClass);
        return Option.<T>createBuilder()
                .name(Component.literal(entry.key()))
                .description(description(entry.metadata()))
                .binding(
                        defaultValue,
                        () -> {
                            final Object staged0 = staged.get(path);
                            if(staged0 != null && typeClass.isInstance(staged0)) {
                                return (T)staged0;
                            }

                            final Object live = readLive(manager, path);
                            if(live != null && typeClass.isInstance(live)) {
                                return (T)live;
                            }

                            return defaultValue;
                        },
                        v -> staged.put(path, v)
                )
                .controller(controller::apply)
                .build();
    }

    // Save flush
    //--------------------------------------------------

    private void flush(
            final ConfigManager<?> manager,
            final Map<String, Object> staged
    ) {
        for(final Map.Entry<String, Object> e : staged.entrySet()) {
            try {
                manager.set(e.getKey(), e.getValue());
            } catch(final RuntimeException ex) {
                LOGGER.warn("[coal-yacl-adapter] failed to stage '{}' = {}: {}", e.getKey(), e.getValue(), ex.getMessage());
            }
        }

        staged.clear();

        try {
            manager.save();
        } catch(final Exception ex) {
            LOGGER.warn("[coal-yacl-adapter] save failed: {}", ex.getMessage());
        }
    }

}
