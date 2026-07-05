package com.oliveryasuna.mc.coal.fabric;

import com.oliveryasuna.mc.coal.api.platform.Environment;
import com.oliveryasuna.mc.coal.api.platform.Platform;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * Fabric implementation of {@link Platform}. Discovered by
 * {@link com.oliveryasuna.mc.coal.api.Coal Coal}'s ServiceLoader dispatch
 * via {@code META-INF/services/com.oliveryasuna.mc.coal.api.platform.Platform}.
 * <p>
 * The main-thread executor targets {@link Minecraft} on client dist and the
 * live {@link MinecraftServer} on server dist. {@link CoalFabricMod} wires
 * {@link #setServer(MinecraftServer)} on
 * {@code SERVER_STARTING}/{@code SERVER_STOPPED}.
 */
public final class FabricPlatform implements Platform {

    //==================================================
    // Static fields
    //==================================================

    /**
     * Package-private handle so {@link CoalFabricMod} can call
     * {@link #setServer(MinecraftServer)} on the specific instance
     * ServiceLoader constructed.
     */
    static volatile FabricPlatform installed;

    //==================================================
    // Fields
    //==================================================

    private volatile MinecraftServer server;

    //==================================================
    // Constructors
    //==================================================

    public FabricPlatform() {
        super();

        installed = this;
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public Executor mainThreadExecutor() {
        if(FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return runnable -> {
                // Defensive: Minecraft.getInstance() can be null very early
                // in game startup. Fall back to inline so callers work
                // regardless of phase.
                final Minecraft mc = Minecraft.getInstance();
                if(mc != null) {
                    mc.execute(runnable);
                } else {
                    runnable.run();
                }
            };
        }

        return runnable -> {
            final MinecraftServer s = server;
            if(s != null) {
                s.execute(runnable);
            } else {
                runnable.run();
            }
        };
    }

    @Override
    public Logger logger(final String name) {
        return LoggerFactory.getLogger(name);
    }

    @Override
    public Environment environment() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT
                ? Environment.CLIENT
                : Environment.SERVER;
    }

    @Override
    public Optional<Path> gameDir() {
        return Optional.of(FabricLoader.getInstance().getGameDir());
    }

    @Override
    public Optional<String> loaderName() {
        return Optional.of("fabric");
    }

    @Override
    public Optional<String> loaderVersion() {
        return FabricLoader.getInstance()
                .getModContainer("fabricloader")
                .map(mc -> mc.getMetadata().getVersion().getFriendlyString());
    }

    //==================================================
    // Getters/setters
    //==================================================

    public void setServer(final MinecraftServer server) {
        this.server = server;
    }

}
