package com.oliveryasuna.mc.coal.neoforge;

import com.oliveryasuna.mc.coal.api.platform.Environment;
import com.oliveryasuna.mc.coal.api.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * NeoForge implementation of {@link Platform}. Discovered by
 * {@link com.oliveryasuna.mc.coal.api.Coal Coal}'s ServiceLoader dispatch
 * via {@code META-INF/services/com.oliveryasuna.mc.coal.api.platform.Platform}.
 */
public final class NeoForgePlatform implements Platform {

    //==================================================
    // Constructors
    //==================================================

    public NeoForgePlatform() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public Executor mainThreadExecutor() {
        if(FMLEnvironment.dist == Dist.CLIENT) {
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
            final MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
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
        return FMLEnvironment.dist == Dist.CLIENT
                ? Environment.CLIENT
                : Environment.SERVER;
    }

    @Override
    public Optional<Path> gameDir() {
        return Optional.of(FMLPaths.GAMEDIR.get());
    }

    @Override
    public Optional<String> loaderName() {
        return Optional.of("neoforge");
    }

    @Override
    public Optional<String> loaderVersion() {
        return Optional.empty();
    }

}
