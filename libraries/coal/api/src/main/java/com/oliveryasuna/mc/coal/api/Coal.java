package com.oliveryasuna.mc.coal.api;

import com.oliveryasuna.commons.language.condition.Arguments;
import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.coal.api.config.ConfigHandle;
import com.oliveryasuna.mc.coal.api.config.ConfigSpec;
import com.oliveryasuna.mc.coal.api.migration.MigrationSpec;
import com.oliveryasuna.mc.coal.api.platform.Platform;
import com.oliveryasuna.mc.coal.api.spi.ConfigProvider;
import com.oliveryasuna.mc.coal.api.spi.ConfigProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * COAL entry point.
 * <p>
 * Discovers a {@link ConfigProvider} via
 * {@code ServiceLoader<ConfigProviderFactory>} on first use, then delegates
 * every call to it. Modeled on SLF4J's {@code LoggerFactory}.
 * <p>
 * <b>Provider selection.</b> Highest {@link ConfigProviderFactory#priority()}
 * wins. A {@code WARN} names every candidate when more than one is discovered.
 * <p>
 * <b>Zero providers.</b> An inline private fallback provider is installed with
 * an {@code ERROR} log. The fallback returns fresh POJO instances but does not
 * persist. Consumers who want deliberate no-op behavior should add
 * {@code coal-noop} to their build — it's a full, deep-no-op implementation.
 * <p>
 * <b>Idempotency.</b> {@link #bootstrap()} is first-wins; subsequent calls
 * no-op with a {@code WARN}. {@link #bootstrap(ConfigProvider)} always replaces
 * (INFO-logged) — intended for tests and deliberate overrides.
 */
public final class Coal {

    //==================================================
    // Static fields
    //==================================================

    private static final Logger LOGGER = LoggerFactory.getLogger("coal");

    private static final Object LOCK = new Object();

    private static volatile ConfigProvider provider;

    //==================================================
    // Static methods
    //==================================================

    // Bootstrap
    //--------------------------------------------------

    /**
     * Discovers a {@link ConfigProvider} via {@link ServiceLoader} and installs
     * it. First-wins; subsequent calls no-op with a {@code WARN}.
     */
    public static void bootstrap() {
        synchronized(LOCK) {
            if(Coal.provider != null) {
                LOGGER.warn("Coal.bootstrap() called after a provider was already installed ({}). Ignoring.", Coal.provider.name());
                return;
            }

            Coal.provider = discover();
        }
    }

    /**
     * Installs {@code explicitProvider} unconditionally, replacing any
     * previously installed provider. Logs at {@code INFO}. Intended for tests
     * and deliberate overrides.
     */
    public static void bootstrap(final ConfigProvider explicitProvider) {
        Arguments.requireNotNull(explicitProvider, "explicitProvider");

        synchronized(LOCK) {
            final ConfigProvider previous = Coal.provider;
            Coal.provider = explicitProvider;

            if(previous != null) {
                LOGGER.info("Coal.bootstrap(ConfigProvider) replaced installed provider '{}' with '{}'.", previous.name(), explicitProvider.name());
            } else {
                LOGGER.info("Coal.bootstrap(ConfigProvider) installed provider '{}'.", explicitProvider.name());
            }
        }
    }

    // Registration
    //--------------------------------------------------

    // Annotated POJO -> managed handle
    //

    public static <S> ConfigHandle<S> register(final Class<S> type) {
        return register(type, MigrationSpec.empty());
    }

    public static <S> ConfigHandle<S> register(
            final Class<S> type,
            final MigrationSpec migrations
    ) {
        return ensureProvider().register(type, migrations);
    }

    // Dynamically-shaped configs
    //

    public static ConfigHandle<Map<String, Object>> register(final ConfigSpec spec) {
        return register(spec, MigrationSpec.empty());
    }

    public static ConfigHandle<Map<String, Object>> register(
            final ConfigSpec spec,
            final MigrationSpec migrations
    ) {
        return ensureProvider().register(spec, migrations);
    }

    // Access to the installed provider (rarely needed by mods)
    //--------------------------------------------------

    public static ConfigProvider getProvider() {
        return ensureProvider();
    }

    public static boolean isBootstrapped() {
        return Coal.provider != null;
    }

    // Helpers
    //--------------------------------------------------

    /**
     * Returns the installed provider, auto-bootstrapping on first use.
     */
    private static ConfigProvider ensureProvider() {
        if(Coal.provider == null) {
            synchronized(LOCK) {
                if(Coal.provider == null) {
                    Coal.provider = discover();
                }
            }
        }

        return Coal.provider;
    }

    /**
     * ServiceLoader dispatch. Highest priority wins; WARN names every candidate
     * on ties or overlap. Installs the inline fallback with an ERROR log if
     * nothing is discovered.
     */
    private static ConfigProvider discover() {
        final List<ConfigProviderFactory> factories = new ArrayList<>();
        for(final ConfigProviderFactory factory : ServiceLoader.load(ConfigProviderFactory.class)) {
            factories.add(factory);
        }

        if(factories.isEmpty()) {
            LOGGER.error(
                    "No COAL provider found on the classpath. Installing inline fallback — "
                    + "config calls will not persist. Add 'coal-noop' for deliberate no-op behavior "
                    + "or a real implementation (e.g., 'coal-rubric')."
            );

            return new InlineFallbackProvider(inlinePlatform());
        }

        factories.sort(Comparator.comparingInt(ConfigProviderFactory::priority).reversed());

        if(factories.size() > 1) {
            final StringBuilder candidates = new StringBuilder();
            for(final ConfigProviderFactory f : factories) {
                candidates.append(f.name()).append("(priority=").append(f.priority()).append("), ");
            }

            LOGGER.warn(
                    "Multiple COAL providers on the classpath: {}. Installing '{}' (highest priority).",
                    candidates.substring(0, candidates.length() - 2),
                    factories.getFirst().name()
            );
        }

        final ConfigProviderFactory selected = factories.getFirst();
        final ConfigProvider p = selected.create(inlinePlatform());

        LOGGER.info("COAL provider '{}' installed (priority {}).", selected.name(), selected.priority());

        return p;
    }

    /**
     * Minimal {@link Platform} for the discovery/fallback path. Providers that
     * want a richer Platform can be created via
     * {@link ConfigProviderFactory#create(Platform)} in a later API rev that
     * lets consumers supply a Platform instance to {@link #bootstrap()}. For
     * now, the inline platform delegates config dir to the JVM working
     * directory.
     */
    private static Platform inlinePlatform() {
        return InlinePlatform.INSTANCE;
    }

    //==================================================
    // Constructors
    //==================================================

    private Coal() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
