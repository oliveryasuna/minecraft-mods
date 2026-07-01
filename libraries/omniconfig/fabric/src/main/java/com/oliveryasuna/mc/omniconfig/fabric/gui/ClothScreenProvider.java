package com.oliveryasuna.mc.omniconfig.fabric.gui;

import com.oliveryasuna.mc.omniconfig.api.ConfigManager;
import com.oliveryasuna.mc.omniconfig.api.annotation.Widget;
import com.oliveryasuna.mc.omniconfig.schema.EntryMetadata;
import com.oliveryasuna.mc.omniconfig.schema.Schema;
import com.oliveryasuna.mc.omniconfig.schema.SchemaCategory;
import com.oliveryasuna.mc.omniconfig.schema.SchemaEntry;
import com.oliveryasuna.mc.omniconfig.validation.validator.OneOfValidator;
import com.oliveryasuna.mc.omniconfig.validation.validator.RangeValidator;
import com.oliveryasuna.mc.omniconfig.value.ValueType;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

public final class ClothScreenProvider implements ScreenProvider {

    //==================================================
    // Static methods
    //==================================================

    private static Component[] tooltip(final EntryMetadata meta) {
        if(meta.getComment().isEmpty()) {
            return new Component[0];
        }
        return meta.getComment().stream()
                .map(Component::literal)
                .map(c -> (Component)c)
                .toArray(Component[]::new);
    }

    //==================================================
    // Constructors
    //==================================================

    public ClothScreenProvider() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public String id() {
        return "cloth";
    }

    @Override
    public Screen create(
            final Minecraft client,
            final Screen parent,
            final ConfigManager<?> manager
    ) {
        final Schema schema = manager.getSchema();
        // Staged values: identical model to YaclScreenProvider — Cloth fires
        // saveConsumer per entry on Save & Quit. Flush all at once via
        // manager.set + manager.save.
        final Map<String, Object> staged = new LinkedHashMap<>();

        final ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal(schema.name()))
                .setSavingRunnable(() -> {
                    try {
                        for(final Map.Entry<String, Object> e : staged.entrySet()) {
                            manager.set(e.getKey(), e.getValue());
                        }
                        manager.save();
                    } catch(final IOException io) {
                        throw new UncheckedIOException(io);
                    }
                });

        final ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        final SchemaCategory root = schema.root();
        if(!root.entries().isEmpty()) {
            // Top-level entries → "General" category, mirroring YACL.
            final ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
            for(final SchemaEntry entry : root.entries()) {
                addEntry(general, entryBuilder, entry, entry.getKey(), staged, manager);
            }
        }
        for(final SchemaCategory child : root.categories()) {
            final ConfigCategory cat = builder.getOrCreateCategory(Component.literal(child.getName()));
            for(final SchemaEntry entry : child.entries()) {
                addEntry(cat, entryBuilder, entry, child.getName() + "." + entry.getKey(), staged, manager);
            }
            // Unlike YACL, Cloth's SubCategoryListEntry recurses cleanly, so
            // deeper SchemaCategory levels become nested sub-categories
            // (preserves the source hierarchy).
            for(final SchemaCategory grandchild : child.categories()) {
                cat.addEntry(buildSubCategory(entryBuilder, grandchild, child.getName() + "." + grandchild.getName(), staged, manager));
            }
        }

