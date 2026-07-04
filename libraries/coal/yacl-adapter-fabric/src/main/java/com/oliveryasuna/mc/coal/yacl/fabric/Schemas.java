package com.oliveryasuna.mc.coal.yacl.fabric;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.coal.api.Format;
import com.oliveryasuna.mc.coal.api.schema.*;

import java.util.*;

/**
 * Concrete implementations of {@link Schema} / {@link SchemaCategory} /
 * {@link SchemaEntry}. Constructed by {@link AnnotationSchemaReader} and by the
 * {@code ConfigSpec} path in {@link YaclConfigProvider}.
 */
final class Schemas {

    //==================================================
    // Static methods
    //==================================================

    // Helpers
    //--------------------------------------------------

    /**
     * Assemble a {@link SchemaCategory} from a mutable-list-based intermediate.
     */
    static SchemaCategory buildCategory(
            final String name,
            final List<String> comment,
            final List<SchemaEntry> entries,
            final List<SchemaCategory> categories
    ) {
        return new SchemaCategoryImpl(name, comment, entries, categories);
    }

    static List<SchemaEntry> mutableEntries() {
        return new ArrayList<>();
    }

    static List<SchemaCategory> mutableCategories() {
        return new ArrayList<>();
    }

    //==================================================
    // Constructors
    //==================================================

    private Schemas() {
        super();

        throw new UnsupportedInstantiationException();
    }

    //==================================================
    // Nested
    //==================================================

    static final class SchemaEntryImpl implements SchemaEntry {

        //==================================================
        // Fields
        //==================================================

        private final String key;
        private final ValueType type;
        private final Object defaultValue;
        private final EntryMetadata metadata;
        private final ValueAccessor accessor;

        //==================================================
        // Constructors
        //==================================================

        SchemaEntryImpl(
                final String key,
                final ValueType type,
                final Object defaultValue,
                final EntryMetadata metadata,
                final ValueAccessor accessor
        ) {
            super();

            this.key = key;
            this.type = type;
            this.defaultValue = defaultValue;
            this.metadata = metadata;
            this.accessor = accessor;
        }

        //==================================================
        // Methods
        //==================================================

        @Override
        public String key() {
            return key;
        }

        @Override
        public ValueType type() {
            return type;
        }

        @Override
        public Object defaultValue() {
            return defaultValue;
        }

        @Override
        public EntryMetadata metadata() {
            return metadata;
        }

        @Override
        public ValueAccessor accessor() {
            return accessor;
        }

        @Override
        public Object readFrom(final Object instance) {
            return accessor.read(instance);
        }

        @Override
        public void writeTo(
                final Object instance,
                final Object value
        ) {
            accessor.write(instance, value);
        }

    }

    static final class SchemaCategoryImpl implements SchemaCategory {

        //==================================================
        // Fields
        //==================================================

        private final String name;
        private final List<String> comment;
        private final List<SchemaEntry> entries;
        private final List<SchemaCategory> categories;
        private final Map<String, SchemaEntry> entryByKey;
        private final Map<String, SchemaCategory> categoryByName;

        //==================================================
        // Constructors
        //==================================================

        SchemaCategoryImpl(
                final String name,
                final List<String> comment,
                final List<SchemaEntry> entries,
                final List<SchemaCategory> categories
        ) {
            super();

            this.name = name;
            this.comment = List.copyOf(comment);
            this.entries = List.copyOf(entries);
            this.categories = List.copyOf(categories);

            final Map<String, SchemaEntry> em = new LinkedHashMap<>();
            for(final SchemaEntry e : entries) {
                em.put(e.key(), e);
            }
            this.entryByKey = Collections.unmodifiableMap(em);

            final Map<String, SchemaCategory> cm = new LinkedHashMap<>();
            for(final SchemaCategory c : categories) {
                cm.put(c.name(), c);
            }
            this.categoryByName = Collections.unmodifiableMap(cm);
        }

        //==================================================
        // Methods
        //==================================================

        @Override
        public String name() {
            return name;
        }

        @Override
        public List<String> comment() {
            return comment;
        }

        @Override
        public List<SchemaEntry> entries() {
            return entries;
        }

        @Override
        public List<SchemaCategory> categories() {
            return categories;
        }

        @Override
        public Optional<SchemaCategory> category(final String childName) {
            return Optional.ofNullable(categoryByName.get(childName));
        }

        @Override
        public Optional<SchemaEntry> entry(final String key) {
            return Optional.ofNullable(entryByKey.get(key));
        }

    }

    static final class SchemaImpl implements Schema {

        //==================================================
        // Static methods
        //==================================================

        private static void indexCategory(
                final SchemaCategory cat,
                final String prefix,
                final Map<String, SchemaEntry> idx
        ) {
            for(final SchemaEntry e : cat.entries()) {
                idx.put(prefix.isEmpty() ? e.key() : prefix + "." + e.key(), e);
            }
            for(final SchemaCategory child : cat.categories()) {
                final String next = prefix.isEmpty() ? child.name() : prefix + "." + child.name();
                indexCategory(child, next, idx);
            }
        }

        //==================================================
        // Fields
        //==================================================

        private final Class<?> type;
        private final String id;
        private final String name;
        private final Format format;
        private final int version;
        private final SchemaCategory root;
        private final Map<String, SchemaEntry> pathIndex;
        private final Set<String> allPaths;

        //==================================================
        // Constructors
        //==================================================

        SchemaImpl(
                final Class<?> type,
                final String id,
                final String name,
                final Format format,
                final int version,
                final SchemaCategory root
        ) {
            super();

            this.type = type;
            this.id = id;
            this.name = name;
            this.format = format;
            this.version = version;
            this.root = root;

            final Map<String, SchemaEntry> idx = new LinkedHashMap<>();
            indexCategory(root, "", idx);
            this.pathIndex = Collections.unmodifiableMap(idx);
            this.allPaths = Set.copyOf(idx.keySet());
        }

        //==================================================
        // Methods
        //==================================================

        @Override
        public Class<?> type() {
            return type;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Format format() {
            return format;
        }

        @Override
        public int version() {
            return version;
        }

        @Override
        public SchemaCategory root() {
            return root;
        }

        @Override
        public Optional<SchemaEntry> find(final String dottedPath) {
            return Optional.ofNullable(pathIndex.get(dottedPath));
        }

        @Override
        public Set<String> paths() {
            return allPaths;
        }

    }

}
