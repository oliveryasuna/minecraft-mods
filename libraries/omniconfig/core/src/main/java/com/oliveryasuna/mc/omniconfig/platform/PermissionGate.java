package com.oliveryasuna.mc.omniconfig.platform;

/**
 * Authorizes privileged config actions (e.g., a client editing server-scoped
 * values).
 * <p>
 * Default implementation denies everything; loader adapters supply a real
 * permission-level check.
 */
@FunctionalInterface
public interface PermissionGate {

    //==================================================
    // Static fields
    //==================================================

    PermissionGate DENY_ALL = (actor, requiredLevel) -> false;

    //==================================================
    // Methods
    //==================================================

    boolean canEdit(
            Object actor,
            int requiredLevel
    );

}
