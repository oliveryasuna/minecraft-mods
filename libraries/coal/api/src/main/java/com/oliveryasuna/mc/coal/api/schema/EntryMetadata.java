package com.oliveryasuna.mc.coal.api.schema;

import com.oliveryasuna.mc.coal.api.annotation.Reload;
import com.oliveryasuna.mc.coal.api.annotation.Sync;
import com.oliveryasuna.mc.coal.api.annotation.Widget;
import com.oliveryasuna.mc.coal.api.validation.Validator;

import java.util.List;
import java.util.Optional;

public interface EntryMetadata {

    //==================================================
    // Static methods
    //==================================================

    /**
     * Returns a fresh mutable {@link Builder} pre-populated with defaults:
     * empty comment, {@link Sync.Scope#CLIENT}, {@link Reload.Tier#WORLD},
     * {@link Widget.Type#AUTO}, not hidden, no validators, no key override.
     */
    static Builder builder() {
        return new DefaultEntryMetadataBuilder();
    }

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

    /**
     * Fluent builder for {@link EntryMetadata}. Obtain via
     * {@link EntryMetadata#builder()}.
     */
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

    /**
     * Immutable default implementation. Constructed via {@link Builder#build()}
     * or a caller that wants an ad-hoc instance.
     */
    record DefaultEntryMetadata(
            List<String> comment,
            Sync.Scope syncScope,
            Reload.Tier reloadTier,
            Widget.Type widget,
            boolean isHidden,
            List<Validator<?>> validators,
            Optional<String> keyOverride
    ) implements EntryMetadata {

        //==================================================
        // Constructors
        //==================================================

        public DefaultEntryMetadata {
            comment = List.copyOf(comment);
            validators = List.copyOf(validators);
        }

    }

}
