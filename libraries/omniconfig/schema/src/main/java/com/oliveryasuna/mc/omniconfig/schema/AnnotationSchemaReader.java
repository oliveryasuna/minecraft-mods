package com.oliveryasuna.mc.omniconfig.schema;

import com.oliveryasuna.mc.omniconfig.api.annotation.*;
import com.oliveryasuna.mc.omniconfig.validation.validator.*;
import com.oliveryasuna.mc.omniconfig.value.CodecRegistry;
import com.oliveryasuna.mc.omniconfig.value.ValueType;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Reflectively reads an {@link Config}-annotated class into a {@link Schema}.
 * <p>
 * Uses only {@link java.lang.reflect}: no NightConfig and no Minecraft types.
 * <p>
 * <b>Declared fields only (by design).</b> Only fields declared directly on the
 * config class (and on nested-object classes it recurses into) are read;
 * inherited superclass fields are intentionally ignored. This keeps field
 * ordering and key resolution unambiguous. To share configuration between
 * classes, prefer <em>composition</em> (a nested-object field becomes a
 * category) over inheritance. (Documentation note: surface this clearly in user
 * docs — a base-class field silently not appearing is otherwise surprising).
 * <p>
 * The on-disk key for a field is its Java name unless overridden with
 * {@link Key}. Static, transient,
 * and synthetic fields are skipped.
 */
public final class AnnotationSchemaReader {

    //==================================================
    // Fields
    //==================================================

    private final DefaultsResolver defaults;
    /**
     * Extra classes the codec registry has marked as scalar leaves via
     * {@link CodecRegistry#registerLeaf(Class, com.oliveryasuna.mc.omniconfig.value.ValueCodec)}.
     * Empty for the no-arg constructor (built-in scalars only).
     */
    private final Set<Class<?>> leafTypes;

    //==================================================
    // Constructors
    //==================================================

    public AnnotationSchemaReader() {
        this(Set.of());
    }

    /**
     * Reads the codec registry's leaf-type set at construction so subsequent
     * {@link #read(Class)} calls classify those types as
     * {@link com.oliveryasuna.mc.omniconfig.value.ValueType.Kind#SCALAR} rather
     * than recursing into them as nested categories.
     *
     * @param codecs Registry whose leaf types should be honored.
     */
    public AnnotationSchemaReader(final CodecRegistry codecs) {
        this(codecs.getLeafTypes());
    }

    private AnnotationSchemaReader(final Set<Class<?>> leafTypes) {
        super();

        this.defaults = new DefaultsResolver();
        this.leafTypes = leafTypes;
    }

    //==================================================
    // Methods
    //==================================================

    public Schema read(final Class<?> type) {
        final Config config = type.getAnnotation(Config.class);
        if(config == null) {
            throw new IllegalArgumentException(type.getName() + " is not annotated with @Config");
        }

        final SchemaCategory.Builder root = readInto(type);

        final Schema schema = new Schema(type, config.id(), config.name(), config.format(), config.version(), root.build());
        SchemaValidator.validate(schema);
        return schema;
    }

    /**
     * Builds a {@link SchemaCategory} tree for an arbitrary class without
     * requiring {@link Config @Config}. Used by sub-editors that need to
     * render an element type (e.g. {@code List<MyPojo>}'s element class) as
     * a categorized set of fields.
     * <p>
     * The returned category is the (unnamed) root containing the class's
     * direct fields; nested-object fields become child sub-categories.
     * Default tier is {@link Reload.Tier#WORLD} and default scope is
     * {@link Sync.Scope#CLIENT} unless the class declares
     * {@link Reload @Reload} / {@link Sync @Sync}.
     *
     * @param type Class to read.
     * @return Root category for {@code type}.
     */
    public SchemaCategory readClass(final Class<?> type) {
        return readInto(type).build();
    }

    private void readInto(
            final Class<?> type,
            final Object owner,
            final SchemaCategory.Builder targetBuilder,
            final List<Field> ownerPath,
            final Reload.Tier inheritedTier,
            final Sync.Scope inheritedScope
    ) {
        for(final Field field : type.getDeclaredFields()) {
            final int mods = field.getModifiers();
            if(Modifier.isStatic(mods) || Modifier.isTransient(mods) || field.isSynthetic()) {
                continue;
            }
            field.setAccessible(true);

            final ValueType valueType = ValueType.of(field.getGenericType(), leafTypes);

            if(valueType.getKind() == ValueType.Kind.OBJECT) {
                readNestedCategory(field, owner, targetBuilder, ownerPath, inheritedTier, inheritedScope);
                continue;
            }

            final EntryMetadata metadata = buildMetadata(field, inheritedTier, inheritedScope);
            final Object defaultValue = defaults.read(field, owner);
            final String key = field.isAnnotationPresent(Key.class)
                    ? field.getAnnotation(Key.class).value()
                    : field.getName();
            final SchemaEntry entry = new SchemaEntry(key, valueType, defaultValue, metadata, new FieldAccessor(ownerPath, field));

            if(field.isAnnotationPresent(Category.class)) {
                targetBuilder.child(field.getAnnotation(Category.class).value()).addEntry(entry);
            } else {
                targetBuilder.addEntry(entry);
            }
        }
    }

