package com.oliveryasuna.mc.omniconfig.lifecycle;

import com.oliveryasuna.mc.omniconfig.event.ChangeEvent;
import com.oliveryasuna.mc.omniconfig.schema.Schema;
import com.oliveryasuna.mc.omniconfig.value.ConfigSnapshot;
import com.oliveryasuna.mc.omniconfig.api.annotation.Reload;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Computes the per-entry changes between two snapshots, tagging each with its
 * reload tier.
 * <p>
 * The loader/game layer decides how to act on each tier; core only classifies.
 * (Deferral semantics for WORLD/RESTART are enforced by consumers).
 */
public final class ReloadController {

    //==================================================
    // Fields
    //==================================================

    private final Schema schema;

    //==================================================
    // Constructors
    //==================================================

    public ReloadController(final Schema schema) {
        super();

        this.schema = schema;
    }

    //==================================================
    // Methods
    //==================================================

    public List<ChangeEvent> diff(
            final ConfigSnapshot before,
            final ConfigSnapshot after
    ) {
        final List<ChangeEvent> changes = new ArrayList<>();
        for(final Map.Entry<String, Object> e : after.asMap().entrySet()) {
            final String path = e.getKey();
            final Object now = e.getValue();
            final Object old = before == null ? null : before.get(path);

            if(!Objects.equals(old, now)) {
                changes.add(new ChangeEvent(path, old, now, tierOf(path)));
            }
        }

        return changes;
    }

    public Reload.Tier highestTier(final List<ChangeEvent> changes) {
        Reload.Tier max = Reload.Tier.LIVE;
        for(final ChangeEvent c : changes) {
            if(c.tier().ordinal() > max.ordinal()) {
                max = c.tier();
            }
        }

        return max;
    }

    private Reload.Tier tierOf(final String path) {
        return schema.find(path)
                .map(entry -> entry.getMetadata().getReloadTier())
                .orElse(Reload.Tier.WORLD);
    }


}
