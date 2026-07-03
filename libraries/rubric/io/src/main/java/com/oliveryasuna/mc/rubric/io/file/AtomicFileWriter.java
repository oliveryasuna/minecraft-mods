package com.oliveryasuna.mc.rubric.io.file;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;

/**
 * Writes a byte array to a target file via a temp file plus atomic rename.
 * <p>
 * <strong>Guarantee:</strong> if the write fails (I/O error, render exception
 * thrown by the caller before the call, or process death between temp-write and
 * move), the original file is left untouched. Once the move succeeds the target
 * file contains exactly the new bytes.
 * <p>
 * Durability: after writing the temp file we call
 * {@link FileChannel#force(boolean)} so the bytes hit disk before the rename.
 * The directory entry itself is not fsynced — that would require a separate
 * open of the parent directory and is platform-specific; the rename is atomic
 * at the VFS level, which is sufficient for "no half-written file" semantics.
 * <p>
 * {@link StandardCopyOption#ATOMIC_MOVE} is preferred. Filesystems that reject
 * atomic move across the same directory (rare) fall through to a plain
 * {@link StandardCopyOption#REPLACE_EXISTING} move.
 */
final class AtomicFileWriter {

    //==================================================
    // Static methods
    //==================================================

    static void write(
            final Path target,
            final byte[] bytes
    ) throws IOException {
        final Path directory = target.toAbsolutePath().getParent();
        if(directory == null) {
            throw new IOException("Target has no parent directory: " + target);
        }
        Files.createDirectories(directory);

        final Path temp = Files.createTempFile(directory, target.getFileName().toString(), ".tmp");
        try {
            try(final FileChannel channel = FileChannel.open(
                    temp,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
            )) {
                channel.write(java.nio.ByteBuffer.wrap(bytes));
                channel.force(true);
            }
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch(final AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch(final IOException | RuntimeException e) {
            Files.deleteIfExists(temp);
            throw e;
        }
    }

    //==================================================
    // Constructors
    //==================================================

    private AtomicFileWriter() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
