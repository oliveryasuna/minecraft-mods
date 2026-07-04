package com.oliveryasuna.mc.coal.yacl.fabric;

import com.oliveryasuna.mc.coal.api.Format;
import com.oliveryasuna.mc.coal.api.annotation.*;
import com.oliveryasuna.mc.coal.api.config.ConfigSpec;
import com.oliveryasuna.mc.coal.api.schema.*;
import com.oliveryasuna.mc.coal.api.validation.Validator;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Supplier;

/**
 * Reflection-based {@link SchemaReader}. Reads COAL annotations directly off
 * consumer POJOs — no {@code @ConfigEntry.Gui.*} or {@code ConfigData}
 * required.
 * <p>
 * <b>Category model.</b> Field-level {@link Category} places entries under the
 * named category. Type-level {@link Category} prefixes every entry. Field-level
 * category paths MAY be dotted (e.g., {@code @Category("gui.frontend")}) to
 * nest deeper — segments matching an existing category on the path are reused.
 */
final class AnnotationSchemaReader implements SchemaReader {

    //==================================================
    // Static methods
    //==================================================

    // Category-tree assembly
    //--------------------------------------------------

    private static void ensureAncestors(
            final Map<String, CategoryBucket> buckets,
            final String path
    ) {
        if(path.isEmpty()) {
            return;
        }

        final int lastDot = path.lastIndexOf('.');
        final String parent = lastDot < 0 ? "" : path.substring(0, lastDot);
        buckets.computeIfAbsent(parent, p -> new CategoryBucket(leafName(p), List.of()));
        ensureAncestors(buckets, parent);
    }

    private static boolean isDirectChild(
            final String parent,
            final String candidate
    ) {
        if(parent.isEmpty()) {
            return !candidate.contains(".");
        } else if(!candidate.startsWith(parent + ".")) {
            return false;
        }

        return !candidate.substring(parent.length() + 1).contains(".");
    }

    private static int depthOf(final String path) {
        if(path.isEmpty()) {
            return 0;
        }

        int d = 1;
        for(int i = 0; i < path.length(); i++) {
            if(path.charAt(i) == '.') {
                d++;
            }
        }

        return d;
    }

    private static String leafName(final String dotted) {
        final int lastDot = dotted.lastIndexOf('.');

        return lastDot < 0 ? dotted : dotted.substring(lastDot + 1);
    }

    // Reflection helpers
    //--------------------------------------------------

