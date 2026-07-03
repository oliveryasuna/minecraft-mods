package com.oliveryasuna.mc.coal.api;

import com.oliveryasuna.commons.language.condition.Arguments;
import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.coal.api.config.ConfigHandle;
import com.oliveryasuna.mc.coal.api.config.ConfigSpec;
import com.oliveryasuna.mc.coal.api.migration.MigrationSpec;
import com.oliveryasuna.mc.coal.api.platform.Platform;
import com.oliveryasuna.mc.coal.api.spi.ConfigProvider;
import com.oliveryasuna.mc.coal.api.spi.ConfigProviderFactory;
import com.oliveryasuna.mc.coal.api.spi.ProviderNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * COAL entry point.
 * <p>
 * Discovers a {@link Platform} and a {@link ConfigProvider} via
 * {@code ServiceLoader} on first use, then delegates every call to the
 * discovered provider. Modeled on SLF4J's {@code LoggerFactory}.
 * <p>
 * <b>Provider selection.</b> Highest
 * {@link ConfigProviderFactory#priority()} wins. A {@code WARN} names every
 * candidate when more than one is discovered. The lowest-priority provider is
 * always {@code coal-noop} (priority 0), bundled with the {@code coal} mod, so
 * a real provider is never strictly required for the game to boot.
 * <p>
 * <b>Platform discovery.</b> Exactly one {@link Platform} is expected on the
 * classpath — shipped by whichever COAL loader integration is installed
 * ({@code coal-fabric} / {@code coal-neoforge}). Zero or multiple are treated
 * as configuration errors — {@link #ensureProvider()} throws
 * {@link ProviderNotFoundException}.
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
     * Discovers the {@link Platform} and {@link ConfigProvider} via
     * {@link ServiceLoader} and installs the provider. First-wins; subsequent
     * calls no-op with a {@code WARN}.
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
     * ServiceLoader dispatch. Requires exactly one {@link Platform} on the
     * classpath and at least one {@link ConfigProviderFactory}. Highest factory
     * priority wins; {@code WARN} names every candidate on overlap. The
     * {@code coal-noop} factory (priority 0) is always the last-resort pick
     * when it's the only provider on the classpath.
     *
     * @throws ProviderNotFoundException if no {@link Platform} was
     *                                   discovered, or no
     *                                   {@link ConfigProviderFactory} was
     *                                   discovered.
     */
    private static ConfigProvider discover() {
        final Platform platform = discoverPlatform();

        final List<ConfigProviderFactory> factories = new ArrayList<>();
        for(final ConfigProviderFactory factory : ServiceLoader.load(ConfigProviderFactory.class)) {
            factories.add(factory);
        }

        if(factories.isEmpty()) {
            throw new ProviderNotFoundException(
                    "No COAL ConfigProviderFactory found on the classpath. "
                    + "The 'coal' mod bundles coal-noop as a last-resort provider; if you see this, "
                    + "the classpath is not what you think it is."
            );
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
        final ConfigProvider p = selected.create(platform);

        LOGGER.info("COAL provider '{}' installed (priority {}).", selected.name(), selected.priority());
        LOGGER.debug("COAL platform: {}", platform.getClass().getName());

        return p;
    }

    /**
     * Loads exactly one {@link Platform} via {@link ServiceLoader}.
     *
     * @throws ProviderNotFoundException if zero or multiple Platforms are
     *                                   discovered.
     */
    private static Platform discoverPlatform() {
        final Iterator<Platform> it = ServiceLoader.load(Platform.class).iterator();
        if(!it.hasNext()) {
            throw new ProviderNotFoundException("No COAL Platform found on the classpath. Install a loader integration (e.g., the 'coal' mod for Fabric or NeoForge).");
        }
        final Platform platform = it.next();
        if(it.hasNext()) {
            final StringBuilder found = new StringBuilder(platform.getClass().getName());
            while(it.hasNext()) {
                found.append(", ").append(it.next().getClass().getName());
            }
            throw new ProviderNotFoundException(
                    "Multiple COAL Platforms found on the classpath, expected exactly one: " + found
            );
        }
        return platform;
    }

    //==================================================
    // Constructors
    //==================================================

    private Coal() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
