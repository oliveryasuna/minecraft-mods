package com.oliveryasuna.mc.omniconfig.schema;

/**
 * Reads and writes one entry's value froma  config "state".
 * <p>
 * The state is a POJO for annotation-defined configs ({@link FieldAccessor}) or
 * a flat map for builder-defined configs ({@link MapAccessor}). This is what
 * lets both definition styles share the mapper/corrector/snapshot machinery.
 */
public interface ValueAccessor {

    //==================================================
    // Methods
    //==================================================

    Object read(Object state);

    void write(
            Object state,
            Object value
    );

}
