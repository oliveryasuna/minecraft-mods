package com.oliveryasuna.mc.omniconfig.io;

import com.oliveryasuna.mc.omniconfig.schema.Schema;
import com.oliveryasuna.mc.omniconfig.value.ValueTree;
import com.oliveryasuna.mc.omniconifg.api.Format;

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
