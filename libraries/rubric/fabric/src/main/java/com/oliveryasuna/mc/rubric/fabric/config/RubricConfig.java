package com.oliveryasuna.mc.rubric.fabric.config;

import com.oliveryasuna.mc.rubric.api.annotation.Comment;
import com.oliveryasuna.mc.rubric.api.annotation.Config;
import com.oliveryasuna.mc.rubric.api.annotation.Range;

@Config(id = "rubric", name = "Rubric")
public final class RubricConfig {

    //==================================================
    // Fields
    //==================================================

    @Comment("GUI settings.")
    public GUI gui = new GUI();

    @Comment("File I/O settings.")
    public IO io = new IO();

    @Comment("Server <-> client sync settings.")
    public Sync sync = new Sync();

    @Comment("Validation / load-policy settings.")
    public Validation validation = new Validation();

    //==================================================
    // Constructors
    //==================================================

    public RubricConfig() {
        super();
    }

    //==================================================
    // Nested
    //==================================================

    public static final class GUI {

        //==================================================
        // Fields
        //==================================================

        @Comment("Frontend to use when multiple are registered. AUTO picks the first registered provider (typically YACL). Other values pin a specific one; if that frontend is not installed, the placeholder screen is shown.")
        public Frontend preferredFrontend = Frontend.AUTO;

        @Comment("Append metadata tag suffixes to entry labels in the GUI. (e.g. \"[restart]\", \"[server, world]\")")
        public boolean showMetadataSuffixes = true;

        // TODO: Use.
        @Comment("Number of discrete steps in a slider widget — the entry's range is divided into N ticks. Higher = finer drag, lower = chunkier. Only used for bounded numeric entries whose @Widget does not pin its own step.")
        public int defaultSliderTicks = 200;

        //==================================================
        // Constructors
        //==================================================

        public GUI() {
            super();
        }

    }

    public static final class IO {

        //==================================================
        // Fields
        //==================================================

        // TODO: Use.
        @Comment("How many timestamped corrupt-file backups to keep. Backups are written by TimestampedBackupStrategy before a malformed config is overwritten on load; older backups beyond this count are pruned. (0 = backups disabled)")
        public int backupRetention = 10;

        // TODO: Use.
        @Comment("Minimum quiet time before a file-watch event triggers a reload. Coalesces editor staged-write-then-rename sequences into a single reload. Raise if your editor still triggers double reloads. (in milliseconds)")
        public long fileWatchDebounceMillis = 200L;

        // TODO: Use.
        @Comment("Write configs via tmp file + atomic rename, so a crash mid-write never leaves a half-written file. Disable only when your filesystem rejects ATOMIC_MOVE — symptom: AtomicMoveNotSupportedException in logs. Known offenders: NFS, some FUSE mounts, sync clients like Dropbox / OneDrive.")
        public boolean atomicWrites = true;

        //==================================================
        // Constructors
        //==================================================

        public IO() {
            super();
        }

    }

    public static final class Sync {

        //==================================================
        // Fields
        //==================================================

        // TODO: Use.
        @Comment("Hard cap on a single sync payload. Server-pushed snapshots larger than this are rejected (defense vs malicious / misconfigured server). (in bytes)")
        @Range(min = 1_024, max = 16 * 1_024 * 1_024)
        public int payloadMaxBytes = 1_048_576;

        // TODO: Use.
        @Comment("Maximum wait for the server's Handshake before the sync session is dropped. (in milliseconds)")
        @Range(min = 500, max = 60_000)
        public long handshakeTimeoutMillis = 5_000L;

        // TODO: Use.
        @Comment("Fail-closed when the server's ProtocolVersion differs from ours. When false, mismatched payloads are skipped with a log warning instead.")
        public boolean requireProtocolMatch = true;

        //==================================================
        // Constructors
        //==================================================

        public Sync() {
            super();
        }

    }

    public static final class Validation {

        //==================================================
        // Fields
        //==================================================

        // TODO: Use.
        @Comment("Refuse to load a config whose load produced any corrections, instead of the default correct-and-log behavior. Useful for modpack devs / CI who want a config typo to fail the boot loudly.")
        public boolean strict = false;

        // TODO: Use.
        @Comment("On each load, write a sibling <config>.corrections.log listing every correction applied. Off by default — turn on when debugging schema mismatches.")
        public boolean dumpCorrections = false;

        //==================================================
        // Constructors
        //==================================================

        public Validation() {
            super();
        }

    }

}
