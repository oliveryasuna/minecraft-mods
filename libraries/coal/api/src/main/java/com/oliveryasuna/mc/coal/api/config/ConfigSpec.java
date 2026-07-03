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

    public record EntrySpec(
            String key,
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

        //==================================================
        // Constructors
        //==================================================

        public Builder(final String id) {
            super();

            this.id = id;
            this.entries = new ArrayList<>();
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
            // TODO: Implement.
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
            // TODO: Implement.
        }

        public <T> Builder entry(
                final String key,
                final Class<T> type,
                final T defaultValue,
                final Consumer<EntryMetadata.Builder> meta
        ) {
            // TODO: Implement.
        }

        public Builder category(
                final String name,
                final Consumer<Builder> category
        ) {
            // TODO: Implement.
        }

        public ConfigSpec build() {
            return new ConfigSpec(id, name, format, version, entries);
        }

    }

}
