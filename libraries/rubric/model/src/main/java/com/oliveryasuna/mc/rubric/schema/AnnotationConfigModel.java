package com.oliveryasuna.mc.rubric.schema;

import com.oliveryasuna.mc.rubric.value.CodecRegistry;

/**
 * {@link ConfigModel} backed by a {@code @Config}-annotated POJO.
 *
 * @param <T> The type of the POJO.
 */
public final class AnnotationConfigModel<T> implements ConfigModel<T> {

    //==================================================
    // Fields
    //==================================================

    private final Class<T> type;
    private final Schema schema;
    private final DefaultsResolver defaults;

    //==================================================
    // Constructors
    //==================================================

    public AnnotationConfigModel(final Class<T> type) {
        this(type, new AnnotationSchemaReader());
    }

    /**
     * Builds the schema using {@code codecs}'s registered leaf types so custom
     * scalar types ({@code ResourceLocation}, Mojang-bridged codecs, etc.) are
     * classified
     * {@link com.oliveryasuna.mc.rubric.value.ValueType.Kind#SCALAR}
     * instead of being recursed into as nested categories.
     *
     * @param type   POJO class.
     * @param codecs Registry whose leaf types should be honored.
     */
    public AnnotationConfigModel(final Class<T> type, final CodecRegistry codecs) {
        this(type, new AnnotationSchemaReader(codecs));
    }

    private AnnotationConfigModel(final Class<T> type, final AnnotationSchemaReader reader) {
        super();

        this.type = type;
        this.schema = reader.read(type);
        this.defaults = new DefaultsResolver();
    }


    //==================================================
    // Methods
    //==================================================

    @Override
    public Schema schema() {
        return schema;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T newState() {
        return (T)defaults.instantiate(type);
    }

}
