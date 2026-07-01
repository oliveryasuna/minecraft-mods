package com.oliveryasuna.mc.rubric.event;

import com.oliveryasuna.mc.rubric.api.annotation.Reload;

/**
 * A single value change, tagged with the entry's reload tier.
 */
public record ChangeEvent(
        String path,
        Object oldValue,
        Object newValue,
        Reload.Tier tier
) {

}
