package com.oliveryasuna.mc.omniconfig.event;

import com.oliveryasuna.mc.omniconifg.api.annotation.Reload;

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
