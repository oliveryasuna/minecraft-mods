package com.oliveryasuna.mc.omniconfig.lifecycle;

/**
 * Notified after a
 * {@link com.oliveryasuna.mc.omniconfig.api.ConfigManager ConfigManager}
 * replaces its state via a successful reload. Fires only on subsequent loads
 * (not the first one) and always on the platform's main thread.
 *
 * @param <S> State type the manager carries (the {@code @Config} POJO, or
 *            {@code Map<String, Object>} for builder configs).
 */
@FunctionalInterface
public interface ReloadListener<S> {

    /**
     * @param previous State instance that has just been replaced. The
     *                 manager no longer holds it; callers may inspect it to
     *                 capture pre-reload values (e.g. in-flight UI edits)
     *                 before applying anything to {@code current}.
     * @param current  State instance the manager now holds. Mutations made
     *                 here are visible via
     *                 {@link com.oliveryasuna.mc.omniconfig.api.ConfigManager#get ConfigManager.get()}.
     */
    void onReload(S previous, S current);

}