        return builder.build();
    }

    // startSubCategory takes a raw-type list because the elements have
    // heterogeneous T parameters — suppress here, not project-wide.
    @SuppressWarnings("rawtypes")
    private AbstractConfigListEntry<?> buildSubCategory(
            final ConfigEntryBuilder entryBuilder,
            final SchemaCategory cat,
            final String pathPrefix,
            final Map<String, Object> staged,
            final ConfigManager<?> manager
    ) {
        final List<AbstractConfigListEntry> children = new ArrayList<>();
        for(final SchemaEntry entry : cat.entries()) {
            if(entry.getMetadata().isHidden()) {
                continue;
            }
            final AbstractConfigListEntry<?> built = buildEntry(entryBuilder, entry, pathPrefix + "." + entry.getKey(), staged, manager);
            if(built != null) {
                children.add(built);
            }
        }
        for(final SchemaCategory grandchild : cat.categories()) {
            children.add(buildSubCategory(entryBuilder, grandchild, pathPrefix + "." + grandchild.getName(), staged, manager));
        }
        return entryBuilder.startSubCategory(Component.literal(cat.getName()), children)
                .setExpanded(false)
                .build();
    }

    private void addEntry(
            final ConfigCategory category,
            final ConfigEntryBuilder entryBuilder,
            final SchemaEntry entry,
            final String path,
            final Map<String, Object> staged,
            final ConfigManager<?> manager
    ) {
        if(entry.getMetadata().isHidden()) {
            return;
        }
        final AbstractConfigListEntry<?> built = buildEntry(entryBuilder, entry, path, staged, manager);
        if(built != null) {
            category.addEntry(built);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private AbstractConfigListEntry<?> buildEntry(
            final ConfigEntryBuilder entryBuilder,
            final SchemaEntry entry,
            final String path,
            final Map<String, Object> staged,
            final ConfigManager<?> manager
    ) {
        final ValueType type = entry.getType();
        final Class<?> raw = type.getRawType();

        return switch(type.getKind()) {
            case SCALAR -> buildScalar(entryBuilder, entry, path, staged, manager, raw);
            case ENUM -> buildEnum(entryBuilder, entry, path, staged, manager, (Class)raw);
            case LIST, MAP, OBJECT -> buildPlaceholder(entryBuilder, entry, path, staged, manager);
        };
    }

    @SuppressWarnings("unchecked")
    private <T> T currentOrDefault(
            final SchemaEntry entry,
            final String path,
            final Map<String, Object> staged,
            final ConfigManager<?> manager,
            final T fallback
    ) {
        if(staged.containsKey(path)) {
            return (T)staged.get(path);
        }
        final Object live = entry.readFrom(manager.get());
        return live == null ? fallback : (T)live;
    }

    private AbstractConfigListEntry<?> buildScalar(
            final ConfigEntryBuilder eb,
            final SchemaEntry entry,
            final String path,
            final Map<String, Object> staged,
            final ConfigManager<?> manager,
            final Class<?> raw
    ) {
        final EntryMetadata meta = entry.getMetadata();
        final Component name = ScreenProviders.displayName(entry, meta);
        final Component[] tip = tooltip(meta);

        if(raw == boolean.class || raw == Boolean.class) {
            final boolean def = (Boolean)(entry.getDefaultValue() == null ? Boolean.FALSE : entry.getDefaultValue());
            return eb.startBooleanToggle(name, currentOrDefault(entry, path, staged, manager, def))
                    .setDefaultValue(def)
                    .setTooltip(tip)
                    .setSaveConsumer(v -> staged.put(path, v))
                    .build();
        }
        if(raw == String.class) {
            final Optional<OneOfValidator> oneOf = ScreenProviders.findOneOf(meta);
            final String def = (String)(entry.getDefaultValue() == null ? "" : entry.getDefaultValue());
            if(oneOf.isPresent()) {
                final List<String> allowed = new ArrayList<>(oneOf.get().allowed());
                return eb.startStringDropdownMenu(name, currentOrDefault(entry, path, staged, manager, def))
                        .setSelections(allowed)
                        // Pure dropdown — disallow typing arbitrary values.
                        .setSuggestionMode(false)
                        .setDefaultValue(def)
                        .setTooltip(tip)
                        .setSaveConsumer(v -> staged.put(path, v))
                        .build();
            }
            if(meta.getWidget() == Widget.Type.COLOR) {
                final int defRgb = ScreenProviders.parseColor(def, 0xFFFFFF);
                final int current = ScreenProviders.parseColor(currentOrDefault(entry, path, staged, manager, def), defRgb);
                return eb.startColorField(name, current)
                        .setDefaultValue(defRgb)
                        .setTooltip(tip)
                        .setSaveConsumer(rgb -> staged.put(path, ScreenProviders.formatColor(rgb)))
                        .build();
            }
            return eb.startStrField(name, currentOrDefault(entry, path, staged, manager, def))
                    .setDefaultValue(def)
                    .setTooltip(tip)
                    .setSaveConsumer(v -> staged.put(path, v))
                    .build();
        }
        if(raw == int.class || raw == Integer.class) {
            final int def = (Integer)(entry.getDefaultValue() == null ? 0 : entry.getDefaultValue());
            final Optional<RangeValidator> r = ScreenProviders.findRange(meta);
            if(ScreenProviders.useSlider(meta, r)) {
                final RangeValidator rv = r.get();
                return eb.startIntSlider(name, currentOrDefault(entry, path, staged, manager, def), (int)Math.round(rv.min()), (int)Math.round(rv.max()))
                        .setDefaultValue(def)
                        .setTooltip(tip)
                        .setSaveConsumer(v -> staged.put(path, v))
                        .build();
            }
            final var b = eb.startIntField(name, currentOrDefault(entry, path, staged, manager, def))
                    .setDefaultValue(def)
                    .setTooltip(tip);
            r.ifPresent(rv -> {
                b.setMin((int)Math.round(rv.min()));
                b.setMax((int)Math.round(rv.max()));
            });
            return b.setSaveConsumer(v -> staged.put(path, v)).build();
        }
        if(raw == long.class || raw == Long.class) {
            final long def = (Long)(entry.getDefaultValue() == null ? 0L : entry.getDefaultValue());
            final Optional<RangeValidator> r = ScreenProviders.findRange(meta);
            if(ScreenProviders.useSlider(meta, r)) {
                final RangeValidator rv = r.get();
                return eb.startLongSlider(name, currentOrDefault(entry, path, staged, manager, def), Math.round(rv.min()), Math.round(rv.max()))
                        .setDefaultValue(def)
                        .setTooltip(tip)
                        .setSaveConsumer(v -> staged.put(path, v))
                        .build();
            }
            final var b = eb.startLongField(name, currentOrDefault(entry, path, staged, manager, def))
                    .setDefaultValue(def)
                    .setTooltip(tip);
            r.ifPresent(rv -> {
                b.setMin(Math.round(rv.min()));
                b.setMax(Math.round(rv.max()));
            });
            return b.setSaveConsumer(v -> staged.put(path, v)).build();
        }
        if(raw == double.class || raw == Double.class) {
            final double def = (Double)(entry.getDefaultValue() == null ? 0d : entry.getDefaultValue());
            final Optional<RangeValidator> r = ScreenProviders.findRange(meta);
            final var b = eb.startDoubleField(name, currentOrDefault(entry, path, staged, manager, def))
                    .setDefaultValue(def)
                    .setTooltip(tip);
            r.ifPresent(rv -> {
                b.setMin(rv.min());
                b.setMax(rv.max());
            });
            return b.setSaveConsumer(v -> staged.put(path, v)).build();
        }
        if(raw == float.class || raw == Float.class) {
            final float def = (Float)(entry.getDefaultValue() == null ? 0f : entry.getDefaultValue());
            final Optional<RangeValidator> r = ScreenProviders.findRange(meta);
            final var b = eb.startFloatField(name, currentOrDefault(entry, path, staged, manager, def))
                    .setDefaultValue(def)
                    .setTooltip(tip);
            r.ifPresent(rv -> {
                b.setMin((float)rv.min());
                b.setMax((float)rv.max());
            });
            return b.setSaveConsumer(v -> staged.put(path, v)).build();
        }
        // Codec-mediated leaves (UUID/Instant/Duration/ResourceLocation/...)
        // — same caveat as YaclScreenProvider: round-tripping through
        // toString means manager.set may throw at flush time. Tracked in
        // YACL_GAPS#A4.
        final String def = entry.getDefaultValue() == null ? "" : entry.getDefaultValue().toString();
        return eb.startStrField(name, String.valueOf(currentOrDefault(entry, path, staged, manager, def)))
                .setDefaultValue(def)
                .setTooltip(tip)
                .setSaveConsumer(v -> staged.put(path, v))
                .build();
    }

    private <E extends Enum<E>> AbstractConfigListEntry<?> buildEnum(
            final ConfigEntryBuilder eb,
            final SchemaEntry entry,
            final String path,
            final Map<String, Object> staged,
            final ConfigManager<?> manager,
            final Class<E> enumClass
    ) {
        final EntryMetadata meta = entry.getMetadata();
        @SuppressWarnings("unchecked") final E def = (E)(entry.getDefaultValue() == null ? enumClass.getEnumConstants()[0] : entry.getDefaultValue());
        return eb.startEnumSelector(ScreenProviders.displayName(entry, meta), enumClass, currentOrDefault(entry, path, staged, manager, def))
                .setDefaultValue(def)
                .setTooltip(tooltip(meta))
                .setSaveConsumer(v -> staged.put(path, v))
                .build();
    }

    private AbstractConfigListEntry<?> buildPlaceholder(
            final ConfigEntryBuilder eb,
            final SchemaEntry entry,
            final String path,
            final Map<String, Object> staged,
            final ConfigManager<?> manager
    ) {
        // Mirror YaclScreenProvider: list/map/object render as a disabled
        // text field labelled "(edit on disk)". Cloth has NestedListListEntry
        // available; wiring it up is YACL_GAPS#A2 follow-up work.
        final EntryMetadata meta = entry.getMetadata();
        return eb.startStrField(
                        Component.literal(entry.getKey() + " (edit on disk)").withStyle(ChatFormatting.GRAY),
                        "(complex)")
                .setDefaultValue("(complex)")
                .setTooltip(tooltip(meta))
                .setSaveConsumer(v -> { /* discarded */ })
                .build();
    }

}
