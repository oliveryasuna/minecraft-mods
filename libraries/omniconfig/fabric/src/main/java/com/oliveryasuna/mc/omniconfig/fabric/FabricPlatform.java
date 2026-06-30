package com.oliveryasuna.mc.omniconfig.fabric;

import com.oliveryasuna.mc.omniconfig.platform.Platform;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.Executor;

/**
 * Fabric implementation of {@link Platform}. Class names are Mojmap (matches
 * Fabric Loom's official Mojang mappings).
 */
public final class FabricPlatform implements Platform {

    //==================================================
    // Fields
    //==================================================

    private volatile MinecraftServer server;

    //==================================================
    // Constructors
    //==================================================

    public FabricPlatform() {
        super();
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

    //==================================================
    // Getters/setters
    //==================================================

    public void setServer(final MinecraftServer server) {
        this.server = server;
    }

}
