package com.oliveryasuna.mc.rubric.neoforge;

import com.oliveryasuna.mc.rubric.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.Executor;

/**
 * NeoForge implementation of {@link Platform}. Mojmap names (NeoForge ships
 * Mojang mappings by default).
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
                // Defensive: Minecraft.getInstance() can be null very early in
                // game startup. Fall back to inline so callers (e.g.
                // change-event dispatch from ConfigManager.set) work regardless
                // of phase.
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

}
