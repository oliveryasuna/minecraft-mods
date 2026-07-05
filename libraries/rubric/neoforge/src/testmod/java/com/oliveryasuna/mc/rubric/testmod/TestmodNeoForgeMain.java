package com.oliveryasuna.mc.rubric.testmod;

import com.oliveryasuna.mc.rubric.core.ConfigManager;
import com.oliveryasuna.mc.rubric.io.file.NioFileWatchService;
import com.oliveryasuna.mc.rubric.lifecycle.LoadResult;
import com.oliveryasuna.mc.rubric.loader.RubricSerialization;
import com.oliveryasuna.mc.rubric.migration.MigrationRegistry;
import com.oliveryasuna.mc.rubric.neoforge.Loaders;
import com.oliveryasuna.mc.rubric.neoforge.NeoForgeSyncBootstrap;
import com.oliveryasuna.mc.rubric.sync.SyncService;
import com.oliveryasuna.mc.rubric.value.CodecRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Mod(TestmodNeoForgeMain.MOD_ID)
public final class TestmodNeoForgeMain {

    //==================================================
    // Static fields
    //==================================================

    public static final String MOD_ID = "rubric_testmod";

    private static final Logger LOGGER = LoggerFactory.getLogger("rubric-testmod");

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

    public TestmodNeoForgeMain(final IEventBus modEventBus) {
        super();

        LOGGER.info("[rubric-testmod] constructor");

        final MigrationRegistry migrations = new MigrationRegistry();

        final CodecRegistry codecs = new CodecRegistry();
        Loaders.registerMcCodecs(codecs);
        TestmodNeoForgeMain.sharedCodecs = codecs;

        final ConfigManager<SampleConfig> manager = new ConfigManager<>(
                SampleConfig.class,
                RubricSerialization.allFormatsIO(),
                Loaders.platform(),
                codecs,
                migrations
        );
        TestmodNeoForgeMain.manager = manager;

        try {
            final LoadResult result = manager.load();
            LOGGER.info("[rubric-testmod] loaded; file=" + manager.getFile());
            LOGGER.info("[rubric-testmod] corrections=" + result.corrections().size());
        } catch(final IOException e) {
            LOGGER.error("[rubric-testmod] load failed", e);
            return;
        }

        manager.getEvents().subscribe(event -> {
            LOGGER.info("[rubric-testmod] change path=" + event.path() + " old=" + event.oldValue() + " new=" + event.newValue());
        });

        try {
            manager.startFileWatch(new NioFileWatchService(), Loaders.platform().mainThreadExecutor());
        } catch(final IOException error) {
            LOGGER.warn("[rubric-testmod] file-watch wiring failed", error);
        }

        TestmodNeoForgeMain.serverSyncService = NeoForgeSyncBootstrap.installServer(codecs, Loaders.permissionGate());
        TestmodNeoForgeMain.serverSyncService.register(manager);
    }

}