    private SchemaCategory.Builder readInto(final Class<?> type) {
        final Reload.Tier rootTier = type.isAnnotationPresent(Reload.class)
                ? type.getAnnotation(Reload.class).value()
                : Reload.Tier.WORLD;
        final Sync.Scope rootScope = type.isAnnotationPresent(Sync.class)
                ? type.getAnnotation(Sync.class).value()
                : Sync.Scope.CLIENT;

        final Object rootDefault = defaults.instantiate(type);
        final SchemaCategory.Builder root = SchemaCategory.builder("");
        readInto(type, rootDefault, root, List.of(), rootTier, rootScope);

        return root;
    }

    private void readNestedCategory(
            final Field field,
            final Object owner,
            final SchemaCategory.Builder targetBuilder,
            final List<Field> ownerPath,
            final Reload.Tier inheritedTier,
            final Sync.Scope inheritedScope
    ) {
        final Class<?> nestedType = field.getType();

        final String categoryName = field.isAnnotationPresent(Category.class)
                ? field.getAnnotation(Category.class).value() : field.getName();

        final Reload.Tier subTier = nestedType.isAnnotationPresent(Reload.class)
                ? nestedType.getAnnotation(Reload.class).value() : inheritedTier;
        final Sync.Scope subScope = nestedType.isAnnotationPresent(Sync.class)
                ? nestedType.getAnnotation(Sync.class).value() : inheritedScope;

        Object nestedOwner = defaults.read(field, owner);
        if(nestedOwner == null) {
            nestedOwner = defaults.instantiate(nestedType);
        }

        final List<Field> childOwnerPath = new ArrayList<>(ownerPath);
        childOwnerPath.add(field);

        final SchemaCategory.Builder child = targetBuilder.child(categoryName);
        if(field.isAnnotationPresent(Comment.class)) {
            child.comment(List.of(field.getAnnotation(Comment.class).value()));
        }
        readInto(nestedType, nestedOwner, child, childOwnerPath, subTier, subScope);
    }

    private EntryMetadata buildMetadata(
            final Field field,
            final Reload.Tier inheritedTier,
            final Sync.Scope inheritedScope
    ) {
        final EntryMetadata.Builder b = EntryMetadata.builder();

        if(field.isAnnotationPresent(Comment.class)) {
            b.comment(List.of(field.getAnnotation(Comment.class).value()));
        }

        if(field.isAnnotationPresent(Reload.class)) {
            b.reloadTier(field.getAnnotation(Reload.class).value());
        } else if(field.isAnnotationPresent(RequiresRestart.class)) {
            b.reloadTier(Reload.Tier.RESTART);
        } else {
            b.reloadTier(inheritedTier);
        }

        b.syncScope(field.isAnnotationPresent(Sync.class)
                ? field.getAnnotation(Sync.class).value() : inheritedScope);

        if(field.isAnnotationPresent(Widget.class)) {
            final Widget widget = field.getAnnotation(Widget.class);
            b.widget(widget.value());
            b.allowInvalid(widget.allowInvalid());
        }

        b.hidden(field.isAnnotationPresent(Hidden.class));

        if(field.isAnnotationPresent(Range.class)) {
            final Range r = field.getAnnotation(Range.class);
            b.addValidator(new RangeValidator(r.min(), r.max()));
        }
        if(field.isAnnotationPresent(Length.class)) {
            final Length l = field.getAnnotation(Length.class);
            b.addValidator(new LengthValidator(l.min(), l.max()));
        }
        if(field.isAnnotationPresent(Pattern.class)) {
            b.addValidator(new PatternValidator(field.getAnnotation(Pattern.class).value()));
        }
        if(field.isAnnotationPresent(OneOf.class)) {
            b.addValidator(new OneOfValidator(field.getAnnotation(OneOf.class).value()));
        }
        if(field.isAnnotationPresent(NotNull.class)) {
            b.addValidator(new NotNullValidator());
        }

        return b.build();
    }

}
