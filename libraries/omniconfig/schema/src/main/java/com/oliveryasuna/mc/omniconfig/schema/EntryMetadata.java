package com.oliveryasuna.mc.omniconfig.schema;

import com.oliveryasuna.mc.omniconfig.validation.Validator;
import com.oliveryasuna.mc.omniconifg.api.annotation.Reload;
import com.oliveryasuna.mc.omniconifg.api.annotation.Sync;
import com.oliveryasuna.mc.omniconifg.api.annotation.Widget;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable presentation/behavior metadata for one config entry.
 */
public final class EntryMetadata {

    //==================================================
    // Static methods
    //==================================================

    public static Builder builder() {
        return new Builder();
    }

    //==================================================
    // Fields
    //==================================================

    private final List<String> comment;
    private final Reload.Tier reloadTier;
    private final Sync.Scope syncScope;
    private final Widget.Type widget;
    private final boolean hidden;
    private final boolean allowInvalid;
    private final List<Validator<?>> validators;

    //==================================================
    // Constructors
    //==================================================

    private EntryMetadata(
            final List<String> comment,
            final Reload.Tier reloadTier,
            final Sync.Scope syncScope,
            final Widget.Type widget,
            final boolean hidden,
            final boolean allowInvalid,
            final List<Validator<?>> validators
    ) {
        super();

        this.comment = List.copyOf(comment);
        this.reloadTier = reloadTier;
        this.syncScope = syncScope;
        this.widget = widget;
        this.hidden = hidden;
        this.allowInvalid = allowInvalid;
        this.validators = List.copyOf(validators);
    }

    //==================================================
    // Getters/setters
    //==================================================

    public List<String> getComment() {
        return comment;
    }

    public Reload.Tier getReloadTier() {
        return reloadTier;
    }

    public Sync.Scope getSyncScope() {
        return syncScope;
    }

    public Widget.Type getWidget() {
        return widget;
    }

    public boolean isHidden() {
        return hidden;
    }

    /**
     * @return Whether the GUI may save invalid edits for this entry. When
     * {@code false} (default), an invalid widget state blocks save until the
     * user fixes it. See {@link Widget#allowInvalid()}.
     */
    public boolean isAllowInvalid() {
        return allowInvalid;
    }

    public List<Validator<?>> getValidators() {
        return validators;
    }

    //==================================================
    // Nested
    //==================================================

    /**
     * Mutable builder used by the schema reader.
     */
    public static final class Builder implements org.apache.commons.lang3.builder.Builder<EntryMetadata> {

        //==================================================
        // Fields
        //==================================================

        private List<String> comment;
        private Reload.Tier reloadTier;
        private Sync.Scope syncScope;
        private Widget.Type widget;
        private boolean hidden;
        private boolean allowInvalid;
        private final List<Validator<?>> validators;

        //==================================================
        // Constructors
        //==================================================

        private Builder() {
            super();

            this.comment = List.of();
            this.reloadTier = Reload.Tier.WORLD;
            this.syncScope = Sync.Scope.CLIENT;
            this.widget = Widget.Type.AUTO;
            this.hidden = false;
            this.allowInvalid = false;
            this.validators = new ArrayList<>();
        }

        //==================================================
        // Methods
        //==================================================

        @Override
        public EntryMetadata build() {
            return new EntryMetadata(comment, reloadTier, syncScope, widget, hidden, allowInvalid, validators);
        }

        public Builder comment(final List<String> comment) {
            this.comment = Objects.requireNonNull(comment, "comment");
            return this;
        }

        public Builder reloadTier(final Reload.Tier reloadTier) {
            this.reloadTier = Objects.requireNonNull(reloadTier, "reloadTier");
            return this;
        }

        public Builder syncScope(final Sync.Scope syncScope) {
            this.syncScope = Objects.requireNonNull(syncScope, "syncScope");
            return this;
        }

        public Builder widget(final Widget.Type widget) {
            this.widget = Objects.requireNonNull(widget, "widget");
            return this;
        }

        public Builder hidden(final boolean hidden) {
            this.hidden = hidden;
            return this;
        }

        public Builder allowInvalid(final boolean allowInvalid) {
            this.allowInvalid = allowInvalid;
            return this;
        }

        public Builder addValidator(final Validator<?> validator) {
            this.validators.add(Objects.requireNonNull(validator, "validator"));
            return this;
        }

    }

}
