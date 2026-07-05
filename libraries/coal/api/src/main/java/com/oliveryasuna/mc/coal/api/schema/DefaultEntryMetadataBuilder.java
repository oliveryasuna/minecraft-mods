package com.oliveryasuna.mc.coal.api.schema;

import com.oliveryasuna.mc.coal.api.annotation.Reload;
import com.oliveryasuna.mc.coal.api.annotation.Sync;
import com.oliveryasuna.mc.coal.api.annotation.Widget;
import com.oliveryasuna.mc.coal.api.validation.Validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Package-private default builder for {@link EntryMetadata}. Instances are
 * obtained via {@link EntryMetadata#builder()}.
 */
final class DefaultEntryMetadataBuilder implements EntryMetadata.Builder {

    //==================================================
    // Fields
    //==================================================

    private List<String> comment;
    private Sync.Scope syncScope;
    private Reload.Tier reloadTier;
    private Widget.Type widget;
    private boolean hidden;
    private final List<Validator<?>> validators;
    private Optional<String> keyOverride;

    //==================================================
    // Constructors
    //==================================================

    DefaultEntryMetadataBuilder() {
        super();

        this.comment = List.of();
        this.syncScope = Sync.Scope.CLIENT;
        this.reloadTier = Reload.Tier.WORLD;
        this.widget = Widget.Type.AUTO;
        this.hidden = false;
        this.validators = new ArrayList<>();
        this.keyOverride = Optional.empty();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public EntryMetadata.Builder comment(final String... lines) {
        this.comment = Arrays.asList(lines);
        return this;
    }

    @Override
    public EntryMetadata.Builder syncScope(final Sync.Scope scope) {
        this.syncScope = scope;
        return this;
    }

    @Override
    public EntryMetadata.Builder reloadTier(final Reload.Tier tier) {
        this.reloadTier = tier;
        return this;
    }

    @Override
    public EntryMetadata.Builder widget(final Widget.Type type) {
        this.widget = type;
        return this;
    }

    @Override
    public EntryMetadata.Builder hidden(final boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    @Override
    public EntryMetadata.Builder addValidator(final Validator<?> validator) {
        this.validators.add(validator);
        return this;
    }

    @Override
    public EntryMetadata.Builder keyOverride(final String key) {
        this.keyOverride = Optional.ofNullable(key);
        return this;
    }

    @Override
    public EntryMetadata build() {
        return new EntryMetadata.DefaultEntryMetadata(
                comment,
                syncScope,
                reloadTier,
                widget,
                hidden,
                validators,
                keyOverride
        );
    }

}
