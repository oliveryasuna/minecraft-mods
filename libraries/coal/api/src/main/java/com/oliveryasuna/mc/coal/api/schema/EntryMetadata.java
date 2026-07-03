package com.oliveryasuna.mc.coal.api.schema;

import com.oliveryasuna.mc.coal.api.annotation.Reload;
import com.oliveryasuna.mc.coal.api.annotation.Sync;
import com.oliveryasuna.mc.coal.api.annotation.Widget;
import com.oliveryasuna.mc.coal.api.validation.Validator;

import java.util.List;
import java.util.Optional;

public interface EntryMetadata {

    //==================================================
    // Methods
    //==================================================

    List<String> comment();

    Sync.Scope syncScope();

    Reload.Tier reloadTier();

    Widget.Type widget();

    boolean isHidden();

    List<Validator<?>> validators();

    Optional<String> keyOverride();

    //==================================================
    // Nested
    //==================================================

    interface Builder extends org.apache.commons.lang3.builder.Builder<EntryMetadata> {

        //==================================================
        // Methods
        //==================================================

        Builder comment(String... lines);

        Builder syncScope(Sync.Scope scope);

        Builder reloadTier(Reload.Tier tier);

        Builder widget(Widget.Type type);

        Builder hidden(boolean hidden);

        Builder addValidator(Validator<?> validator);

        Builder keyOverride(String key);

    }

}
