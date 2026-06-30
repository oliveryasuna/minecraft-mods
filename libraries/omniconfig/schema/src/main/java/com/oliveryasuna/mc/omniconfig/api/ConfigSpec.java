package com.oliveryasuna.mc.omniconfig.api;

import com.oliveryasuna.mc.omniconfig.schema.*;
import com.oliveryasuna.mc.omniconfig.validation.validator.LengthValidator;
import com.oliveryasuna.mc.omniconfig.validation.validator.OneOfValidator;
import com.oliveryasuna.mc.omniconfig.validation.validator.PatternValidator;
import com.oliveryasuna.mc.omniconfig.validation.validator.RangeValidator;
import com.oliveryasuna.mc.omniconfig.value.ValueType;
import com.oliveryasuna.mc.omniconifg.api.Format;
import com.oliveryasuna.mc.omniconifg.api.annotation.Reload;
import com.oliveryasuna.mc.omniconifg.api.annotation.Sync;
import com.oliveryasuna.mc.omniconifg.api.annotation.Widget;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * Programmatic (builder) config definition — the secondary style alongside
 * annotations, for dynamically-shaped configs.
 * <p>
 * Produces a {@link Schema} whose entries are map-backed; register via
 * {@code OmniConfig.register(spec)}.
 *
 * <pre>{@code
 * ConfigSpec spec = ConfigSpec.builder("mymod", "config")
 *     .define("featureEnabled", true).comment("Master toggle.")
 *     .defineInt("maxEntities", 64, 0, 256).reload(Reload.Tier.WORLD)
 *     .push("display")
 *         .defineFloat("opacity", 0.8f, 0.0f, 1.0f).widget(Widget.Type.SLIDER)
 *     .pop()
 *     .build();
 * }</pre>
 * <p>
 * Fluent metadata methods ({@code comment}, {@code reload}, {@code widget},
 * {@code pattern}, ...) apply to the most recently defined entry.
 */
public final class ConfigSpec {

    //==================================================
    // Static methods
    //==================================================

    public static Builder builder(
            final String id,
            final String name
    ) {
        return new Builder(id, name);
    }

    //==================================================
    // Fields
    //==================================================

    private final Schema schema;

    //==================================================
    // Constructors
    //==================================================

    private ConfigSpec(final Schema schema) {
        super();

        this.schema = schema;
    }

    //==================================================
    // Methods
    //==================================================

    public ConfigModel<Map<String, Object>> toModel() {
        return new MapConfigModel(schema);
    }

    //==================================================
    // Getters/setters
    //==================================================

    public Schema getSchema() {
        return schema;
    }

    //==================================================
    // Nested
    //==================================================

    public static final class Builder implements org.apache.commons.lang3.builder.Builder<ConfigSpec> {

        //==================================================
        // Fields
        //==================================================

        private final String id;
        private final String name;
        private Format format;
        private int version;

        private final SchemaCategory.Builder rootBuilder;
        private final Deque<SchemaCategory.Builder> categoryBuilders;
        private final Deque<String> path;
        private Draft current;

        //==================================================
        // Constructors
        //==================================================

        private Builder(
                final String id,
                final String name
        ) {
            super();

            this.id = id;
            this.name = name;
            this.format = Format.TOML;
            this.version = 1;
            this.rootBuilder = SchemaCategory.builder("");
            this.categoryBuilders = new ArrayDeque<>();
            this.path = new ArrayDeque<>();

            this.categoryBuilders.push(this.rootBuilder);
        }

        //==================================================
        // Methods
        //==================================================

        @Override
        public ConfigSpec build() {
            flush();
            final Schema schema = new Schema(Map.class, id, name, format, version, rootBuilder.build());

            SchemaValidator.validate(schema);
            return new ConfigSpec(schema);
        }

        public Builder format(final Format format) {
            this.format = format;
            return this;
        }

        public Builder version(final int version) {
            this.version = version;
            return this;
        }

        // Categories
        //--------------------------------------------------

