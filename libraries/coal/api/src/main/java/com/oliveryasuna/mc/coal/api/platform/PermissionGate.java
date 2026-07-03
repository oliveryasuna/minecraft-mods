package com.oliveryasuna.mc.coal.api.platform;

@FunctionalInterface
public interface PermissionGate {

    //==================================================
    // Static fields
    //==================================================

    PermissionGate DENY_ALL = (actor, level) -> false;
    PermissionGate ALLOW_ALL = (actor, level) -> true;

    //==================================================
    // Methods
    //==================================================

    boolean canEdit(
            Object actor,
            int requiredLevel
    );

}
