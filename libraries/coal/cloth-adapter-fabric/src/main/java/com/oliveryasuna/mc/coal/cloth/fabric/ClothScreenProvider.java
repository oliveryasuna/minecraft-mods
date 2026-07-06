package com.oliveryasuna.mc.coal.cloth.fabric;

import com.oliveryasuna.mc.coal.adapter.common.AdapterScreenSupport;
import com.oliveryasuna.mc.coal.adapter.common.Validators;
import com.oliveryasuna.mc.coal.api.annotation.Reload;
import com.oliveryasuna.mc.coal.api.annotation.Widget;
import com.oliveryasuna.mc.coal.api.config.ConfigManager;
import com.oliveryasuna.mc.coal.api.gui.ScreenProvider;
import com.oliveryasuna.mc.coal.api.schema.*;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Cloth Config-backed {@link ScreenProvider}. Priority {@code 40} — sits
 * below the YACL adapter's {@code 50} so YACL wins when both are installed;
 * users who want Cloth-primary uninstall YACL.
 * <p>
 * Cloth's {@code startSubCategory} recurses cleanly, so we preserve the full
 * {@code SchemaCategory} hierarchy — unlike the YACL adapter which flattens
 * deep-nested categories per branch.
 * <p>
 * Staged edits: each entry's {@code setSaveConsumer} writes into a per-screen
 * {@code staged} map; Cloth's saving runnable flushes them via
 * {@link ConfigManager#set} + {@link ConfigManager#save}.
 */
public final class ClothScreenProvider implements ScreenProvider {

    //==================================================
    // Static fields
    //==================================================

    private static final Logger LOGGER = LoggerFactory.getLogger("coal-cloth-adapter");

    //==================================================
    // Static methods
    //==================================================

    private static Component[] tooltip(final EntryMetadata meta) {
        if(meta.comment().isEmpty()) return new Component[0];
        return meta.comment().stream()
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
    // ScreenProvider
    //==================================================

    @Override
    public String id() {
        return "coal-cloth-adapter";
    }

    @Override
    public int priority() {
        return 40;
    }

    @Override
    public Screen create(
            final Minecraft client,
            final Screen parent,
            final ConfigManager<?> manager
    ) {
        final Schema schema = manager.schema();
        final Map<String, Object> staged = new LinkedHashMap<>();

        final ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal(schema.name()))
                .setSavingRunnable(() -> flush(manager, staged));

        final ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        final SchemaCategory root = schema.root();
        if(!root.entries().isEmpty()) {
            final ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
            for(final SchemaEntry entry : root.entries()) {
                addEntry(general, entryBuilder, entry, entry.key(), manager, staged);
            }
        }
        for(final SchemaCategory child : root.categories()) {
            final ConfigCategory cat = builder.getOrCreateCategory(Component.literal(child.name()));
            for(final SchemaEntry entry : child.entries()) {
                addEntry(cat, entryBuilder, entry, child.name() + "." + entry.key(), manager, staged);
            }
            for(final SchemaCategory grandchild : child.categories()) {
                cat.addEntry(buildSubCategory(entryBuilder, grandchild, child.name() + "." + grandchild.name(), manager, staged));
            }
        }

        return builder.build();
    }

    //==================================================
    // Sub-category recursion
    //==================================================

    @SuppressWarnings("rawtypes")
    private AbstractConfigListEntry<?> buildSubCategory(
            final ConfigEntryBuilder entryBuilder,
            final SchemaCategory cat,
            final String pathPrefix,
            final ConfigManager<?> manager,
            final Map<String, Object> staged
    ) {
        final List<AbstractConfigListEntry> children = new ArrayList<>();
        for(final SchemaEntry entry : cat.entries()) {
            if(entry.metadata().isHidden()) {
                continue;
            }

            final AbstractConfigListEntry<?> built = buildEntry(entryBuilder, entry, pathPrefix + "." + entry.key(), manager, staged);
            if(built != null) {
                children.add(built);
            }
        }
        for(final SchemaCategory grandchild : cat.categories()) {
            children.add(buildSubCategory(entryBuilder, grandchild, pathPrefix + "." + grandchild.name(), manager, staged));
        }

        return entryBuilder.startSubCategory(Component.literal(cat.name()), children)
                .setExpanded(false)
                .build();
    }

    private void addEntry(
            final ConfigCategory category,
            final ConfigEntryBuilder entryBuilder,
            final SchemaEntry entry,
            final String path,
            final ConfigManager<?> manager,
            final Map<String, Object> staged
    ) {
        if(entry.metadata().isHidden()) {
            return;
        }

        final AbstractConfigListEntry<?> built = buildEntry(entryBuilder, entry, path, manager, staged);
        if(built != null) {
            category.addEntry(built);
        }
    }

    //==================================================
    // Entry builders
    //==================================================

    @SuppressWarnings({"unchecked", "rawtypes"})
    private AbstractConfigListEntry<?> buildEntry(
            final ConfigEntryBuilder entryBuilder,
            final SchemaEntry entry,
            final String path,
            final ConfigManager<?> manager,
            final Map<String, Object> staged
    ) {
        final ValueType type = entry.type();
        final Class<?> raw = type.rawType();

        return switch(type.kind()) {
            case SCALAR -> buildScalar(entryBuilder, entry, path, manager, staged, raw);
            case ENUM -> buildEnum(entryBuilder, entry, path, manager, staged, (Class)raw);
            case LIST, MAP, OBJECT -> buildPlaceholder(entryBuilder, entry);
        };
    }

    private AbstractConfigListEntry<?> buildScalar(
            final ConfigEntryBuilder eb,
            final SchemaEntry entry,
            final String path,
            final ConfigManager<?> manager,
            final Map<String, Object> staged,
            final Class<?> raw
    ) {
        final EntryMetadata meta = entry.metadata();
        final Component name = Component.literal(entry.key());
        final Component[] tip = tooltip(meta);

        if(raw == boolean.class || raw == Boolean.class) {
            final boolean def = (Boolean)(entry.defaultValue() == null ? Boolean.FALSE : entry.defaultValue());
            return restartIfNeeded(eb.startBooleanToggle(name, currentOrDefault(path, manager, staged, def))
                    .setDefaultValue(def)
                    .setTooltip(tip)
                    .setSaveConsumer(v -> staged.put(path, v)), meta, BooleanToggleBuilder::requireRestart)
                    .build();
        } else if(raw == String.class) {
            final Optional<Validators.OneOfValidator> oneOf = AdapterScreenSupport.findValidator(meta, Validators.OneOfValidator.class);
            final String def = (String)(entry.defaultValue() == null ? "" : entry.defaultValue());
            if(oneOf.isPresent()) {
                final List<String> allowed = new ArrayList<>(oneOf.get().getAllowed());

                return restartIfNeeded(eb.startStringDropdownMenu(name, currentOrDefault(path, manager, staged, def))
                        .setSelections(allowed)
                        .setSuggestionMode(false)
                        .setDefaultValue(def)
                        .setTooltip(tip)
                        .setSaveConsumer(v -> staged.put(path, v)), meta, DropdownMenuBuilder::requireRestart)
                        .build();
            }
            if(meta.widget() == Widget.Type.COLOR) {
                final int defRgb = AdapterScreenSupport.parseColor(def, 0xFFFFFF);
                final int current = AdapterScreenSupport.parseColor(currentOrDefault(path, manager, staged, def), defRgb);

                return restartIfNeeded(eb.startColorField(name, current)
                        .setDefaultValue(defRgb)
                        .setTooltip(tip)
                        .setSaveConsumer(rgb -> staged.put(path, AdapterScreenSupport.formatColor(rgb & 0xFFFFFF))), meta, ColorFieldBuilder::requireRestart)
                        .build();
            }

            return restartIfNeeded(eb.startStrField(name, currentOrDefault(path, manager, staged, def))
                    .setDefaultValue(def)
                    .setTooltip(tip)
                    .setSaveConsumer(v -> staged.put(path, v)), meta, StringFieldBuilder::requireRestart)
                    .build();
        } else if(raw == int.class || raw == Integer.class) {
            final int def = (Integer)(entry.defaultValue() == null ? 0 : entry.defaultValue());
            final Optional<Validators.RangeValidator> r = AdapterScreenSupport.findValidator(meta, Validators.RangeValidator.class);
            if(r.isPresent()) {
                final Validators.RangeValidator rv = r.get();

                return restartIfNeeded(eb.startIntSlider(name, currentOrDefault(path, manager, staged, def), (int)Math.round(rv.getMin()), (int)Math.round(rv.getMax()))
                        .setDefaultValue(def)
                        .setTooltip(tip)
                        .setSaveConsumer(v -> staged.put(path, v)), meta, IntSliderBuilder::requireRestart)
                        .build();
            }

            final IntFieldBuilder b = eb.startIntField(name, currentOrDefault(path, manager, staged, def))
                    .setDefaultValue(def)
                    .setTooltip(tip);

            return restartIfNeeded(b.setSaveConsumer(v -> staged.put(path, v)), meta, IntFieldBuilder::requireRestart).build();
        } else if(raw == long.class || raw == Long.class) {
            final long def = (Long)(entry.defaultValue() == null ? 0L : entry.defaultValue());
            final Optional<Validators.RangeValidator> r = AdapterScreenSupport.findValidator(meta, Validators.RangeValidator.class);
            if(r.isPresent()) {
                final Validators.RangeValidator rv = r.get();

                return restartIfNeeded(eb.startLongSlider(name, currentOrDefault(path, manager, staged, def), Math.round(rv.getMin()), Math.round(rv.getMax()))
                        .setDefaultValue(def)
                        .setTooltip(tip)
                        .setSaveConsumer(v -> staged.put(path, v)), meta, LongSliderBuilder::requireRestart)
                        .build();
            }

            final LongFieldBuilder b = eb.startLongField(name, currentOrDefault(path, manager, staged, def))
                    .setDefaultValue(def)
                    .setTooltip(tip);

            return restartIfNeeded(b.setSaveConsumer(v -> staged.put(path, v)), meta, LongFieldBuilder::requireRestart).build();
        } else if(raw == double.class || raw == Double.class) {
            final double def = (Double)(entry.defaultValue() == null ? 0d : entry.defaultValue());
            final Optional<Validators.RangeValidator> r = AdapterScreenSupport.findValidator(meta, Validators.RangeValidator.class);
            final DoubleFieldBuilder b = eb.startDoubleField(name, currentOrDefault(path, manager, staged, def))
                    .setDefaultValue(def)
                    .setTooltip(tip);
            r.ifPresent(rv -> {
                b.setMin(rv.getMin());
                b.setMax(rv.getMax());
            });

            return restartIfNeeded(b.setSaveConsumer(v -> staged.put(path, v)), meta, DoubleFieldBuilder::requireRestart).build();
        } else if(raw == float.class || raw == Float.class) {
            final float def = (Float)(entry.defaultValue() == null ? 0f : entry.defaultValue());
            final Optional<Validators.RangeValidator> r = AdapterScreenSupport.findValidator(meta, Validators.RangeValidator.class);
            final FloatFieldBuilder b = eb.startFloatField(name, currentOrDefault(path, manager, staged, def))
                    .setDefaultValue(def)
                    .setTooltip(tip);
            r.ifPresent(rv -> {
                b.setMin((float)rv.getMin());
                b.setMax((float)rv.getMax());
            });

            return restartIfNeeded(b.setSaveConsumer(v -> staged.put(path, v)), meta, FloatFieldBuilder::requireRestart).build();
        }

        // Unknown scalar type — render as disabled placeholder.
        return buildPlaceholder(eb, entry);
    }

    private <E extends Enum<E>> AbstractConfigListEntry<?> buildEnum(
            final ConfigEntryBuilder eb,
            final SchemaEntry entry,
            final String path,
            final ConfigManager<?> manager,
            final Map<String, Object> staged,
            final Class<E> enumClass
    ) {
        final EntryMetadata meta = entry.metadata();
        @SuppressWarnings("unchecked") final E def = (E)(entry.defaultValue() == null
                ? enumClass.getEnumConstants()[0]
                : entry.defaultValue());

        return restartIfNeeded(eb.startEnumSelector(Component.literal(entry.key()), enumClass, currentOrDefault(path, manager, staged, def))
                .setDefaultValue(def)
                .setTooltip(tooltip(meta))
                .setSaveConsumer(v -> staged.put(path, v)), meta, EnumSelectorBuilder::requireRestart)
                .build();
    }

    private AbstractConfigListEntry<?> buildPlaceholder(
            final ConfigEntryBuilder eb,
            final SchemaEntry entry
    ) {
        final EntryMetadata meta = entry.metadata();

        return eb.startStrField(
                        Component.literal(entry.key() + " (edit on disk)").withStyle(ChatFormatting.GRAY),
                        "(complex)")
                .setDefaultValue("(complex)")
                .setTooltip(tooltip(meta))
                .setSaveConsumer(v -> { /* discarded */ })
                .build();
    }

    //==================================================
    // Save flush
    //==================================================

    private void flush(
            final ConfigManager<?> manager,
            final Map<String, Object> staged
    ) {
        for(final Map.Entry<String, Object> e : staged.entrySet()) {
            try {
                manager.set(e.getKey(), e.getValue());
            } catch(final RuntimeException ex) {
                LOGGER.warn("[coal-cloth-adapter] failed to stage '{}' = {}: {}", e.getKey(), e.getValue(), ex.getMessage());
            }
        }

        staged.clear();

        try {
            manager.save();
        } catch(final Exception ex) {
            LOGGER.warn("[coal-cloth-adapter] save failed: {}", ex.getMessage());
        }
    }

    //==================================================
    // Helpers
    //==================================================

    /**
     * Read the current value at {@code path}: staged edits shadow live manager
     * state; live manager state shadows the entry's default. Never returns
     * {@code null}.
     */
    @SuppressWarnings("unchecked")
    private static <T> T currentOrDefault(
            final String path,
            final ConfigManager<?> manager,
            final Map<String, Object> staged,
            final T defaultValue
    ) {
        final Object staged0 = staged.get(path);
        if(staged0 != null && defaultValue != null && defaultValue.getClass().isInstance(staged0)) {
            return (T)staged0;
        }

        final Object live = AdapterScreenSupport.readLive(manager, path);
        if(live != null && defaultValue != null && defaultValue.getClass().isInstance(live)) {
            return (T)live;
        }

        return defaultValue;
    }

    /**
     * Apply {@code @Reload(RESTART)} / {@code @RequiresRestart} to any Cloth
     * builder that exposes {@code requireRestart()}. The caller supplies a
     * lambda so we don't need a common supertype — Cloth's
     * {@code AbstractFieldBuilder} and {@code DropdownMenuBuilder} both have
     * the method but no shared interface exposes it. Keeps the fluent chain
     * intact by returning the builder.
     */
    private static <B> B restartIfNeeded(
            final B builder,
            final EntryMetadata meta,
            final java.util.function.Consumer<B> requireRestart
    ) {
        if(meta.reloadTier() == Reload.Tier.RESTART) {
            requireRestart.accept(builder);
        }

        return builder;
    }

}
