package com.oliveryasuna.mc.omniconfig.testmod;

import com.oliveryasuna.mc.omniconfig.api.ConfigManager;
import com.oliveryasuna.mc.omniconfig.fabric.FabricSyncBootstrap;
import com.oliveryasuna.mc.omniconfig.fabric.Loaders;
import com.oliveryasuna.mc.omniconfig.fabric.OmniConfigSerialization;
import com.oliveryasuna.mc.omniconfig.io.file.NioFileWatchService;
import com.oliveryasuna.mc.omniconfig.lifecycle.LoadResult;
import com.oliveryasuna.mc.omniconfig.migration.MigrationRegistry;
import com.oliveryasuna.mc.omniconfig.sync.SyncService;
import com.oliveryasuna.mc.omniconfig.value.CodecRegistry;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class TestmodFabricMain implements ModInitializer {

    //==================================================
    // Static fields
    //==================================================

    private static final Logger LOGGER = LoggerFactory.getLogger("omniconfig-testmod");

    private static volatile ConfigManager<SampleConfig> manager;
    private static volatile SyncService serverSyncService;
    private static volatile CodecRegistry sharedCodecs;

    //==================================================
    // Static methods
    //==================================================

    public static ConfigManager<SampleConfig> manager() {
        return manager;
    }

    public static SyncService serverSyncService() {
        return serverSyncService;
    }

    public static CodecRegistry sharedCodecs() {
        return sharedCodecs;
    }

    //==================================================
    // Constructors
    //==================================================

    public TestmodFabricMain() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public void onInitialize() {
        LOGGER.info("[omniconfig-testmod] onInitialize");

        final MigrationRegistry migrations = new MigrationRegistry();

        final CodecRegistry codecs = new CodecRegistry();
        Loaders.registerMcCodecs(codecs);
        TestmodFabricMain.sharedCodecs = codecs;

        final ConfigManager<SampleConfig> manager = new ConfigManager<>(
                SampleConfig.class,
                OmniConfigSerialization.defaultIO(),
                Loaders.platform(),
                codecs,
                migrations
        );
        TestmodFabricMain.manager = manager;

        try {
            final LoadResult result = manager.load();
            LOGGER.info("[omniconfig-testmod] loaded; file=" + manager.getFile());
            LOGGER.info("[omniconfig-testmod] corrections=" + result.corrections().size());
        } catch(final IOException e) {
            LOGGER.error("[omniconfig-testmod] load failed", e);
            return;
        }

        manager.getEvents().subscribe(event -> {
            LOGGER.info("[omniconfig-testmod] change path=" + event.path() + " old=" + event.oldValue() + " new=" + event.newValue());
        });

        try {
            manager.startFileWatch(new NioFileWatchService(), Loaders.platform().mainThreadExecutor());
        } catch(final IOException error) {
            LOGGER.warn("[omniconfig-testmod] file-watch wiring failed", error);
        }

        TestmodFabricMain.serverSyncService = FabricSyncBootstrap.installServer(codecs, Loaders.permissionGate());
        TestmodFabricMain.serverSyncService.register(manager);
    }

}