        public Builder push(final String category) {
            flush();
            categoryBuilders.push(categoryBuilders.peek().child(category));
            path.addLast(category);
            return this;
        }

        public Builder pop() {
            flush();
            categoryBuilders.pop();
            path.removeLast();
            return this;
        }

        // Define
        //--------------------------------------------------

        public Builder define(
                final String key,
                final boolean def
        ) {
            return draft(key, ValueType.scalar(boolean.class), def);
        }

        public Builder defineString(
                final String key,
                final String def
        ) {
            return draft(key, ValueType.scalar(String.class), def);
        }

        public <E extends Enum<E>> Builder defineEnum(
                final String key,
                final E def
        ) {
            return draft(key, ValueType.enumType(def.getDeclaringClass()), def);
        }

        public Builder defineStringList(
                final String key,
                final List<String> def
        ) {
            return draft(key, ValueType.listOf(ValueType.scalar(String.class)), def);
        }

        public Builder defineInt(
                final String key,
                final int def,
                final int min,
                final int max
        ) {
            return draft(key, ValueType.scalar(int.class), def).range(min, max);
        }

        public Builder defineLong(
                final String key,
                final long def,
                final long min,
                final long max
        ) {
            return draft(key, ValueType.scalar(long.class), def).range(min, max);
        }

        public Builder defineFloat(
                final String key,
                final float def,
                final float min,
                final float max
        ) {
            return draft(key, ValueType.scalar(float.class), def).range(min, max);
        }

        public Builder defineDouble(
                final String key,
                final double def,
                final double min,
                final double max
        ) {
            return draft(key, ValueType.scalar(double.class), def).range(min, max);
        }

        // Fluent metadata (apply to current entry)
        //--------------------------------------------------

        public Builder comment(final String... lines) {
            meta().comment(List.of(lines));
            return this;
        }

        public Builder reload(final Reload.Tier tier) {
            meta().reloadTier(tier);
            return this;
        }

        public Builder sync(final Sync.Scope scope) {
            meta().syncScope(scope);
            return this;
        }

        public Builder widget(final Widget.Type widget) {
            meta().widget(widget);
            return this;
        }

        public Builder hidden() {
            meta().hidden(true);
            return this;
        }

        public Builder allowInvalid(final boolean allowInvalid) {
            meta().allowInvalid(allowInvalid);
            return this;
        }

        public Builder pattern(final String regex) {
            meta().addValidator(new PatternValidator(regex));
            return this;
        }

        public Builder oneOf(final String... allowed) {
            meta().addValidator(new OneOfValidator(allowed));
            return this;
        }

        public Builder length(final int min, final int max) {
            meta().addValidator(new LengthValidator(min, max));
            return this;
        }

        private Builder range(final double min, final double max) {
            meta().addValidator(new RangeValidator(min, max));
            return this;
        }


        // Internals
        //--------------------------------------------------

        private Builder draft(
                final String key,
                final ValueType type,
                final Object def
        ) {
            flush();
            current = new Draft(key, type, def);
            return this;
        }

        private EntryMetadata.Builder meta() {
            if(current == null) {
                throw new IllegalStateException("no entry to attach metadata to; call a define* method first");
            }
            return current.meta;
        }

        private void flush() {
            if(current == null) {
                return;
            }

            final String full = path.isEmpty()
                    ? current.key
                    : String.join(".", path) + "." + current.key;
            final SchemaEntry entry = new SchemaEntry(current.key, current.type, current.def, current.meta.build(), new MapAccessor(full));
            categoryBuilders.peek().addEntry(entry);
            current = null;
        }

        //==================================================
        // Nested
        //==================================================

        private static final class Draft {

            //==================================================
            // Fields
            //==================================================

            final String key;
            final ValueType type;
            final Object def;
            final EntryMetadata.Builder meta;

            //==================================================
            // Constructors
            //==================================================

            Draft(
                    final String key,
                    final ValueType type,
                    final Object def
            ) {
                super();

                this.key = key;
                this.type = type;
                this.def = def;
                this.meta = EntryMetadata.builder();
            }

        }

    }

}
