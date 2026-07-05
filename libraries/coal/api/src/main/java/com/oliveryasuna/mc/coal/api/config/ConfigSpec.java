package com.oliveryasuna.mc.coal.api.config;

import com.oliveryasuna.mc.coal.api.Format;
import com.oliveryasuna.mc.coal.api.schema.EntryMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ConfigSpec {

    //==================================================
    // Fields
    //==================================================

    private final String id;
    private final String name;
    private final Format format;
    private final int version;
    private final List<EntrySpec> entries;

    //==================================================
    // Constructors
    //==================================================

    public ConfigSpec(
            final String id,
            final String name,
            final Format format,
            final int version,
            final List<EntrySpec> entries
    ) {
        super();

        this.id = id;
        this.name = name;
        this.format = format;
        this.version = version;
        this.entries = List.copyOf(entries);
    }

    //==================================================
    // Getters/setters
    //==================================================

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Format getFormat() {
        return format;
    }

    public int getVersion() {
        return version;
    }

    public List<EntrySpec> getEntries() {
        return entries;
    }

    //==================================================
    // Nested
    //==================================================

    /**
     * One declared entry.
     * <p>
     * {@code categoryPath} is a dotted path locating the entry's owning
     * category — empty string means the root category. Providers reconstruct a
     * {@code SchemaCategory} tree from the flat entry list at registration.
     */
    public record EntrySpec(
            String key,
            String categoryPath,
            Class<?> type,
            Object defaultValue,
            EntryMetadata metadata
    ) {

    }

    public static final class Builder {

        //==================================================
        // Fields
        //==================================================

        private final String id;
        private String name;
        private Format format;
        private int version;
        private final List<EntrySpec> entries;
        private final String categoryPath;

        //==================================================
        // Constructors
        //==================================================

        // Package-private — used by category(...) to inherit state.
        Builder(
                final String id,
                final String categoryPath,
                final List<EntrySpec> entries
        ) {
            super();

            this.id = id;
            this.format = Format.TOML;
            this.version = 1;
            this.categoryPath = categoryPath;
            this.entries = entries;
        }

        public Builder(final String id) {
            this(id, "", new ArrayList<>());
        }

        //==================================================
        // Methods
        //==================================================

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder format(final Format format) {
            this.format = format;
            return this;
        }

        public Builder format(final String formatId) {
            this.format = Format.of(formatId);
            return this;
        }

        public Builder version(final int version) {
            this.version = version;
            return this;
        }

        public <T> Builder entry(
                final String key,
                final Class<T> type,
                final T defaultValue
        ) {
            entries.add(new EntrySpec(key, categoryPath, type, defaultValue, EntryMetadata.builder().build()));
            return this;
        }

        public <T> Builder entry(
                final String key,
                final Class<T> type,
                final T defaultValue,
                final Consumer<EntryMetadata.Builder> meta
        ) {
            final EntryMetadata.Builder builder = EntryMetadata.builder();
            meta.accept(builder);
            entries.add(new EntrySpec(key, categoryPath, type, defaultValue, builder.build()));
            return this;
        }

        /**
         * Scope subsequent entries added inside {@code category} under a dotted
         * category path.
         * <p>
         * Nested calls further extend the path (child category "b" inside
         * parent "a" resolves to {@code "a.b"}). Entries added outside a
         * {@code category(...)} block sit at the root.
         */
        public Builder category(
                final String name,
                final Consumer<Builder> category
        ) {
            final String childPath = categoryPath.isEmpty() ? name : (categoryPath + "." + name);
            final Builder child = new Builder(id, childPath, entries);
            child.name = this.name;
            child.format = this.format;
            child.version = this.version;
            category.accept(child);
            return this;
        }

        public ConfigSpec build() {
            return new ConfigSpec(id, name, format, version, entries);
        }

    }

}
