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

import java.io.IOException;

public final class TestmodFabricMain implements ModInitializer {

    //==================================================
    // Static fields
    //==================================================

    private static final System.Logger LOG = System.getLogger("omniconfig-testmod");

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
        LOG.log(System.Logger.Level.INFO, "[omniconfig-testmod] onInitialize");

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
            LOG.log(System.Logger.Level.INFO, "[omniconfig-testmod] loaded; file=" + manager.getFile());
            LOG.log(System.Logger.Level.INFO, "[omniconfig-testmod] corrections=" + result.corrections().size());
        } catch(final IOException e) {
            LOG.log(System.Logger.Level.ERROR, "[omniconfig-testmod] load failed", e);
            return;
        }

        manager.getEvents().subscribe(event -> {
            LOG.log(System.Logger.Level.INFO, "[omniconfig-testmod] change path=" + event.path() + " old=" + event.oldValue() + " new=" + event.newValue());
        });

        try {
            manager.startFileWatch(new NioFileWatchService(), Loaders.platform().mainThreadExecutor());
        } catch(final IOException error) {
            LOG.log(System.Logger.Level.WARNING, "[omniconfig-testmod] file-watch wiring failed", error);
        }

        TestmodFabricMain.serverSyncService = FabricSyncBootstrap.installServer(codecs, Loaders.permissionGate());
        TestmodFabricMain.serverSyncService.register(manager);
    }

}
