package com.oliveryasuna.mc.omniconfig.schema;

import java.util.*;

/**
 * A named, ordered grouping of entries and sub-categories.
 */
public final class SchemaCategory {

    //==================================================
    // Static methods
    //==================================================

    public static Builder builder(final String name) {
        return new Builder(name);
    }

    //==================================================
    // Fields
    //==================================================

    private final String name;
    private final List<String> comment;
    private final Map<String, SchemaEntry> entries;
    private final Map<String, SchemaCategory> categories;

    //==================================================
    // Constructors
    //==================================================

    private SchemaCategory(
            final String name,
            final List<String> comment,
            final Map<String, SchemaEntry> entries,
            final Map<String, SchemaCategory> categories
    ) {
        super();

        this.name = name;
        this.comment = List.copyOf(comment);
        this.entries = Collections.unmodifiableMap(new LinkedHashMap<>(entries));
        this.categories = Collections.unmodifiableMap(new LinkedHashMap<>(categories));
    }

    //==================================================
    // Methods
    //==================================================

    public Collection<SchemaEntry> entries() {
        return entries.values();
    }

    public Collection<SchemaCategory> categories() {
        return categories.values();
    }

    public Optional<SchemaEntry> entry(final String key) {
        return Optional.ofNullable(entries.get(key));
    }

    public Optional<SchemaCategory> category(final String name) {
        return Optional.ofNullable(categories.get(name));
    }

    //==================================================
    // Getters/setters
    //==================================================

    public String getName() {
        return name;
    }

    public List<String> getComment() {
        return comment;
    }

    public Map<String, SchemaEntry> getEntries() {
        return entries;
    }

    public Map<String, SchemaCategory> getCategories() {
        return categories;
    }

    //==================================================
    // Nested
    //==================================================

    /**
     * Builder holding child builders so flat and nested categories merge
     * cleanly.
     */
    public static final class Builder implements org.apache.commons.lang3.builder.Builder<SchemaCategory> {

        //==================================================
        // Fields
        //==================================================

        private final String name;
        private List<String> comment;
        private final Map<String, SchemaEntry> entries;
        private final Map<String, Builder> children;

        //==================================================
        // Constructors
        //==================================================

        private Builder(final String name) {
            super();

            this.name = name;
            this.comment = List.of();
            this.entries = new LinkedHashMap<>();
            this.children = new LinkedHashMap<>();
        }

        //==================================================
        // Methods
        //==================================================

        @Override
        public SchemaCategory build() {
            final Map<String, SchemaCategory> built = new LinkedHashMap<>();
            for(final Map.Entry<String, Builder> e : children.entrySet()) {
                built.put(e.getKey(), e.getValue().build());
            }

            return new SchemaCategory(name, comment, entries, built);
        }

        public Builder comment(final List<String> comment) {
            this.comment = List.copyOf(comment);
            return this;
        }

        public Builder addEntry(final SchemaEntry entry) {
            if(entries.containsKey(entry.getKey())) {
                throw new IllegalArgumentException("Duplicate key '" + entry.getKey() + "' in category '" + name + "'");
            }

            entries.put(entry.getKey(), entry);

            return this;
        }

        /**
         * Returns the existing or newly-created child builder for
         * {@code childName}.
         *
         * @param childName The name of the child to get or create.
         * @return The child builder.
         */
        public Builder child(final String childName) {
            return children.computeIfAbsent(childName, Builder::new);
        }

    }

}