    private static Object readDefault(final Field field) {
        final Class<?> owner = field.getDeclaringClass();
        try {
            final Object instance = newInstanceReflective(owner);
            field.setAccessible(true);

            return field.get(instance);
        } catch(final ReflectiveOperationException e) {
            throw new IllegalArgumentException(
                    "coal-yacl-adapter: cannot read default from " + owner.getName() + "#" + field.getName()
                    + " — needs a public no-arg constructor", e
            );
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T newInstance(final Class<T> type) {
        try {
            return (T)newInstanceReflective(type);
        } catch(final ReflectiveOperationException e) {
            throw new IllegalArgumentException("coal-yacl-adapter: cannot instantiate " + type.getName() + " — needs a public no-arg constructor", e);
        }
    }

    private static Object newInstanceReflective(final Class<?> type) throws ReflectiveOperationException {
        final Constructor<?> ctor = type.getDeclaredConstructor();
        ctor.setAccessible(true);

        return ctor.newInstance();
    }

    private static Map<String, Object> defaultsMap(final ConfigSpec spec) {
        final Map<String, Object> map = new LinkedHashMap<>();
        for(final ConfigSpec.EntrySpec e : spec.getEntries()) {
            map.put(fullPath(e.categoryPath(), e.key()), e.defaultValue());
        }

        return map;
    }

    private static String readTypeCategory(final Class<?> type) {
        final Category c = type.getAnnotation(Category.class);

        return c != null ? c.value() : "";
    }

    private static List<String> readTypeComment(final Class<?> type) {
        final Comment c = type.getAnnotation(Comment.class);

        return c != null ? List.of(c.value()) : List.of();
    }

    private static String readFieldCategory(final Field field) {
        final Category c = field.getAnnotation(Category.class);

        return c != null ? c.value() : "";
    }

    private static String combine(
            final String a,
            final String b
    ) {
        if(a.isEmpty()) {
            return b;
        } else if(b.isEmpty()) {
            return a;
        }

        return a + "." + b;
    }

    private static String fullPath(
            final String categoryPath,
            final String key
    ) {
        return categoryPath == null || categoryPath.isEmpty() ? key : categoryPath + "." + key;
    }

    //==================================================
    // Constructors
    //==================================================

    AnnotationSchemaReader() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    // SchemaReader
    //--------------------------------------------------

    @Override
    public <S> ConfigModel<S> read(final Class<S> type) {
        final Config cfg = type.getAnnotation(Config.class);
        if(cfg == null) {
            throw new IllegalArgumentException("coal-yacl-adapter: type " + type.getName() + " is missing @Config");
        }

        final Format format = Format.of(cfg.format());
        final String typeLevelCategoryPath = readTypeCategory(type);

        // Build entries under root/prefixed paths using a mutable per-path
        // bucket.
        final Map<String, CategoryBucket> buckets = new LinkedHashMap<>();
        buckets.put("", new CategoryBucket("", readTypeComment(type)));

        for(final Field field : type.getDeclaredFields()) {
            if(Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if(Modifier.isTransient(field.getModifiers())) {
                continue;
            }

            final String catPath = combine(typeLevelCategoryPath, readFieldCategory(field));

            final SchemaEntry entry = buildEntry(field);
            buckets.computeIfAbsent(catPath, p -> new CategoryBucket(leafName(p), List.of()))
                    .entries.add(entry);

            // Ensure every ancestor path exists as a bucket so we can assemble a tree later.
            ensureAncestors(buckets, catPath);
        }

        final S defaultInstance = newInstance(type);
        final SchemaCategory root = assembleCategoryTree(buckets);
        final Schema schema = new Schemas.SchemaImpl(type, cfg.id(), cfg.name(), format, cfg.version(), root);

        return new ConfigModelImpl<>(schema, () -> newInstance(type));
    }

    @Override
    public ConfigModel<Map<String, Object>> read(final ConfigSpec spec) {
        final Map<String, CategoryBucket> buckets = new LinkedHashMap<>();
        buckets.put("", new CategoryBucket("", List.of()));

        for(final ConfigSpec.EntrySpec e : spec.getEntries()) {
            final SchemaEntry entry = new Schemas.SchemaEntryImpl(
                    e.key(),
                    inferValueType(e.type(), e.type()),
                    e.defaultValue(),
                    e.metadata(),
                    new MapEntryAccessor(fullPath(e.categoryPath(), e.key()), e.type())
            );
            final String catPath = e.categoryPath() == null ? "" : e.categoryPath();
            buckets.computeIfAbsent(catPath, p -> new CategoryBucket(leafName(p), List.of()))
                    .entries.add(entry);
            ensureAncestors(buckets, catPath);
        }

        final SchemaCategory root = assembleCategoryTree(buckets);
        final Schema schema = new Schemas.SchemaImpl(Map.class, spec.getId(), spec.getName(), spec.getFormat(), spec.getVersion(), root);

        return new ConfigModelImpl<>(schema, () -> defaultsMap(spec));
    }

    // Entry builders
    //--------------------------------------------------

    private SchemaEntry buildEntry(final Field field) {
        final Key keyAnnotation = field.getAnnotation(Key.class);
        final String key = keyAnnotation != null ? keyAnnotation.value() : field.getName();

        final ValueType valueType = inferValueType(field.getType(), field.getGenericType());
        final Object defaultValue = readDefault(field);

        final EntryMetadata.Builder mb = EntryMetadata.builder();

        final Comment c = field.getAnnotation(Comment.class);
        if(c != null) {
            mb.comment(c.value());
        }

        final Widget w = field.getAnnotation(Widget.class);
        if(w != null) {
            mb.widget(w.value());
        }

        if(field.isAnnotationPresent(Hidden.class)) mb.hidden(true);

        final Sync sync = field.getAnnotation(Sync.class);
        if(sync != null) {
            mb.syncScope(sync.value());
        }

        final Reload reload = field.getAnnotation(Reload.class);
        if(reload != null) {
            mb.reloadTier(reload.value());
        } else if(field.isAnnotationPresent(RequiresRestart.class)) {
            mb.reloadTier(Reload.Tier.RESTART);
        }

        if(keyAnnotation != null) {
            mb.keyOverride(keyAnnotation.value());
        }

        for(final Validator<?> v : Validators.forField(field)) {
            mb.addValidator(v);
        }

        final EntryMetadata metadata = mb.build();

        return new Schemas.SchemaEntryImpl(key, valueType, defaultValue, metadata, new Values.FieldAccessor(field));
    }

    // ValueType inference
    //--------------------------------------------------

    private ValueType inferValueType(
            final Class<?> raw,
            final Type generic
    ) {
        if(raw.isEnum()) {
            return Values.ValueTypeImpl.enumType(raw);
        } else if(raw == String.class || raw.isPrimitive() || Number.class.isAssignableFrom(raw) || raw == Boolean.class || raw == Character.class) {
            return Values.ValueTypeImpl.scalar(raw);
        } else if(Collection.class.isAssignableFrom(raw)) {
            return Values.ValueTypeImpl.list(raw, inferElementType(generic, 0));
        } else if(Map.class.isAssignableFrom(raw)) {
            return Values.ValueTypeImpl.map(raw, inferElementType(generic, 1));
        }

        return Values.ValueTypeImpl.object(raw, List.of());
    }

    /**
     * Resolve a {@link ParameterizedType} argument at {@code index} into a
     * {@link ValueType}. Falls back to a raw {@code Object} scalar when the
     * generic type isn't parameterized (raw type at reflection layer).
     */
    private ValueType inferElementType(
            final Type generic,
            final int index
    ) {
        if(generic instanceof final ParameterizedType pt) {
            final Type[] args = pt.getActualTypeArguments();
            if(index < args.length) {
                final Type arg = args[index];
                if(arg instanceof final Class<?> c) {
                    return inferValueType(c, c);
                } else if(arg instanceof final ParameterizedType nested) {
                    final Type rawTypeArg = nested.getRawType();
                    if(rawTypeArg instanceof final Class<?> rawClass) {
                        return inferValueType(rawClass, nested);
                    }
                }
            }
        }

        return Values.ValueTypeImpl.scalar(Object.class);
    }

    // Category-tree assembly
    //--------------------------------------------------

    private SchemaCategory assembleCategoryTree(final Map<String, CategoryBucket> buckets) {
        // Walk from deepest paths back to root, wrapping child categories into parents.
        final List<String> paths = new ArrayList<>(buckets.keySet());
        paths.sort((a, b) -> Integer.compare(depthOf(b), depthOf(a)));

        final Map<String, SchemaCategory> built = new HashMap<>();

        for(final String path : paths) {
            final CategoryBucket bucket = buckets.get(path);
            final List<SchemaCategory> children = new ArrayList<>();
            for(final String other : paths) {
                if(other.equals(path)) {
                    continue;
                }
                if(!isDirectChild(path, other)) {
                    continue;
                }

                final SchemaCategory built0 = built.get(other);
                if(built0 != null) {
                    children.add(built0);
                }
            }

            built.put(path, Schemas.buildCategory(bucket.name, bucket.comment, bucket.entries, children));
        }

        return built.get("");
    }

    //==================================================
    // Nested
    //==================================================

    /**
     * Mutable intermediate used during tree assembly. Fields are populated as
     * we walk the reflected class, then handed to
     * {@link Schemas#buildCategory}.
     */
    private static final class CategoryBucket {

        //==================================================
        // Fields
        //==================================================

        final String name;
        final List<String> comment;
        final List<SchemaEntry> entries;

        //==================================================
        // Constructors
        //==================================================

        CategoryBucket(
                final String name,
                final List<String> comment
        ) {
            super();

            this.name = name;
            this.comment = comment;
            this.entries = new ArrayList<>();
        }

    }

    /**
     * {@link ConfigModel} record — trivial pair-plus-supplier.
     */
    private static final class ConfigModelImpl<S> implements ConfigModel<S> {

        //==================================================
        // Fields
        //==================================================

        private final Schema schema;
        private final Supplier<S> stateFactory;

        //==================================================
        // Constructors
        //==================================================

        ConfigModelImpl(
                final Schema schema,
                final Supplier<S> stateFactory
        ) {
            super();

            this.schema = schema;
            this.stateFactory = stateFactory;
        }

        //==================================================
        // Methods
        //==================================================

        @Override
        public Schema schema() {
            return schema;
        }

        @Override
        public S newState() {
            return stateFactory.get();
        }

    }

    /**
     * {@link ValueAccessor} for
     * {@code ConfigSpec}-derived entries whose backing state is a
     * {@code Map<String, Object>}. Reads/writes the fully-qualified path key.
     */
    private static final class MapEntryAccessor implements ValueAccessor {

        //==================================================
        // Fields
        //==================================================

        private final String path;
        private final Class<?> declared;

        //==================================================
        // Consturctors
        //==================================================

        MapEntryAccessor(
                final String path,
                final Class<?> declared
        ) {
            super();

            this.path = path;
            this.declared = declared;
        }

        //==================================================
        // Methods
        //==================================================

        @Override
        @SuppressWarnings("unchecked")
        public Object read(final Object instance) {
            return ((Map<String, Object>)instance).get(path);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void write(final Object instance, final Object value) {
            ((Map<String, Object>)instance).put(path, value);
        }

        @Override
        public Class<?> declaredType() {
            return declared;
        }

    }

}
