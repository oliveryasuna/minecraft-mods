package com.oliveryasuna.mc.rubric.io;

import com.oliveryasuna.mc.rubric.schema.Schema;
import com.oliveryasuna.mc.rubric.value.ValueTree;
import com.oliveryasuna.mc.rubric.api.Format;

/**
 * Converts between raw bytes and a {@link ValueTree} for one {@link Format}.
 */
public interface FormatAdapter {

    //==================================================
    // Methods
    //==================================================

    Format format();

    ValueTree parse(byte[] bytes);

    byte[] render(
            ValueTree tree,
            Schema schema
    );

}
